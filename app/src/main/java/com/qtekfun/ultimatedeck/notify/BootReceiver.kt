// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarms do not survive a restart. Receiving this starts the app process, and the app
 * reschedules every reminder when it starts (see [ReminderCoordinator]); nothing else to do.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Only the system's boot broadcast; the app start already rescheduled the reminders.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    }
}
