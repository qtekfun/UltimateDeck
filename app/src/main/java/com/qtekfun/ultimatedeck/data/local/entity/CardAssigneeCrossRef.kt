// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** A user assigned to a card. */
@Entity(
    tableName = "card_assignee",
    primaryKeys = ["accountId", "cardId", "uid"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "cardId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DeckUserEntity::class,
            parentColumns = ["accountId", "uid"],
            childColumns = ["accountId", "uid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "uid")]
)
data class CardAssigneeCrossRef(val accountId: Long, val cardId: Long, val uid: String)
