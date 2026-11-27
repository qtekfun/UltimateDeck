// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DueStateTest {
    private val today = LocalDate.parse("2026-12-31")

    @Test
    fun `a due date is overdue, today, tomorrow or later`() {
        assertEquals(DueState.OVERDUE, dueState(today.minusDays(1), today))
        assertEquals(DueState.TODAY, dueState(today, today))
        assertEquals(DueState.TOMORROW, dueState(LocalDate.parse("2027-01-01"), today))
        assertEquals(DueState.LATER, dueState(today.plusDays(2), today))
    }
}
