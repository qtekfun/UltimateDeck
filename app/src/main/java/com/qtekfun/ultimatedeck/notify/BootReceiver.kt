// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarms do not survive a restart or an update. Receiving this starts the app process, and the
 * app reschedules every reminder ([ReminderCoordinator]) and starts robust mode's service
 * ([KeepAliveController]) when it starts; nothing else to do.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Only the system's own broadcasts; the app start already did the work.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
    }
}
