// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import java.time.Duration
import java.time.Instant

/** A reminder that was shown: its card and the time it was for, since cards are reused. */
data class ShownReminder(val cardId: Long, val at: Instant)

/**
 * Reminders whose time passed without showing, because the system stopped the app and its
 * alarms (RF-10). Only those within [window] count: an older one would be noise, not a
 * reminder. [notBefore] leaves out what was already past when recovery began (an update, or
 * reminders just turned on), so nothing old floods the screen.
 */
object MissedReminders {
    fun pick(
        planned: List<Reminder>,
        shown: Set<ShownReminder>,
        now: Instant,
        window: Duration,
        notBefore: Instant
    ): List<Reminder> {
        val since = maxOf(now.minus(window), notBefore)
        return planned.filter { reminder ->
            !reminder.at.isAfter(now) && reminder.at.isAfter(since) &&
                ShownReminder(reminder.cardId, reminder.at) !in shown
        }.sortedBy { it.at }
    }

    /** Records older than this can go: no reminder that old is picked any more. */
    fun keepAfter(now: Instant, window: Duration): Instant = now.minus(window)
}
