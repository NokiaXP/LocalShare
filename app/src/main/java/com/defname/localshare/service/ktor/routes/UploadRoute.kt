package com.defname.localshare.service.ktor.routes

import com.defname.localshare.domain.manager.UploadManager
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.upload(
    securityHandler: ServerSecurityHandler,
    uploadManager: UploadManager,
) {
    post("/{token}/upload/{batchId}/{fileHash}") {
        val remoteHost = call.request.local.remoteHost
        if (!securityHandler.verifyAccess(call) || !securityHandler.hasUploadPermission(remoteHost)) {
            return@post call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val batchId = call.parameters["batchId"]
        val fileHash = call.parameters["fileHash"]

        if (batchId == null || fileHash == null) {
            return@post call.respondText("Bad Request.", status = HttpStatusCode.BadRequest)
        }

        val clientIp = call.request.local.remoteAddress

        val inputStream = call.receiveStream()

        inputStream.also {
            uploadManager.onFileUpload(clientIp, batchId, fileHash, it)
            it.close()
        }
        call.respondText("OK", status = HttpStatusCode.OK)
    }
}
