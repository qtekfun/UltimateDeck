// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestDeliveryTest {
    private val scheduled = Instant.parse("2026-10-05T10:01:00Z")

    @Test
    fun `nothing is known before a test is scheduled`() {
        assertNull(TestDelivery.of(null, null, scheduled))
    }

    @Test
    fun `an alarm within the tolerance is on time`() {
        val arrived = scheduled.plusSeconds(60)

        assertEquals(
            TestDelivery.OnTime(scheduled, arrived),
            TestDelivery.of(scheduled, arrived, arrived)
        )
    }

    @Test
    fun `a later alarm says how many minutes late`() {
        val arrived = scheduled.plusSeconds(5 * 60L)

        assertEquals(
            TestDelivery.Late(scheduled, arrived, 5),
            TestDelivery.of(scheduled, arrived, arrived)
        )
    }

    @Test
    fun `without an alarm it waits, then gives up`() {
        assertEquals(
            TestDelivery.Waiting(scheduled),
            TestDelivery.of(scheduled, null, scheduled.plusSeconds(10 * 60L))
        )
        assertEquals(
            TestDelivery.Missing(scheduled),
            TestDelivery.of(scheduled, null, scheduled.plusSeconds(10 * 60L + 1))
        )
    }

    @Test
    fun `only the reminder with the test id is a test`() {
        val at = scheduled
        assertTrue(Reminder(1, TEST_CARD_ID, 0, "", "", at, at).isTest)
        assertFalse(Reminder(1, 5, 0, "", "", at, at).isTest)
    }
}
