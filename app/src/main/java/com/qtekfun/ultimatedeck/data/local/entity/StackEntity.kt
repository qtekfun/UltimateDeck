// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import java.time.Instant

/** A board column ("stack" in Deck). */
@Entity(
    tableName = "stack",
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
data class StackEntity(
    val accountId: Long,
    val id: Long,
    val boardId: Long,
    val title: String,
    val order: Int,
    val lastModified: Instant? = null,
    val etag: String? = null,
    val deletedAt: Instant? = null
)
