package com.defname.localshare.data

import android.net.Uri
import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.UploadBatch
import com.defname.localshare.domain.model.UploadFileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UploadRepository {
    private val _batches = MutableStateFlow<Map<String, UploadBatch>>(emptyMap())
    val batches = _batches.asStateFlow()

    fun initNewUploadBatch(batchId: String, draft: DraftUploadBatch, fileUris: Map<String, Uri>) {
        val newBatch = UploadBatch(
            batchId = batchId,
            clientIp = draft.clientIp,
            files = draft.files.map { file ->
                file.copy(state = UploadFileState.Pending, uri = fileUris[file.checksum])
            }
        )
        _batches.update { it + (batchId to newBatch) }
    }

    fun removeBatch(batchId: String) {
        _batches.update { it - batchId }
    }

    fun getBatch(batchId: String): UploadBatch? {
        return _batches.value[batchId]
    }

    fun updateBatch(batch: UploadBatch) {
        _batches.update { it + (batch.batchId to batch) }
    }

    fun updateFileState(batchId: String, fileChecksum: String, newState: UploadFileState) {
        _batches.update { currentBatches ->
            val batch = currentBatches[batchId] ?: return@update currentBatches
            val updatedFiles = batch.files.map { file ->
                if (file.checksum == fileChecksum) file.copy(state = newState) else file
            }
            currentBatches + (batchId to batch.copy(files = updatedFiles))
        }
    }
}