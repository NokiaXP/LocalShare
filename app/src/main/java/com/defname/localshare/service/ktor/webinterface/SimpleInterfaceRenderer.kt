// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.webinterface

import android.content.Context
import com.defname.localshare.R
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.util.sizeAsString


@Suppress("HardCodedStringLiteral")
const val FILE_ENTRY_TEMPLATE = """<li>
        <img class="icon" src="/{{token}}/icon/{{icon}}">
        <span class="filename">{{filename}}</span>
        <span class="filesize">{{filesize}}</span>
        <span class="buttons">
            <a href="{{token}}/file/{{fileid}}">Abrir</a>
            <a href="{{token}}/file/{{fileid}}">Descargar</a>
        </span>
    </li>"""

class SimpleInterfaceRenderer(
    private val context: Context,
) : InterfaceRenderer {
    private val templateFile: String = "web/simple/template.html"
    private val fileEntryTemplateFile: String = "web/simple/file_item_template.html"

    override fun render(token: String, files: List<FileInfo>, uploadFeatureActive: Boolean): String {
        // 1. Template aus Assets laden
        val template = context.assets.open(templateFile).bufferedReader().use { it.readText() }
        val fileEntryTemplate = context.assets.open(fileEntryTemplateFile).bufferedReader().use { it.readText() }

        // 2. Den dynamischen Teil für die Dateien bauen (HTML-Schnipsel)

        val htmlFileList = files.map {
            fileEntryTemplate.replace("{{token}}", token)
                .replace("{{filename}}", it.name)
                .replace("{{icon}}", it.iconFile)
                .replace("{{mimetype}}", it.mimeType)
                .replace("{{filesize}}", it.size.sizeAsString())
                .replace("{{fileid}}", it.id)
        }

        // 3. Platzhalter im Template ersetzen
        val finalHtml = template
            .replace("{{token}}", token)
            .replace("{{appname}}", context.applicationInfo.loadLabel(context.packageManager).toString())
            .replace("{{appurl}}", context.getString(R.string.app_url))
            .replace("{{filelist}}", htmlFileList.joinToString("\n"))

        return finalHtml
    }
}