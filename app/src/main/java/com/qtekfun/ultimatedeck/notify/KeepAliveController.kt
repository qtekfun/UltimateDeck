// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.domain.reminders.KeepAlivePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Starts and stops robust mode's [KeepAliveService] as the settings and the session say (RF-10).
 * It runs whenever the app process starts, which also covers a restart or an update of the app
 * (see [BootReceiver]).
 */
@Singleton
class KeepAliveController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: AccountSession,
    private val settings: SettingsRepository
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(session.activeAccount, settings.settings) { account, settings ->
                KeepAlivePolicy.shouldRun(settings, signedIn = account != null)
            }
                .distinctUntilChanged()
                .collect { run -> if (run) startService() else stopService() }
        }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, KeepAliveService::class.java)
            )
        } catch (_: IllegalStateException) {
            // Android refuses to start it from the background (12+); it starts the next time
            // the app is opened, when this runs again.
        }
    }

    private fun stopService() {
        context.stopService(Intent(context, KeepAliveService::class.java))
    }
}
