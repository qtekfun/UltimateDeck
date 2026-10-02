// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

/** The Deck API of one account, split by area. Created by [DeckApiFactory]. */
data class DeckApi(
    val boards: BoardApi,
    val cards: CardApi,
    val cardMetadata: CardMetadataApi,
    val attachments: AttachmentApi
)
