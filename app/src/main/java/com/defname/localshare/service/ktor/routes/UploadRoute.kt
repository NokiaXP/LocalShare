package com.defname.localshare.service.ktor.routes

import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.upload(
    securityHandler: ServerSecurityHandler,
) {
    post("/{token}/upload/request") {
        if (!securityHandler.verifyAccess(call)) {
            return@post call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }
        securityHandler.requestUploadPermission(call.request.local.remoteHost)
        return@post call.respondText("Permission requested.", status = HttpStatusCode.OK)
    }

    post("/{token}/upload") {
        val remoteHost = call.request.local.remoteHost
        if (!securityHandler.verifyAccess(call) || !securityHandler.hasUploadPermission(remoteHost)) {
            return@post call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        // TODO: Implement multi-part file upload to MediaStore
        call.respondText("Upload received (not yet implemented)", status = HttpStatusCode.NotImplemented)
    }
}