package com.defname.localshare.domain.manager

import com.defname.localshare.data.FileStorageProvider
import com.defname.localshare.data.UploadRepository
import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.EventEnvelop
import com.defname.localshare.domain.model.UploadFile
import com.defname.localshare.domain.model.UploadStatus
import com.defname.localshare.domain.model.WebEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.InputStream

class UploadManager(
    private val uploadRepository: UploadRepository,
    private val fileStorageProvider: FileStorageProvider
) {
    private val _openUploadRequests = MutableStateFlow(emptyList<DraftUploadBatch>())
    val openUploadRequests = _openUploadRequests.asStateFlow()

    private val _uploadEvents = MutableSharedFlow<Pair<EventEnvelop, String>>()
    val uploadEvents = _uploadEvents.asSharedFlow()

    fun onNewUploadRequest(clientIp: String, draftId: Int, files: List<UploadFile>) {
        if (findRequest(clientIp, draftId) != null) {
            return
        }
        if (files.isEmpty()) {
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
        val fileErrors = mutableListOf<String>()
        val fileUris = draft.files.map { file ->
            val uri = fileStorageProvider.createOutputFile(file.name, file.mimeType)
            if (uri == null) {
                fileErrors += file.checksum
                return@map null
            }
            file.checksum to uri
        }.filterNotNull().toMap()

        if (fileUris.isEmpty()) {
            emitUploadError(clientIp, draft.draftId.toString(), "No files to upload.")
            return
        }

        val requestId = uploadRepository.initNewUploadBatch(draft, fileUris)

        val event = EventEnvelop(event = WebEvent.UploadAccepted(draftId, requestId))
        _uploadEvents.emit(event to clientIp)

        // emit file creation errors
        for (fileHash in fileErrors) {
            emitFileError(clientIp, requestId, fileHash, "File cannot be created.")
        }
    }

    suspend fun onUploadCanceledByClient(clientIp: String, batchId: String) {
        val batch = uploadRepository.getBatch(batchId) ?: return
        if (batch.clientIp != clientIp) {
            return
        }

        // delete files of batch
        batch.files.forEach { file ->
            file.uri?.also {
                fileStorageProvider.deleteFile(it)
            }
        }

        uploadRepository.removeBatch(batchId)
    }

    suspend fun onFileUpload(clientIp: String, batchId: String, fileHash: String, inputStream: InputStream) {
        val batch = uploadRepository.getBatch(batchId) ?: return
        if (batch.clientIp != clientIp) {
            return
        }

        val file = batch.files.find { it.checksum == fileHash } ?: return
        val uri = file.uri ?: return

        val outputStream = fileStorageProvider.openOutputStream(uri)
        if (outputStream == null) {
            emitFileError(clientIp, batchId, fileHash, "File can not be written.")
            return
        }

        outputStream.also {
            inputStream.copyTo(it)
            it.close()

            val event = EventEnvelop(
                event = WebEvent.UploadUpdated(
                    id = batchId,
                    fileChecksum = fileHash,
                    status = UploadStatus.FINISHED
                )
            )
            _uploadEvents.emit(event to clientIp)
        }
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

    suspend private fun emitUploadError(clientIp: String, id: String, message: String? = null) {
        val event = EventEnvelop(
            event = WebEvent.UploadError(id, message)
        )
        _uploadEvents.emit(event to clientIp)
    }

    private suspend fun emitFileError(clientIp: String, requestId: String, fileHash: String, message: String) {
        val update = EventEnvelop(
            event = WebEvent.UploadUpdated(
                id = requestId,
                fileChecksum = fileHash,
                status = UploadStatus.ERROR,
                message = message
            )
        )
        _uploadEvents.emit(update to clientIp)
    }
}