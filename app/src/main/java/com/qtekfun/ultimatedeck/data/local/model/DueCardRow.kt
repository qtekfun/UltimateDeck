// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.model

import java.time.Instant

/** An open card with a due date, as reminders need it (RF-10). */
data class DueCardRow(
    val cardId: Long,
    val boardId: Long,
    val boardTitle: String,
    val title: String,
    val dueDate: Instant,
    val assignedToMe: Boolean
)
