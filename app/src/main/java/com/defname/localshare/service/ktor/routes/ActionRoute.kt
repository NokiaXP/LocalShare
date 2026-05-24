package com.defname.localshare.service.ktor.routes

import com.defname.localshare.data.RemoteCommunicationRepository
import com.defname.localshare.domain.model.ActionEnvelop
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.apiAction(
    securityHandler: ServerSecurityHandler,
    remoteCommunicationRepository: RemoteCommunicationRepository,
) {
    post("/{token}/api/action") {
        if (!securityHandler.verifyAccess(call)) {
            return@post call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val actionEnvelop = call.receive<ActionEnvelop>()

        val response = remoteCommunicationRepository.handleAction(
            actionEnvelop,
            call.request.local.remoteAddress
        )

        call.respondText("OK", status = HttpStatusCode.OK)
    }
}