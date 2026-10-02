// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** A label assigned to a card. */
@Entity(
    tableName = "card_label",
    primaryKeys = ["accountId", "cardId", "labelId"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "cardId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "labelId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "labelId")]
)
data class CardLabelCrossRef(val accountId: Long, val cardId: Long, val labelId: Long)
