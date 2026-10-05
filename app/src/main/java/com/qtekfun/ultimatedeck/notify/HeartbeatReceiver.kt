// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The heartbeat alarm (RF-10): starts the app if the system had stopped it, sets the reminders
 * again and brings back missed ones. The replan sets the next beat.
 */
@AndroidEntryPoint
class HeartbeatReceiver : BroadcastReceiver() {
    @Inject
    lateinit var beat: ReminderBeat

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                beat.beat()
            } finally {
                pending.finish()
            }
        }
    }
}
