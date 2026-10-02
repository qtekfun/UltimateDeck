// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import com.qtekfun.ultimatedeck.data.local.model.DueCardRow
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ReminderLead
import com.qtekfun.ultimatedeck.data.settings.ReminderScope
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReminderPlannerTest {
    private val now = Instant.parse("2026-10-02T10:00:00Z")
    private val on = AppSettings(reminders = true)

    private fun due(id: Long, at: String, mine: Boolean = true) =
        DueCardRow(id, 1, "Board", "Card $id", Instant.parse(at), mine)

    @Test
    fun `nothing is planned while reminders are off`() {
        assertEquals(
            emptyList<Reminder>(),
            ReminderPlanner.plan(listOf(due(1, "2026-10-03T10:00:00Z")), AppSettings(), now)
        )
    }

    @Test
    fun `reminders ring at the due time, minus the chosen lead, only in the future`() {
        val cards =
            listOf(
                due(1, "2026-10-02T10:30:00Z"),
                due(2, "2026-10-04T10:00:00Z"),
                due(3, "2026-10-01T10:00:00Z")
            )

        val atDue = ReminderPlanner.plan(cards, on, now)
        val hourBefore = ReminderPlanner.plan(
            cards,
            on.copy(reminderLead = ReminderLead.ONE_HOUR),
            now
        )
        val dayBefore = ReminderPlanner.plan(
            cards,
            on.copy(reminderLead = ReminderLead.ONE_DAY),
            now
        )

        assertEquals(
            listOf(1L to "2026-10-02T10:30:00Z", 2L to "2026-10-04T10:00:00Z"),
            atDue.map {
                it.cardId to
                    it.at.toString()
            }
        )
        assertEquals(
            listOf(2L to "2026-10-04T09:00:00Z"),
            hourBefore.map {
                it.cardId to
                    it.at.toString()
            }
        )
        assertEquals(
            listOf(2L to "2026-10-03T10:00:00Z"),
            dayBefore.map {
                it.cardId to
                    it.at.toString()
            }
        )
        assertEquals(
            Reminder(
                2,
                1,
                "Board",
                "Card 2",
                Instant.parse("2026-10-04T10:00:00Z"),
                Instant.parse("2026-10-03T10:00:00Z")
            ),
            dayBefore.single()
        )
    }

    @Test
    fun `the scope decides whether cards of others count`() {
        val cards =
            listOf(
                due(1, "2026-10-03T10:00:00Z", mine = true),
                due(2, "2026-10-03T10:00:00Z", mine = false)
            )

        assertEquals(listOf(1L), ReminderPlanner.plan(cards, on, now).map { it.cardId })
        assertEquals(
            listOf(1L, 2L),
            ReminderPlanner.plan(cards, on.copy(reminderScope = ReminderScope.ALL), now).map {
                it.cardId
            }
        )
    }
}
