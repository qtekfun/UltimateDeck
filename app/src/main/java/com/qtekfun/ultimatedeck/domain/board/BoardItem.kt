// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.board

/** A board as listed: [color] is Deck's hex RGB without '#'. */
data class BoardItem(val id: Long, val title: String, val color: String)
