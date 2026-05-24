package com.defname.localshare.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ActionEnvelop(
    val id: Int? = null,
    val action: WebAction
)

@Serializable
sealed class WebAction {
    @Serializable @SerialName("request_upload_permission")
    class RequestUploadPermission : WebAction()
}


@Serializable
data class EventEnvelop(
    val correlationId: Int? = null,
    val event: WebEvent
)

@Serializable
sealed class WebEvent {
    @Serializable @SerialName("init")
    class Init(
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

}