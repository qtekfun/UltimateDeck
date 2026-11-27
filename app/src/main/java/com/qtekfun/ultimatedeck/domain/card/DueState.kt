// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import java.time.LocalDate

/** How pressing a due date is. */
enum class DueState { OVERDUE, TODAY, TOMORROW, LATER }

fun dueState(due: LocalDate, today: LocalDate): DueState = when {
    due.isBefore(today) -> DueState.OVERDUE
    due == today -> DueState.TODAY
    due == today.plusDays(1) -> DueState.TOMORROW
    else -> DueState.LATER
}
