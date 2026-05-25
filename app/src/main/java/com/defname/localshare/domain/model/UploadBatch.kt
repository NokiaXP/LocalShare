package com.defname.localshare.domain.model


data class DraftUploadBatch(
    val draftId: Int,
    val clientIp: String,
    val files: List<UploadFile>
)

data class UploadBatch(
    val batchId: String,
    val clientIp: String,
    val files: List<UploadFile>
)

data class UploadFile(
    val checksum: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val state: UploadFileState
)

sealed class UploadFileState {
    object Pending : UploadFileState()
    object Finished : UploadFileState()
    object Error : UploadFileState()
    object Canceled: UploadFileState()
    class Uploading(val progress: Int) : UploadFileState()
}