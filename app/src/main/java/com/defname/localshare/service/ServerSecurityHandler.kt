// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service

import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.domain.model.UploadPermissionStatus
import com.defname.localshare.service.notification.NotificationHelper
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.RoutingCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.NetworkInterface

class ServerSecurityHandler(
    private val securityRepository: SecurityRepository,
    private val notificationHelper: NotificationHelper
) {
    /**
     * Checks if the request comes from the same device.
     * To prevent spoofing, we verify that if the remote address claims to be a loopback,
     * the local receiving address must also be a loopback.
     */
    private suspend fun isSameDevice(remoteHost: String, localHost: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val remoteAddr = InetAddress.getByName(remoteHost)
            val localAddr = InetAddress.getByName(localHost)

            // If the remote address is a loopback address (127.0.0.1 / ::1 / localhost),
            // it is ONLY valid if the request was also received on a loopback interface.
            // External attackers cannot (normally) reach the local loopback interface.
            if (remoteAddr.isLoopbackAddress) {
                return@withContext localAddr.isLoopbackAddress
            }

            // Additionally check if the remote IP matches any of our own network interfaces (WLAN, etc.)
            // If I call my own LAN-IP from the same device, this will trigger.
            NetworkInterface.getByInetAddress(remoteAddr) != null
        } catch (e: Exception) {
            // Fallback: If DNS resolution fails or anything else happens, 
            // we only trust exact matches of non-spoofable IP strings.
            (remoteHost == "127.0.0.1" && localHost == "127.0.0.1") ||
            (remoteHost == "::1" && localHost == "::1")
        }
    }

    suspend fun verifyAccess(call: RoutingCall): Boolean {
        val remoteHost = call.request.local.remoteHost
        val localHost = call.request.local.localHost

        val token = call.parameters["token"] ?: return false
        if (!securityRepository.checkToken(token)) {
            return false
        }
        if (securityRepository.isBlacklisted(remoteHost)) {
            return false
        }
        if (securityRepository.isWhitelisted(remoteHost)) {
            return true
        }
        if (!securityRepository.isApprovalRequired()) {
            return true
        }

        // Secure same-device check
        if (isSameDevice(remoteHost, localHost)) {
            return true
        }

        var ipAddedByThisCall = false
        if (securityRepository.addPendingRequest(remoteHost)) {
            ipAddedByThisCall = true
        }

        return try {
            withTimeout(30_000L) {
                 combine(securityRepository.whitelist, securityRepository.blacklist) { wl, bl ->
                    if (bl.contains(remoteHost)) {
                        false
                    } else if (wl.any { it.ip == remoteHost }) {
                        true
                    } else {
                        null
                    }
                }.first { it != null } ?: false
            }
        } catch (e: TimeoutCancellationException) {
            false
        } finally {
            if (ipAddedByThisCall) {
                securityRepository.removePendingRequest(remoteHost)
            }
        }
    }

    fun mayContinueDownload(request: ApplicationRequest): Boolean {
        val remoteHost = request.local.remoteHost
        val localHost = request.local.localHost

        if (securityRepository.isBlacklisted(remoteHost)) {
            return false
        }
        if (securityRepository.isWhitelisted(remoteHost)) {
            return true
        }

        return !securityRepository.isApprovalRequired()
    }

    fun approveIp(ip: String) {
        securityRepository.addToWhitelist(ip)
    }

    fun blockIp(ip: String) {
        securityRepository.addToBlacklist(ip)
    }

    fun grantUploadPermission(ip: String) {
        securityRepository.grantUploadPermission(ip)
    }

    fun denyUploadPermission(ip: String) {
        securityRepository.denyUploadPermission(ip)
    }

    fun requestUploadPermission(ip: String) {
        val whiteListEntry = securityRepository.whitelist.value.find { it.ip == ip }
        if (whiteListEntry == null) {
            return
        }
        securityRepository.requestUploadPermission(ip)
        notificationHelper.showUploadPermissionNotification(ip)
    }

    fun hasUploadPermission(ip: String): Boolean {
        val whiteListEntry = securityRepository.whitelist.value.find { it.ip == ip } ?: return false
        return whiteListEntry.uploadPermission == UploadPermissionStatus.GRANTED
    }
}
