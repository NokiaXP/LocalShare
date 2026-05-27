package com.defname.localshare.data

import com.defname.localshare.domain.manager.UploadManager
import com.defname.localshare.domain.model.ActionEnvelop
import com.defname.localshare.domain.model.EventEnvelop
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.domain.model.SharedContent
import com.defname.localshare.domain.model.UploadFile
import com.defname.localshare.domain.model.UploadFileState
import com.defname.localshare.domain.model.UploadPermissionStatus
import com.defname.localshare.domain.model.WebAction
import com.defname.localshare.domain.model.WebEvent
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.util.FlowDelta
import com.defname.localshare.util.asDeltaEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap


@JvmName("asFileInfoDeltaEvents")
fun Flow<List<FileInfo>>.asDeltaEvents() = asDeltaEvents { it.id }
@JvmName("asSharedContentDeltaEvents")
fun Flow<List<SharedContent>>.asDeltaEvents() = asDeltaEvents { it.id }


class RemoteCommunicationRepository(
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository,
    private val securityRepository: SecurityRepository,
    private val uploadManager: UploadManager,
    private val repositoryScope: CoroutineScope
) {
    private val _clientEventFlows = ConcurrentHashMap<String, MutableSharedFlow<EventEnvelop>>()

    init {
        repositoryScope.launch {
            uploadManager.uploadEvents.collect { (event, clientIp) ->
                sendEvent(event to clientIp)
            }
        }
    }

    fun getStreamForClient(remoteIp: String): Flow<EventEnvelop> {
        val personalFlow = _clientEventFlows.getOrPut(remoteIp, {
            MutableSharedFlow(extraBufferCapacity = 64)
        })

        return merge(
            personalFlow,
            observePermissionChangeFor(remoteIp),
            observeGlobalEvents()
        ).onStart { emit(EventEnvelop(event = WebEvent.Init(settingsRepository.settingsFlow.first().uploadFeatureActive))) }
    }

    suspend fun sendEvent(eventTo: Pair<EventEnvelop, String>) {
        val clientIp = eventTo.second
        val event = eventTo.first

        _clientEventFlows.getOrPut(clientIp) {
            MutableSharedFlow(extraBufferCapacity = 64)
        }.emit(event)
    }

    fun handleAction(envelope: ActionEnvelop, remoteIp: String) {
        when (val action = envelope.action) {
            is WebAction.RequestUploadPermission -> {
                securityRepository.requestUploadPermission(remoteIp)
            }
            is WebAction.InitializeFileUpload -> {
                val files = action.files.map { info ->
                    UploadFile(
                        checksum = info.checksum,
                        name = info.name,
                        mimeType = info.mimeType,
                        size = info.size,
                        state = UploadFileState.Pending,
                        uri = null
                    )
                }
                uploadManager.onNewUploadRequest(remoteIp, action.draftId, files)
            }
            is WebAction.AbortFileUpload -> {
                repositoryScope.launch {
                    uploadManager.onUploadCanceledByClient(remoteIp, action.id)
                }
            }
        }
    }

    private fun observeGlobalEvents(): Flow<EventEnvelop> {
        return merge(
            observeFileList(),
            observeSharedContent()
        )
    }

    private fun observePermissionChangeFor(clientIp: String): Flow<EventEnvelop> {
        return securityRepository.whitelist.map { list ->
            val entry = list.find { it.ip == clientIp } ?: return@map UploadPermissionStatus.UNKNOWN
            entry.uploadPermission
        }
            .map { EventEnvelop(event = WebEvent.UploadPermissionChange(it)) }
            .distinctUntilChanged()
    }

    private fun observeFileList(): Flow<EventEnvelop> {
        return serviceRepository.fileList
            .asDeltaEvents()
            .map { delta ->
                val event: WebEvent = when (delta) {
                    is FlowDelta.Added -> WebEvent.AddFile(
                        id = delta.obj.id,
                        name = delta.obj.name,
                        size = delta.obj.size,
                        mimeType = delta.obj.mimeType,
                        iconFile = delta.obj.iconFile
                    )

                    is FlowDelta.Removed -> WebEvent.RemoveFile(
                        id = delta.obj.id
                    )
                }
                EventEnvelop(event = event)
            }
    }

    private fun observeSharedContent(): Flow<EventEnvelop> {
        return serviceRepository.runtimeState.map {
            it.sharedContentList
        }
            .asDeltaEvents()
            .map { delta ->
                val event = when (delta) {
                    is FlowDelta.Added -> WebEvent.AddText(
                        id = delta.obj.id,
                        content = when (delta.obj) {
                            is SharedContent.Text -> delta.obj.text
                            is SharedContent.Other -> delta.obj.data
                        },
                        mimeType = delta.obj.mimeType,
                        label = if (delta.obj is SharedContent.Other) delta.obj.label else null
                    )
                    is FlowDelta.Removed -> WebEvent.RemoveText(delta.obj.id)
                }
                EventEnvelop(event = event)
            }
    }
}
