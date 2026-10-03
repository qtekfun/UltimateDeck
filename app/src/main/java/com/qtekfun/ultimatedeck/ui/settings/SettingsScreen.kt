// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.BuildConfig
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.SettingFlag
import com.qtekfun.ultimatedeck.data.settings.ThemeMode
import com.qtekfun.ultimatedeck.ui.session.LogoutAction

/** Settings (T18): appearance, language and account. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    accountName: String,
    server: String,
    onLogOut: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.board_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppearanceSection(settings, viewModel)
            if (AppLanguages.supported) LanguageSection()
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            RemindersSection(settings, viewModel)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Section(R.string.settings_management)
            Toggle(
                stringResource(R.string.settings_allow_deleting),
                stringResource(R.string.settings_allow_deleting_hint),
                settings.allowDeleting,
                onChange = { viewModel.setFlag(SettingFlag.ALLOW_DELETING, it) }
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            BackupSection()
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Section(R.string.settings_account)
            Text(
                accountName,
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                server,
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.padding(horizontal = 8.dp)) { LogoutAction(accountName, onLogOut) }
            AppVersion()
        }
    }
}

/** Which version is installed, at the foot of Settings. */
@Composable
private fun AppVersion() {
    Text(
        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    )
}

@Composable
private fun AppearanceSection(settings: AppSettings, viewModel: SettingsViewModel) {
    val dark = settings.theme != ThemeMode.LIGHT
    Section(R.string.settings_appearance)
    ThemeMode.entries.forEach { mode ->
        Choice(stringResource(themeLabel(mode)), settings.theme == mode) {
            viewModel.setTheme(mode)
        }
    }
    Toggle(
        stringResource(R.string.settings_amoled),
        stringResource(R.string.settings_amoled_hint),
        settings.amoled,
        enabled = dark,
        onChange = { viewModel.setFlag(SettingFlag.AMOLED, it) }
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Toggle(
            stringResource(R.string.settings_dynamic_color),
            stringResource(R.string.settings_dynamic_color_hint),
            settings.dynamicColor,
            onChange = { viewModel.setFlag(SettingFlag.DYNAMIC_COLOR, it) }
        )
    }
}

@Composable
private fun LanguageSection() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    var language by remember { mutableStateOf(AppLanguages.current(context)) }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Section(R.string.settings_language)
    AppLanguage.entries.forEach { option ->
        Choice(stringResource(languageLabel(option)), language == option) {
            language = option
            AppLanguages.set(context, option)
        }
    }
}

@Composable
internal fun Section(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
internal fun Choice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}

@Composable
internal fun Toggle(
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

private fun languageLabel(language: AppLanguage) = when (language) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.ENGLISH -> R.string.settings_language_english
    AppLanguage.SPANISH -> R.string.settings_language_spanish
}
