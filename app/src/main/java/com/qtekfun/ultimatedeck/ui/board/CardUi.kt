// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

import java.time.LocalDate

/** A card as shown on the board; [description] is its markdown. */
data class CardUi(
    val id: Long,
    val title: String,
    val description: String = "",
    val labels: List<LabelUi> = emptyList(),
    val assignees: List<String> = emptyList(),
    val dueDate: LocalDate? = null,
    val attachments: Int = 0,
    val checklistDone: Int = 0,
    val checklistTotal: Int = 0,
    val pendingSync: Boolean = false
)
