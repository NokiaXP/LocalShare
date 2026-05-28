package com.defname.localshare.domain.manager

import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FeatureFlagsProvider(
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    val uploadFeature = settingsRepository.settingsFlow
        .map { it.uploadFeatureActive }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
}