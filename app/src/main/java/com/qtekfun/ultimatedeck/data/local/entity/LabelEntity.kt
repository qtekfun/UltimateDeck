// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** A board label; [color] is the hex RGB Deck stores, without the leading #. */
@Entity(
    tableName = "label",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "boardId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "boardId")]
)
data class LabelEntity(
    val accountId: Long,
    val id: Long,
    val boardId: Long,
    val title: String,
    val color: String
)
