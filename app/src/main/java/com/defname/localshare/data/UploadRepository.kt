package com.defname.localshare.data

import android.net.Uri
import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.UploadBatch
import com.defname.localshare.domain.model.UploadFileState
import java.util.concurrent.ConcurrentHashMap

class UploadRepository(

) {
    private val _batches = ConcurrentHashMap<String, UploadBatch>()

    companion object {
        private var lastBatchId: Int = 0
    }

    fun initNewUploadBatch(draft: DraftUploadBatch, fileUris: Map<String, Uri>) : String {
        lastBatchId += 1
        val newBatchId = lastBatchId.hashCode().toString()
        val newBatch = UploadBatch(
            batchId = newBatchId,
            clientIp = draft.clientIp,
            files = draft.files.map { file ->
                file.copy(state = UploadFileState.Pending, uri = fileUris[file.checksum])
            }
        )
        _batches += (newBatchId to newBatch)

        return newBatchId
    }

    fun removeBatch(batchId: String) {
        _batches -= batchId
    }

    fun getBatch(batchId: String) : UploadBatch? {
        return _batches[batchId]
    }
}