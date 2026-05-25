// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UploadPermissionStatus {
    @SerialName("granted")
    GRANTED,
    @SerialName("denied")
    DENIED,
    @SerialName("pending")
    PENDING,
    @SerialName("not_requested")
    NOT_REQUESTED,
    @SerialName("unknown")
    UNKNOWN
}
data class WhiteListEntry(
    val ip: String,
    val timestamp: Long = System.currentTimeMillis(),
    val uploadPermission: UploadPermissionStatus = UploadPermissionStatus.NOT_REQUESTED
)

fun WhiteListEntry.isStillValid(ttlSeconds: Int = 60 * 60, now: Long = System.currentTimeMillis()): Boolean {
    return timestamp + ttlSeconds * 1000L >= now
}
