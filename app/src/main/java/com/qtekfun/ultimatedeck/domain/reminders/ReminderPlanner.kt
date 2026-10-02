// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import com.qtekfun.ultimatedeck.data.local.model.DueCardRow
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ReminderLead
import com.qtekfun.ultimatedeck.data.settings.ReminderScope
import java.time.Duration
import java.time.Instant

/** A notification to show at [at] about a card due at [dueDate]. */
data class Reminder(
    val cardId: Long,
    val boardId: Long,
    val boardTitle: String,
    val title: String,
    val dueDate: Instant,
    val at: Instant
)

/** Which due date reminders to schedule (RF-10): a pure decision, the alarms are elsewhere. */
object ReminderPlanner {
    fun plan(cards: List<DueCardRow>, settings: AppSettings, now: Instant): List<Reminder> {
        if (!settings.reminders) return emptyList()
        val lead = settings.reminderLead.duration
        return cards
            .filter { settings.reminderScope == ReminderScope.ALL || it.assignedToMe }
            .map {
                Reminder(
                    it.cardId,
                    it.boardId,
                    it.boardTitle,
                    it.title,
                    it.dueDate,
                    it.dueDate.minus(lead)
                )
            }
            .filter { it.at.isAfter(now) }
    }

    private val ReminderLead.duration: Duration
        get() = when (this) {
            ReminderLead.AT_DUE -> Duration.ZERO
            ReminderLead.ONE_HOUR -> Duration.ofHours(1)
            ReminderLead.ONE_DAY -> Duration.ofDays(1)
        }
}
