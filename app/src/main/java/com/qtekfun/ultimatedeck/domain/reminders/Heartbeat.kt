// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import java.time.Duration
import java.time.Instant

/**
 * A silent alarm that sets every reminder again and brings back missed ones, for phones that
 * freeze the app and lose its alarms (RF-10). It only runs while some reminder is still to come.
 */
object Heartbeat {
    private const val MINUTES = 30L
    val INTERVAL: Duration = Duration.ofMinutes(MINUTES)

    /** When to beat next, or null when no reminder is left to protect. */
    fun next(now: Instant, pending: List<Reminder>, interval: Duration = INTERVAL): Instant? =
        if (pending.any { it.at.isAfter(now) }) now.plus(interval) else null
}
