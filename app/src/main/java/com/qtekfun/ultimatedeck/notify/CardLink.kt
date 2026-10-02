// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.Intent

private const val EXTRA_CARD = "com.qtekfun.ultimatedeck.CARD"
private const val EXTRA_BOARD = "com.qtekfun.ultimatedeck.BOARD"
private const val EXTRA_BOARD_TITLE = "com.qtekfun.ultimatedeck.BOARD_TITLE"

/** A card to open, e.g. from a reminder notification. */
data class CardLink(val boardId: Long, val boardTitle: String, val cardId: Long) {
    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_CARD, cardId)
        .putExtra(EXTRA_BOARD, boardId)
        .putExtra(EXTRA_BOARD_TITLE, boardTitle)

    companion object {
        fun from(intent: Intent?): CardLink? {
            if (intent == null || !intent.hasExtra(EXTRA_CARD)) return null
            return CardLink(
                boardId = intent.getLongExtra(EXTRA_BOARD, 0),
                boardTitle = intent.getStringExtra(EXTRA_BOARD_TITLE).orEmpty(),
                cardId = intent.getLongExtra(EXTRA_CARD, 0)
            )
        }
    }
}
