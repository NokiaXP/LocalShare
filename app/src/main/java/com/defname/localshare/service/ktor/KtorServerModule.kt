// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor

import android.content.Context
import com.defname.localshare.data.CallAttributes
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.RemoteCommunicationRepository
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.manager.UploadManager
import com.defname.localshare.domain.model.DisconnectReason
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.routes.apiAction
import com.defname.localshare.service.ktor.routes.getAssets
import com.defname.localshare.service.ktor.routes.getEvents
import com.defname.localshare.service.ktor.routes.getFavIcon
import com.defname.localshare.service.ktor.routes.getFile
import com.defname.localshare.service.ktor.routes.getFileIcon
import com.defname.localshare.service.ktor.routes.getLanding
import com.defname.localshare.service.ktor.routes.getThumbnail
import com.defname.localshare.service.ktor.routes.upload
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun Application.configureServerModule(
    serviceRepository: ServiceRepository,
    remoteCommunicationRepository: RemoteCommunicationRepository,
    connectionLogsRepository: ConnectionLogsRepository,
    settingsRepository: SettingsRepository,
    securityHandler: ServerSecurityHandler,
    securityRepository: SecurityRepository,
    fileInfoProvider: FileInfoProvider,
    uploadManager: UploadManager,
    json: Json,
    context: Context
) {
    install(PartialContent)
    install(IgnoreTrailingSlash)
    install(ContentNegotiation) {
        json(json)
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        val connectionId = connectionLogsRepository.clientConnected(
            method = call.request.httpMethod.value,
            path = call.request.uri,
            clientIp = call.request.local.remoteHost
        )

        call.attributes.put(CallAttributes.connectionId, connectionId)

        try {
            proceed()
        } finally {
            val discconnectReason: DisconnectReason = call.response.status()?.let {
                DisconnectReason.Expected(it.value)
            } ?: DisconnectReason.Unexpected.Unknown

            connectionLogsRepository.clientDisconnected(
                connectionId,
                discconnectReason
            )
        }

    }

    // 2. Routing definieren
    routing {
        get("/favicon.ico") {
            call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            return@get call.respond(HttpStatusCode.NoContent)
        }

        getFavIcon(securityHandler, context)

        getThumbnail(securityHandler, serviceRepository, fileInfoProvider, context)

        getFileIcon(securityHandler, context)

        getAssets(securityHandler, context)

        getEvents(securityHandler, remoteCommunicationRepository, serviceRepository, settingsRepository, connectionLogsRepository, securityRepository, json, context)

        getFile(securityHandler, serviceRepository, context)

        getLanding(securityHandler, serviceRepository, settingsRepository, context)

        apiAction(securityHandler, remoteCommunicationRepository)

        upload(securityHandler, uploadManager)

        get("{...}") {
            call.respondText("No Access\n", status = HttpStatusCode.Forbidden)
        }
    }
}