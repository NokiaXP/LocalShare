package com.defname.localshare.data

import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.EventEnvelop
import com.defname.localshare.domain.model.UploadFile
import com.defname.localshare.domain.model.WebEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class UploadManager(
    val uploadRepository: UploadRepository

) {
    private val _openUploadRequests = MutableStateFlow(emptyList<DraftUploadBatch>())
    val openUploadRequests = _openUploadRequests.asStateFlow()

    private val _uploadEvents = MutableSharedFlow<Pair<EventEnvelop, String>>()
    val uploadEvents = _uploadEvents.asSharedFlow()

    fun onNewUploadRequest(clientIp: String, draftId: Int, files: List<UploadFile>) {
        if (findRequest(clientIp, draftId) != null) {
            return
        }
        addRequest(clientIp, draftId, files)
    }

    suspend fun onRequestDenied(clientIp: String, draftId: Int) {
        val request = removeRequest(clientIp, draftId) ?: return
        val event = EventEnvelop(
            event = WebEvent.UploadDenied(draftId)
        )
        _uploadEvents.emit(event to clientIp)
    }

    suspend fun onRequestAccepted(clientIp: String, draftId: Int) {
        val draft = findRequest(clientIp, draftId) ?: return
        val requestId = uploadRepository.initNewUploadBatch(draft)

        val event = EventEnvelop(event = WebEvent.UploadAccepted(draftId, requestId))
        _uploadEvents.emit(event to clientIp)
    }

    suspend fun onUploadCanceledByClient(clientIp: String, batchId: String) {
        TODO()
        // close filestreams, delete files, remove uploadbatch from uploadrepository
        // need to decide whats responsible for which part
    }

    private fun findRequest(clientIp: String, draftId: Int) = _openUploadRequests.value.find { it.draftId == draftId && it.clientIp == clientIp }

    private fun addRequest(clientIp: String, draftId: Int, files: List<UploadFile>) {
        _openUploadRequests.update { list ->
            list + DraftUploadBatch(draftId, clientIp, files)
        }
    }

    private fun removeRequest(clientIp: String, draftId: Int): DraftUploadBatch? {
        val request = findRequest(clientIp, draftId) ?: return null
        _openUploadRequests.update { list ->
            list - request
        }
        return request
    }
}