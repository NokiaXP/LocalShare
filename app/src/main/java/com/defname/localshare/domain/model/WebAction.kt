package com.defname.localshare.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ActionEnvelop(
    val id: Int? = null,
    val action: WebAction
)


@Serializable
data class FileUploadInfo(
    val checksum: String,
    val name: String,
    val mimeType: String,
    val size: Long
)


@Serializable
sealed class WebAction {
    @Serializable @SerialName("request_upload_permission")
    object RequestUploadPermission : WebAction()

    @Serializable @SerialName("initialize_file_upload")
    data class InitializeFileUpload(
        val draftId: Int,
        val files: List<FileUploadInfo>
    ) : WebAction()

    @Serializable @SerialName("abort_file_upload")
    data class AbortFileUpload(val id: String) : WebAction()
}


@Serializable
data class EventEnvelop(
    val correlationId: Int? = null,
    val event: WebEvent
)

@Serializable
enum class UploadStatus {
    @SerialName("pending") PENDING,
    @SerialName("uploading") UPLOADING,
    @SerialName("finished") FINISHED,
    @SerialName("error") ERROR,
    @SerialName("canceled") CANCELED
}

@Serializable
sealed class WebEvent {
    @Serializable @SerialName("init")
    data class Init(
        val uploadsEnabled: Boolean
    ) : WebEvent()

    @Serializable @SerialName("add_file")
    data class AddFile(
        val id: String,
        val name: String,
        val size: Long,
        val mimeType: String,
        val iconFile: String
    ) : WebEvent()

    @Serializable @SerialName("remove_file")
    data class RemoveFile(val id: String) : WebEvent()

    @Serializable @SerialName("add_text")
    data class AddText(
        val id: Int,
        val content: String,
        val mimeType: String = "text/plain",
        val label: String? = null
    ) : WebEvent()

    @Serializable @SerialName("remove_text")
    data class RemoveText(val id: Int) : WebEvent()

    @Serializable @SerialName("upload_permission_change")
    data class UploadPermissionChange(val status: UploadPermissionStatus) : WebEvent()

    @Serializable @SerialName("upload_accepted")
    data class UploadAccepted(
        val draftId: Int,
        val id: String
    ) : WebEvent()

    @Serializable @SerialName("upload_denied")
    data class UploadDenied(val draftId: Int) : WebEvent()

    @Serializable @SerialName("upload_error")
    data class UploadError(val id: String, val message: String? = null) : WebEvent()

    @Serializable @SerialName("upload_updated")
    data class UploadUpdated(
        val id: String,
        val fileChecksum: String,
        val status: UploadStatus =  UploadStatus.PENDING,
        val progress: Long = 0,
        val message: String? = null
    ) : WebEvent()
}