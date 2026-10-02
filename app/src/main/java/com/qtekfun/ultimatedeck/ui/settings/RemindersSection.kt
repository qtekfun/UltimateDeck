// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ReminderLead
import com.qtekfun.ultimatedeck.data.settings.ReminderScope

/** Due date reminders (RF-10): on/off, how long before, which cards, exact alarms. */
@Composable
fun RemindersSection(settings: AppSettings, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setReminders(granted) }
    Section(R.string.settings_reminders)
    Toggle(
        stringResource(R.string.settings_reminders_enable),
        stringResource(R.string.settings_reminders_hint),
        settings.reminders
    ) { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setReminders(enabled)
        }
    }
    if (!settings.reminders) return
    ReminderLead.entries.forEach { lead ->
        Choice(stringResource(leadLabel(lead)), settings.reminderLead == lead) {
            viewModel.setReminderLead(lead)
        }
    }
    ReminderScope.entries.forEach { scope ->
        Choice(stringResource(scopeLabel(scope)), settings.reminderScope == scope) {
            viewModel.setReminderScope(scope)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ExactAlarmNotice(viewModel) {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:${context.packageName}".toUri()
                )
            )
        }
    }
}

/** Shown while exact alarms are not allowed; checked again when coming back from the system. */
@Composable
private fun ExactAlarmNotice(viewModel: SettingsViewModel, onAllow: () -> Unit) {
    var exact by remember { mutableStateOf(viewModel.canScheduleExact()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            exact = viewModel.canScheduleExact()
        }
    }
    if (exact) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.settings_reminders_inexact),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onAllow) {
            Text(stringResource(R.string.settings_reminders_allow_exact))
        }
    }
}

private fun leadLabel(lead: ReminderLead) = when (lead) {
    ReminderLead.AT_DUE -> R.string.settings_reminders_at_due
    ReminderLead.ONE_HOUR -> R.string.settings_reminders_one_hour
    ReminderLead.ONE_DAY -> R.string.settings_reminders_one_day
}

private fun scopeLabel(scope: ReminderScope) = when (scope) {
    ReminderScope.ASSIGNED_TO_ME -> R.string.settings_reminders_mine
    ReminderScope.ALL -> R.string.settings_reminders_all
}
