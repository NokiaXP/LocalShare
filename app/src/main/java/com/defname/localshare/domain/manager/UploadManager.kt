package com.defname.localshare.domain.manager

import com.defname.localshare.data.FileStorageProvider
import com.defname.localshare.data.UploadRepository
import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.EventEnvelop
import com.defname.localshare.domain.model.UploadFile
import com.defname.localshare.domain.model.UploadFileState
import com.defname.localshare.domain.model.UploadStatus
import com.defname.localshare.domain.model.WebEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.InputStream
import java.security.MessageDigest

class UploadManager(
    private val uploadRepository: UploadRepository,
    private val fileStorageProvider: FileStorageProvider,
    private val featureFlagsProvider: FeatureFlagsProvider
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
        val draft = removeRequest(clientIp, draftId) ?: return
        
        val batchId = java.util.UUID.randomUUID().toString().take(6)
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val subFolder = "Upload_${timestamp}_$batchId"

        val fileErrors = mutableListOf<String>()
        val fileUris = draft.files.map { file ->
            val uri = fileStorageProvider.createOutputFile(file.name, file.mimeType, subFolder)
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

        uploadRepository.initNewUploadBatch(batchId, draft, fileUris)

        val event = EventEnvelop(event = WebEvent.UploadAccepted(draftId, batchId))
        _uploadEvents.emit(event to clientIp)

        // emit file creation errors
        for (fileHash in fileErrors) {
            emitFileError(clientIp, batchId, fileHash, "File cannot be created.")
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

        if (file.state is UploadFileState.Uploading || file.state is UploadFileState.Finished) {
            return
        }

        val outputStream = fileStorageProvider.openOutputStream(uri)
        if (outputStream == null) {
            emitFileError(clientIp, batchId, fileHash, "File can not be written.")
            return
        }

        try {
            uploadRepository.updateFileState(batchId, fileHash, UploadFileState.Uploading(0))
            
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var iterations = 0
            
            outputStream.use { out ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                    out.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    uploadRepository.updateFileState(batchId, fileHash, UploadFileState.Uploading(totalBytesRead))
                    iterations += 1
                    if (iterations % 100 == 0) {
                        val event = EventEnvelop(
                            event = WebEvent.UploadUpdated(
                                id = batchId,
                                fileChecksum = fileHash,
                                status = UploadStatus.UPLOADING,
                                progress = totalBytesRead
                            )
                        )
                        _uploadEvents.emit(event to clientIp)
                    }
                }
            }

            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            
            if (actualHash.equals(fileHash, ignoreCase = true)) {
                fileStorageProvider.finalizeFile(uri)
                uploadRepository.updateFileState(batchId, fileHash, UploadFileState.Finished)

                val event = EventEnvelop(
                    event = WebEvent.UploadUpdated(
                        id = batchId,
                        fileChecksum = fileHash,
                        status = UploadStatus.FINISHED
                    )
                )
                _uploadEvents.emit(event to clientIp)
            } else {
                fileStorageProvider.deleteFile(uri)
                uploadRepository.updateFileState(batchId, fileHash, UploadFileState.Error)
                emitFileError(clientIp, batchId, fileHash, "Checksum mismatch! File corrupted.")
            }
        } catch (e: Exception) {
            fileStorageProvider.deleteFile(uri)
            uploadRepository.updateFileState(batchId, fileHash, UploadFileState.Error)
            emitFileError(clientIp, batchId, fileHash, "Upload failed: ${e.message}")
        }
    }

    private fun findRequest(clientIp: String, draftId: Int) = _openUploadRequests.value.find { it.draftId == draftId && it.clientIp == clientIp }

    private fun addRequest(clientIp: String, draftId: Int, files: List<UploadFile>) {
        if (!featureFlagsProvider.uploadFeature.value) {
            return
        }
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