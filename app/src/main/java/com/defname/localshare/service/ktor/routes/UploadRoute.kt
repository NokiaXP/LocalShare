package com.defname.localshare.service.ktor.routes

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.copyTo

fun Route.upload(
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    post("/{token}/upload") {
        val remoteHost = call.request.local.remoteHost
        if (!securityHandler.verifyAccess(call) || !securityHandler.hasUploadPermission(remoteHost)) {
            return@post call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val multipart = call.receiveMultipart()
        var filesUploaded = 0

        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                val fileName = part.originalFileName ?: "upload_${System.currentTimeMillis()}"
                val mimeType = part.contentType?.toString() ?: "application/octet-stream"

                saveToDownloads(context, fileName, mimeType, part)
                filesUploaded++
            }
            part.dispose()
        }

        if (filesUploaded > 0) {
            call.respondText("Successfully uploaded $filesUploaded file(s).", status = HttpStatusCode.OK)
        } else {
            call.respondText("No files were uploaded.", status = HttpStatusCode.BadRequest)
        }
    }
}

private suspend fun saveToDownloads(context: Context, fileName: String, mimeType: String, fileItem: PartData.FileItem) {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val uri = contentResolver.insert(collection, contentValues)

    uri?.let { targetUri ->
        contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            fileItem.provider().copyTo(outputStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(targetUri, contentValues, null, null)
        }
    }
}
