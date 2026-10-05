// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import com.qtekfun.ultimatedeck.data.settings.AppSettings

/** When robust mode's service must be running (RF-10): a pure decision, the service is elsewhere. */
object KeepAlivePolicy {
    /** Only while there are reminders to protect: robust mode on, reminders on, and a session. */
    fun shouldRun(settings: AppSettings, signedIn: Boolean): Boolean =
        settings.robustMode && settings.reminders && signedIn
}
