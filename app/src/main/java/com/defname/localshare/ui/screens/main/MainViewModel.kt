// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ServerUrlProvider
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val serverUrlProvider: ServerUrlProvider,
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val _welcomeMessageVisible = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val showWelcomeMessage = settingsRepository.settingsFlow.first().showWelcomeMessage
            _welcomeMessageVisible.update { showWelcomeMessage }
        }
    }

    fun openInBrowser(context: Context, uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun onDismissWelcomeMessage(showNextTime: Boolean = false) {
        _welcomeMessageVisible.update { false }
        settingsRepository.setShowWelcomeMessage(showNextTime)
    }

    fun navigateToTransfers() {
        viewModelScope.launch {
            _navigationEvents.emit(Screen.Transfers.route)
        }
    }

    val state = combine(
        serverUrlProvider.serverUrls,
        _welcomeMessageVisible,
        serviceRepository.runtimeState
    ) { serverUrls, welcomeMessageVisible, runtimeState ->

        val helpLink = "${serverUrls.defaultServerUrl}/static/docs/index.html"

        MainState(
            helpLink = helpLink,
            welcomeMessageVisible = welcomeMessageVisible,
            serverState = runtimeState.serviceState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState()
    )
}
