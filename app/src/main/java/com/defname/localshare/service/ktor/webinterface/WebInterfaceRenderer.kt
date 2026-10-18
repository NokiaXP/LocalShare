// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.webinterface

import android.content.Context
import com.defname.localshare.LocaleManager
import com.defname.localshare.R
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ktor.json.toJsonString

class WebInterfaceRenderer(
    private val context: Context
) : InterfaceRenderer {
    private val templateFile: String
        get() = if (LocaleManager.currentLanguage(context) == LocaleManager.SPANISH) "web/template.html" else "web/template_en.html"

    override fun render(token: String, files: List<FileInfo>, uploadFeatureActive: Boolean): String {

        // 1. Template aus Assets laden
        val template = context.assets.open(templateFile).bufferedReader().use { it.readText() }

        // 2. Den dynamischen Teil für die Dateien bauen (HTML-Schnipsel)

        val jsonFileList = files.map { it.toJsonString() }

        // 3. Platzhalter im Template ersetzen
        val finalHtml = template
            .replace("{{token}}", token)
            .replace("{{appname}}", context.applicationInfo.loadLabel(context.packageManager).toString())
            .replace("{{appurl}}", context.getString(R.string.app_url))
            .replace("{{filelist}}", "[" + jsonFileList.joinToString(",\n") + "]")
            .replace("{{upload_enabled}}", if (uploadFeatureActive) "true" else "false")

        return finalHtml
    }
}