// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
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
import com.qtekfun.ultimatedeck.data.settings.SettingFlag
import com.qtekfun.ultimatedeck.domain.reminders.PhoneMaker

/** Due date reminders (RF-10): on/off, how long before, which cards, exact alarms. */
@Composable
fun RemindersSection(settings: AppSettings, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setFlag(SettingFlag.REMINDERS, granted) }
    Section(R.string.settings_reminders)
    Toggle(
        stringResource(R.string.settings_reminders_enable),
        stringResource(R.string.settings_reminders_hint),
        settings.reminders
    ) { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setFlag(SettingFlag.REMINDERS, enabled)
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
    Choice(stringResource(R.string.settings_reminders_normal), !settings.reminderAlarmClock) {
        viewModel.setFlag(SettingFlag.REMINDER_ALARM_CLOCK, false)
    }
    Choice(stringResource(R.string.settings_reminders_aggressive), settings.reminderAlarmClock) {
        viewModel.setFlag(SettingFlag.REMINDER_ALARM_CLOCK, true)
    }
    val recommended = PhoneMaker.of(Build.MANUFACTURER) != PhoneMaker.OTHER
    Toggle(
        stringResource(R.string.settings_robust),
        stringResource(
            if (recommended) {
                R.string.settings_robust_hint_recommended
            } else {
                R.string.settings_robust_hint
            }
        ),
        settings.robustMode
    ) { viewModel.setFlag(SettingFlag.ROBUST_MODE, it) }
    Toggle(
        stringResource(R.string.settings_reminders_recover),
        stringResource(R.string.settings_reminders_recover_hint),
        settings.recoverMissed
    ) { viewModel.setFlag(SettingFlag.RECOVER_MISSED, it) }
    SystemPermissions(viewModel)
    ReminderGuide()
}

/** Exact alarms and no battery restrictions, so reminders arrive on time. */
@Composable
private fun SystemPermissions(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PermissionNotice(
            granted = viewModel::canScheduleExact,
            message = R.string.settings_reminders_inexact,
            action = R.string.settings_reminders_allow_exact
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:${context.packageName}".toUri()
                )
            )
        }
    }
    val power = context.getSystemService(PowerManager::class.java)
    PermissionNotice(
        granted = { power.isIgnoringBatteryOptimizations(context.packageName) },
        message = R.string.settings_reminders_battery,
        action = R.string.settings_reminders_allow_battery
    ) { requestBatteryExemption(context) }
}

/**
 * A system permission reminders need, while it is missing: why, and a button to the system
 * screen. Checked again when coming back to the app.
 */
@Composable
internal fun PermissionNotice(
    granted: () -> Boolean,
    message: Int,
    action: Int,
    onAllow: () -> Unit
) {
    var allowed by remember { mutableStateOf(granted()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { allowed = granted() }
    }
    if (allowed) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onAllow) { Text(stringResource(action)) }
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

/**
 * Opens the system dialog to exempt the app from battery optimization, so reminders are not
 * delayed or dropped. Lint flags this as against a Google Play Store policy that limits which
 * apps may ask; UltimateDeck is distributed on F-Droid, where that store rule does not apply,
 * and reminders are exactly the use case the exemption exists for. The user decides.
 */
@SuppressLint("BatteryLife")
private fun requestBatteryExemption(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
    )
}
