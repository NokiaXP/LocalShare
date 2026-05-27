// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.model

import android.net.Uri
import com.defname.localshare.util.sizeAsString

data class FileInfo(
    val id: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val iconFile: String
)

fun FileInfo.sizeAsString(): String {
    return this.size.sizeAsString()
}