// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    val runtimeState = serviceRepository.runtimeState

    fun setPort(port: Int) {
        viewModelScope.launch {
            settingsRepository.setPort(port)
        }
    }

    fun setIdleTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setServerIdleTimeoutSeconds(seconds)
        }
    }

    fun setRequireApproval(requireApproval: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRequireApproval(requireApproval)
        }
    }

    fun setWhiteListEntryTTLSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setWhitelistEntryTTLSeconds(seconds)
        }
    }

    fun setClearFilesListOnSendIntent(clear: Boolean) {
        viewModelScope.launch {
            settingsRepository.setClearFileListOnShareIntent(clear)
        }
    }

    fun setHeartbeatPeriodSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setHeartbeatPeriodSeconds(seconds)
        }
    }

    fun setRegenerateTokenAtAppStart(regenerateTokenAtAppStart: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRegenerateTokenAtAppStart(regenerateTokenAtAppStart)
        }
    }

    fun setUploadFeatureActive(uploadFeatureActive: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUploadFeatureActive(uploadFeatureActive)
        }
    }
}