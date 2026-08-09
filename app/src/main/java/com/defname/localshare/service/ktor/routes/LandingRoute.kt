// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import android.content.Context
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.responses.sendFileListing
import com.defname.localshare.service.ktor.responses.sendZip
import com.defname.localshare.service.ktor.webinterface.InterfaceRenderer
import com.defname.localshare.service.ktor.webinterface.SimpleInterfaceRenderer
import com.defname.localshare.service.ktor.webinterface.WebInterfaceRenderer
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first

fun Route.getLanding(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    settingsRepository: SettingsRepository,
    context: Context
) {
    get("/{token}/") {
        val download = (call.queryParameters["download"] != null)
        val noJS = (call.queryParameters["nojs"] != null)

        //  1. validate
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val fileList = serviceRepository.fileList.first()

        // 2. download all or file listing
        if (download) {
            return@get call.sendZip(
                files = fileList,
                securityHandler = securityHandler,
                context = context
            )
        } else {
            val token = call.parameters["token"]!!
            val renderer: InterfaceRenderer = if (noJS) SimpleInterfaceRenderer(context, "web/simple-template.html") else  WebInterfaceRenderer(context)
            return@get call.sendFileListing(
                files = fileList,
                token = token,
                uploadFeatureActive = settingsRepository.settingsFlow.first().uploadFeatureActive,
                context = context,
                renderer = renderer
            )
        }
    }
}