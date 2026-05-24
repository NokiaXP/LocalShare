package com.defname.localshare.data

import com.defname.localshare.domain.model.ActionEnvelop
import com.defname.localshare.domain.model.EventEnvelop
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.domain.model.SharedContent
import com.defname.localshare.domain.model.UploadPermissionStatus
import com.defname.localshare.domain.model.WebAction
import com.defname.localshare.domain.model.WebEvent
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.ConcurrentHashMap


sealed class FlowDelta<T>(val obj: T) {
    class Added<T>(obj: T) : FlowDelta<T>(obj)
    class Removed<T>(obj: T) : FlowDelta<T>(obj)
}

fun <T, K>Flow<List<T>>.asDeltaEvents(key: (T) -> K): Flow<FlowDelta<T>> = flow {
    var oldList = emptyList<T>()
    this@asDeltaEvents.collect { newList ->
        // 1. Was ist neu? (In newList, aber nicht in oldList)
        val added = newList.filter { newItem -> oldList.none { key(it) == key(newItem) } }
        added.forEach { emit(FlowDelta.Added(it)) }

        // 2. Was wurde gelöscht? (In oldList, aber nicht in newList)
        val removed = oldList.filter { oldItem -> newList.none { key(it) == key(oldItem) } }
        removed.forEach { emit(FlowDelta.Removed(it)) }

        oldList = newList
    }
}

@JvmName("asFileInfoDeltaEvents")
fun Flow<List<FileInfo>>.asDeltaEvents() = asDeltaEvents { it.id }
@JvmName("asSharedContentDeltaEvents")
fun Flow<List<SharedContent>>.asDeltaEvents() = asDeltaEvents { it.id }


class RemoteCommunicationRepository(
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository,
    private val securityRepository: SecurityRepository
) {
    private val _clientEventFlows = ConcurrentHashMap<String, MutableSharedFlow<EventEnvelop>>()

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

        _clientEventFlows[clientIp]?.emit(event)
    }

    fun handleAction(event: ActionEnvelop, remoteIp: String) {
        when (event.action) {
            is WebAction.RequestUploadPermission -> {
                securityRepository.requestUploadPermission(remoteIp)
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