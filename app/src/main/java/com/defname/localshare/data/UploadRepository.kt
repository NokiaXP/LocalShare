package com.defname.localshare.data

import com.defname.localshare.domain.model.DraftUploadBatch
import com.defname.localshare.domain.model.UploadBatch
import com.defname.localshare.domain.model.UploadFileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UploadRepository(

) {
    private val _batches = MutableStateFlow(emptyMap<String, UploadBatch>())
    val batches = _batches.asStateFlow()

    companion object {
        private var lastBatchId: Int = 0
    }

    fun initNewUploadBatch(draft: DraftUploadBatch) : String {
        lastBatchId += 1
        val newBatchId = lastBatchId.hashCode().toString()
        val newBatch = UploadBatch(
            batchId = newBatchId,
            clientIp = draft.clientIp,
            files = draft.files.map { file ->
                file.copy(state = UploadFileState.Pending)
            }
        )
        _batches.update { map ->
            map + (newBatchId to newBatch)
        }
        return newBatchId
    }
}