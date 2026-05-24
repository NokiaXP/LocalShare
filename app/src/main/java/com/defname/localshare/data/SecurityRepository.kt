// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.data

import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.model.UploadPermissionStatus
import com.defname.localshare.domain.model.WhiteListEntry
import com.defname.localshare.domain.model.isStillValid
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class SecurityRepository(
    private val settingsRepository: SettingsRepository,
    private val repositoryScope: CoroutineScope
) {
    private val settings = settingsRepository.settingsFlow
        .stateIn(repositoryScope, SharingStarted.Eagerly, Settings())


    private val _blacklist = MutableStateFlow(setOf<String>())
    val blacklist = _blacklist.asStateFlow()

    private val _whitelist = MutableStateFlow(listOf<WhiteListEntry>())
    val whitelist = _whitelist.asStateFlow()

    private val _pendingRequests = MutableStateFlow(emptyList<String>())
    val pendingRequests = _pendingRequests.asStateFlow()

    private val _pendingUploadPermissionRequests = MutableStateFlow(emptyList<String>())
    val pendingUploadPermissionRequests = _pendingUploadPermissionRequests.asStateFlow()

    init {
        initializeToken()
        startCleaningJob()
    }

    private fun initializeToken() {
        repositoryScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (settings.token.isEmpty() || settings.regenerateTokenAtAppStart) {
                settingsRepository.setToken(generateRandomToken())
            }
        }
    }

    private fun startCleaningJob() {
        repositoryScope.launch {
            while (isActive) {
                delay(60_000)
                cleanupExpiredWhitelistEntries()
            }
        }
    }

    private fun cleanupExpiredWhitelistEntries() {
        val now = System.currentTimeMillis()
        _whitelist.update { it.filter { entry -> entry.isStillValid(settings.value.whitelistEntryTTLSeconds, now) } }
    }

    fun checkToken(providedToken: String): Boolean { return providedToken == settings.value.token }

    fun isApprovalRequired(): Boolean { return settings.value.requireApproval }

    fun addToBlacklist(ip: String) {
        removeFromWhitelist(ip)
        _blacklist.update { it + ip }
    }
    fun removeFromBlacklist(ip: String) { _blacklist.update { it - ip } }
    fun isBlacklisted(ip: String): Boolean { return _blacklist.value.contains(ip) }

    fun addToWhitelist(ip: String) { _whitelist.update { it + WhiteListEntry(ip, System.currentTimeMillis()) } }
    fun removeFromWhitelist(ip: String) { _whitelist.update { it.filter { entry -> entry.ip != ip } } }
    fun isWhitelisted(ip: String): Boolean { return _whitelist.value.any { it.ip == ip && it.isStillValid(settings.value.whitelistEntryTTLSeconds)  } }

    fun addPendingRequest(ip: String): Boolean {
        var added = false
        _pendingRequests.update {
            if (!it.contains(ip)) {
                added = true
                it + ip
            } else {
                it
            }
        }
        return added
    }

    fun removePendingRequest(ip: String) {
        _pendingRequests.update { it - ip }
    }

    fun requestUploadPermission(ip: String) {
        _pendingUploadPermissionRequests.update {
            if (!it.contains(ip)) {
                it + ip
            } else {
                it
            }
        }
    }

    fun grantUploadPermission(ip: String) {
        _pendingUploadPermissionRequests.update { it - ip }
        _whitelist.update {
            val entry: WhiteListEntry = it.find { entry -> entry.ip == ip } ?: return@update it
            it.filter { entry -> entry.ip != ip } + entry.copy(uploadPermission = UploadPermissionStatus.GRANTED)
        }
    }

    fun denyUploadPermission(ip: String) {
        _pendingUploadPermissionRequests.update { it - ip }
        _whitelist.update {
            val entry: WhiteListEntry = it.find { entry -> entry.ip == ip } ?: return@update it
            it.filter { entry -> entry.ip != ip } + entry.copy(uploadPermission = UploadPermissionStatus.DENIED)
        }
    }

    companion object {
        fun generateRandomToken(): String {
            return UUID.randomUUID().toString().take(6)
        }
    }
}