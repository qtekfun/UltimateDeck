// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.reminders.MakerScreens
import com.qtekfun.ultimatedeck.domain.reminders.PhoneMaker

/**
 * Help for phones that stop apps in the background and lose their alarms (RF-10): blocked
 * notifications, and what to switch on in this phone maker's own screens.
 */
@Composable
fun ReminderGuide() {
    val context = LocalContext.current
    val maker = remember { PhoneMaker.of(Build.MANUFACTURER) }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        PermissionNotice(
            granted = { NotificationManagerCompat.from(context).areNotificationsEnabled() },
            message = R.string.settings_reminders_blocked,
            action = R.string.settings_reminders_allow_notifications
        ) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.settings_reminders_guide_title),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            stringResource(guideText(maker)),
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = { openMakerSettings(context, maker) }) {
            Text(
                stringResource(
                    if (MakerScreens.of(maker).isEmpty()) {
                        R.string.settings_reminders_open_app_info
                    } else {
                        R.string.settings_reminders_open_maker
                    }
                )
            )
        }
    }
}

private fun guideText(maker: PhoneMaker) = when (maker) {
    PhoneMaker.COLOROS -> R.string.settings_reminders_guide_coloros
    PhoneMaker.XIAOMI -> R.string.settings_reminders_guide_xiaomi
    PhoneMaker.HUAWEI -> R.string.settings_reminders_guide_huawei
    PhoneMaker.SAMSUNG -> R.string.settings_reminders_guide_samsung
    PhoneMaker.VIVO -> R.string.settings_reminders_guide_vivo
    PhoneMaker.OTHER -> R.string.settings_reminders_guide_generic
}

/**
 * Tries the maker's auto-start screens in order; recent systems refuse some of them, so it ends
 * on the app's info page, where the same switches live.
 */
private fun openMakerSettings(context: Context, maker: PhoneMaker) {
    val opened = MakerScreens.of(maker).any { screen ->
        runCatching {
            context.startActivity(
                Intent().setComponent(ComponentName(screen.packageName, screen.className))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }
    if (!opened) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri()
            )
        )
    }
}
