// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HeartbeatTest {
    private val now = Instant.parse("2026-10-05T10:00:00Z")

    private fun reminder(at: Instant) = Reminder(1, 1, 1, "Board", "Card", at, at)

    @Test
    fun `beats one interval from now while a reminder is still to come`() {
        val pending = listOf(reminder(now.plusSeconds(3600)))

        assertEquals(now.plus(Heartbeat.INTERVAL), Heartbeat.next(now, pending))
        assertEquals(now.plusSeconds(60), Heartbeat.next(now, pending, Duration.ofMinutes(1)))
    }

    @Test
    fun `does not beat when nothing is pending or everything is past`() {
        assertNull(Heartbeat.next(now, emptyList()))
        assertNull(Heartbeat.next(now, listOf(reminder(now), reminder(now.minusSeconds(5)))))
    }
}
