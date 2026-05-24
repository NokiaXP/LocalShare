// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import android.content.Context
import android.util.Log
import com.defname.localshare.data.CallAttributes
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.RemoteCommunicationRepository
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.DisconnectReason
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedWriter

fun BufferedWriter.writeEvent(eventName: String, data: String) {
    write("event: $eventName\n")
    write("data: $data\n\n")
    flush()
}

fun BufferedWriter.writeHeartbeat() {
    write(": heartbeat\n\n")
    flush()
}

fun Route.getEvents(
    securityHandler: ServerSecurityHandler,
    remoteCommunicationRepository: RemoteCommunicationRepository,
    serviceRepository: ServiceRepository,
    settingsRepository: SettingsRepository,
    connectionLogsRepository: ConnectionLogsRepository,
    securityRepository: SecurityRepository,
    json: Json,
    context: Context
) {
    get ("/{token}/events") {

        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Private))
        call.response.header(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
        call.response.header(HttpHeaders.Connection, "keep-alive")
        call.response.header("X-Accel-Buffering", "no")

        call.respondOutputStream {
            // Wir nutzen den OutputStream, um die Kontrolle über das Flushing zu haben
            val writer = bufferedWriter()

            // Standardmäßig gehen wir von einem Server-Shutdown aus
            var disconnectReason: DisconnectReason = DisconnectReason.ServerShutdown

            try {
                coroutineScope {
                    launch {
                        val clientFLow = remoteCommunicationRepository.getStreamForClient(call.request.local.remoteAddress)
                        clientFLow.collect { eventEnvelope ->
                            val jsonElement = json.encodeToJsonElement(eventEnvelope) as JsonObject
                            val eventObject = jsonElement["event"]?.jsonObject ?: return@collect
                            val eventName = eventObject["type"]?.jsonPrimitive?.content ?: "message"
                            val dataObject = buildJsonObject {
                                jsonElement["correlationId"]?.let { put("correlationId", it) }

                                eventObject.forEach { (key, value) ->
                                    if (key != "type") {
                                        put(key, value)
                                    }
                                }
                            }

                            writer.writeEvent(eventName, dataObject.toString())
                        }
                    }

                    // Heartbeat Loop (prüft regelmäßig und hält Verbindung offen)
                    launch {
                        while (true) {
                            delay(settingsRepository.settingsFlow.first().sseHeartbeatPeriodSeconds * 1000L)
                            if (!securityHandler.verifyAccess(call)) {
                                disconnectReason = DisconnectReason.Unexpected.AuthInvalid
                                this@coroutineScope.cancel("Access denied")
                            }
                            writer.writeHeartbeat()
                        }
                    }
                }
            } catch (e: Exception) {
                // Wenn es KEINE CancellationException ist (z.B. IOException weil Socket zu),
                // dann ist der Client weg.
                if (e !is kotlinx.coroutines.CancellationException) {
                    disconnectReason = DisconnectReason.Unexpected.ClientGone
                }
            } finally {
                val connectionId = call.attributes.getOrNull(CallAttributes.connectionId)
                if (connectionId != null) {
                    connectionLogsRepository.clientDisconnected(connectionId, disconnectReason)
                } else {
                    Log.d("FileServerService", "Client disconnected without connectionId")
                }
            }
        }
    }
}
