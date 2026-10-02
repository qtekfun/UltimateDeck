// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** A user who can work on a board, and so can be assigned to its cards (T16). */
@Entity(
    tableName = "board_member",
    primaryKeys = ["accountId", "boardId", "uid"],
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "boardId"],
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
data class BoardMemberCrossRef(val accountId: Long, val boardId: Long, val uid: String)
