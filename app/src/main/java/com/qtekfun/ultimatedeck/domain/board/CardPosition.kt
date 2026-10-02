// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.board

/**
 * Position of a card on a board: the index of its column and its index inside that column.
 */
data class CardPosition(val column: Int, val index: Int)
