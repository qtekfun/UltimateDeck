// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DueDatesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val october5 = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()

    @Test
    fun `a picked day keeps the time of the previous due date`() {
        val previous = Instant.parse("2026-10-01T16:30:00Z") // 18:30 in Madrid

        assertEquals(Instant.parse("2026-10-05T16:30:00Z"), dueDateFor(october5, previous, madrid))
    }

    @Test
    fun `a first due date is set at midday`() {
        assertEquals(Instant.parse("2026-10-05T10:00:00Z"), dueDateFor(october5, null, madrid))
    }

    @Test
    fun `the picker shows the local day of the due date`() {
        // 00:30 on the 5th in Madrid is still the 4th in UTC.
        val lateNight = Instant.parse("2026-10-04T22:30:00Z")

        assertEquals(october5, pickerMillisFor(lateNight, madrid))
    }
}
