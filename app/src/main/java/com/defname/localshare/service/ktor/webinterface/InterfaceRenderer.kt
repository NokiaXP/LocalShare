// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.webinterface

import com.defname.localshare.domain.model.FileInfo

interface InterfaceRenderer {
    fun render(token: String, files: List<FileInfo>, uploadFeatureActive: Boolean): String
}