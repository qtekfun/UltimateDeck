// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MissedRemindersTest {
    private val now = Instant.parse("2026-10-05T12:00:00Z")
    private val window = Duration.ofHours(24)
    private val begin = Instant.parse("2026-10-01T00:00:00Z")

    private fun reminder(card: Long, at: String) = Reminder(
        accountId = 1,
        cardId = card,
        boardId = 1,
        boardTitle = "Board",
        title = "Card $card",
        dueDate = Instant.parse(at),
        at = Instant.parse(at)
    )

    private fun pick(
        planned: List<Reminder>,
        shown: Set<ShownReminder> = emptySet(),
        notBefore: Instant = begin
    ) = MissedReminders.pick(planned, shown, now, window, notBefore).map { it.cardId }

    @Test
    fun `a reminder past and never shown is missed, oldest first`() {
        val planned = listOf(
            reminder(2, "2026-10-05T11:00:00Z"),
            reminder(1, "2026-10-05T09:00:00Z")
        )

        assertEquals(listOf(1L, 2L), pick(planned))
    }

    @Test
    fun `reminders shown, still to come or older than the window are not missed`() {
        val shown = reminder(1, "2026-10-05T09:00:00Z")
        val planned = listOf(
            shown,
            reminder(2, "2026-10-05T13:00:00Z"),
            reminder(3, "2026-10-04T11:59:59Z"),
            reminder(4, "2026-10-04T12:00:01Z")
        )

        assertEquals(listOf(4L), pick(planned, setOf(ShownReminder(shown.cardId, shown.at))))
    }

    @Test
    fun `a shown record belongs to its time, so a card with a new date is missed again`() {
        val planned = listOf(reminder(1, "2026-10-05T09:00:00Z"))

        assertEquals(
            listOf(1L),
            pick(planned, setOf(ShownReminder(1, Instant.parse("2026-10-04T09:00:00Z"))))
        )
    }

    @Test
    fun `what was past before recovery began is left alone`() {
        val planned = listOf(
            reminder(1, "2026-10-05T08:00:00Z"),
            reminder(2, "2026-10-05T10:00:00Z")
        )

        assertEquals(listOf(2L), pick(planned, notBefore = Instant.parse("2026-10-05T09:00:00Z")))
    }

    @Test
    fun `a reminder exactly now is missed, one exactly at the limit is not`() {
        val planned = listOf(
            reminder(1, "2026-10-05T12:00:00Z"),
            reminder(2, "2026-10-04T12:00:00Z")
        )

        assertEquals(listOf(1L), pick(planned))
    }

    @Test
    fun `records are kept for as long as the window`() {
        assertEquals(Instant.parse("2026-10-04T12:00:00Z"), MissedReminders.keepAfter(now, window))
    }
}
