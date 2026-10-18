// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.defname.localshare.LocaleManager
import com.defname.localshare.R
import com.defname.localshare.data.RuntimeState
import com.defname.localshare.ui.components.ConfirmedTextField
import com.defname.localshare.ui.theme.LocalShareTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onOpenDrawer: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val runtimeState by viewModel.runtimeState.collectAsState()
    val context = LocalContext.current
    val selectedLanguage = LocaleManager.currentLanguage(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroup(title = stringResource(R.string.settings_language_title)) {
                LanguageOption(
                    label = stringResource(R.string.settings_language_english),
                    selected = selectedLanguage == LocaleManager.ENGLISH,
                    onClick = {
                        if (selectedLanguage != LocaleManager.ENGLISH) {
                            LocaleManager.setLanguage(context, LocaleManager.ENGLISH)
                            (context as? android.app.Activity)?.recreate()
                        }
                    }
                )
                LanguageOption(
                    label = stringResource(R.string.settings_language_spanish),
                    selected = selectedLanguage == LocaleManager.SPANISH,
                    onClick = {
                        if (selectedLanguage != LocaleManager.SPANISH) {
                            LocaleManager.setLanguage(context, LocaleManager.SPANISH)
                            (context as? android.app.Activity)?.recreate()
                        }
                    }
                )
            }

            // Server configuration
            SettingsGroup(title = stringResource(R.string.settings_server_title)) {
                // Port Einstellung
                SettingsRow {
                    ConfirmedTextField(
                        value = settings.serverPort.toString(),
                        validator = { input -> input.toIntOrNull().let { it != null && it in 1024.. 65535 } },
                        errorHint = { Text(stringResource(R.string.error_port_range)) },
                        enabled = runtimeState.serviceState == RuntimeState.STOPPED,
                        label = { Text(stringResource(R.string.settings_server_port_label)) },
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            if (runtimeState.serviceState != RuntimeState.STOPPED) {
                                Text(stringResource(R.string.settings_server_port_hint_not_available))
                            } else {
                                Text(stringResource(R.string.settings_server_port_hint))
                            }
                        },
                        onValueChange = { viewModel.setPort(it.toInt()) },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                SettingsRow {
                    ConfirmedTextField(
                        value = settings.serverIdleTimeoutSeconds.toString(),
                        label = { Text(stringResource(R.string.settings_server_timeout_label)) },
                        validator = { input -> input.toIntOrNull() != null },
                        errorHint = { Text(stringResource(R.string.error_seconds_number)) },
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            Text(stringResource(R.string.settings_server_timeout_hint))
                        },
                        onValueChange = { viewModel.setIdleTimeoutSeconds(it.toInt()) },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            // Gruppe 2: Sicherheit
            SettingsGroup(title = stringResource(R.string.settings_security_title)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_regenerate_token_at_app_start_label),
                    subtitle = stringResource(R.string.settings_generate_a_new_security_token_every_time_the_app_starts_hint),
                    checked = settings.regenerateTokenAtAppStart,
                    onCheckedChange =viewModel::setRegenerateTokenAtAppStart
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_security_require_approval_label),
                    subtitle = stringResource(R.string.settings_security_require_approval_hint),
                    checked = settings.requireApproval,
                    onCheckedChange =viewModel::setRequireApproval
                )

                if (settings.requireApproval) {
                    SettingsRow {
                        ConfirmedTextField(
                            value = settings.whitelistEntryTTLSeconds.toString(),
                            label = { Text(stringResource(R.string.settings_security_whitelist_ttl_label)) },
                            validator = { it.toIntOrNull() != null },
                            errorHint = { Text(stringResource(R.string.error_seconds_number)) },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.weight(1f),
                            supportingText = {
                                Text(stringResource(R.string.settings_security_whitelist_ttl_hint))
                            },
                            onValueChange = { viewModel.setWhiteListEntryTTLSeconds(it.toInt()) }
                        )
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_webinterface_title)) {
                SettingsRow {
                    ConfirmedTextField(
                        value = settings.sseHeartbeatPeriodSeconds.toString(),
                        label = { Text(stringResource(R.string.settings_webinterface_heartbeat_label)) },
                        validator = { it.toIntOrNull() != null },
                        errorHint = { Text(stringResource(R.string.error_seconds_number)) },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            Text(stringResource(R.string.settings_webinterface_heartbeat_hint))
                        },
                        onValueChange = { viewModel.setHeartbeatPeriodSeconds(it.toInt()) }
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_misc_title)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_misc_clear_file_list_on_share_label),
                    subtitle = stringResource(R.string.settings_misc_clear_file_list_on_share_hint),
                    checked = settings.clearFileListOnShareIntent,
                    onCheckedChange = viewModel::setClearFilesListOnSendIntent
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_experimental_title)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_upload_feature_title),
                    subtitle = stringResource(R.string.settings_upload_feature_hint),
                    checked = settings.uploadFeatureActive,
                    onCheckedChange = viewModel::setUploadFeatureActive
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    LocalShareTheme {
        SettingsScreen(
            onOpenDrawer = {},
        )
    }
}

@Composable
@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(content = content)
    }
}

@Composable
fun SettingsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
