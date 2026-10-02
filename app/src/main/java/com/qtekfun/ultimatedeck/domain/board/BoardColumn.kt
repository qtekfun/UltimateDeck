// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.board

import java.time.Instant

/** A board column with its visible cards, in order. */
data class BoardColumn(val id: Long, val title: String, val cards: List<CardItem>)

/** A card as shown on the board. [pendingSync] marks local changes not yet on the server. */
data class CardItem(
    val id: Long,
    val title: String,
    val description: String,
    val labels: List<CardLabel>,
    val assignees: List<String>,
    val dueDate: Instant?,
    val attachments: Int,
    val checklistDone: Int,
    val checklistTotal: Int,
    val pendingSync: Boolean
)

/** [color] is Deck's hex RGB without '#'. */
data class CardLabel(val title: String, val color: String)
