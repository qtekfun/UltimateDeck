// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.model

/** A user assigned to a card, as shown on the board. */
data class CardAssigneeRow(val cardId: Long, val uid: String, val displayName: String)
