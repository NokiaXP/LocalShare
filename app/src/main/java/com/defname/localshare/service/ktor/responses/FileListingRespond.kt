// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.responses

import android.content.Context
import android.util.Log
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ktor.webinterface.InterfaceRenderer
import com.defname.localshare.service.ktor.webinterface.WebInterfaceRenderer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText

suspend fun ApplicationCall.sendFileListing(
    files: List<FileInfo>,
    token: String,
    uploadFeatureActive: Boolean,
    context: Context,
    renderer: InterfaceRenderer = WebInterfaceRenderer(context)
) {
    try {
        val finalHtml = renderer.render(token, files, uploadFeatureActive)
        respondText(finalHtml, ContentType.Text.Html)

    } catch (e: Exception) {
        Log.e("FileServerService", "Error loading template", e)
        respond(HttpStatusCode.InternalServerError, "Error de plantilla")
    }
}