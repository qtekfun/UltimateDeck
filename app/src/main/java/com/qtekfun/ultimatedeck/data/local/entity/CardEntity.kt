// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import java.time.Instant

/**
 * A card with its current local values. The last values known from the server live in
 * [CardServerSnapshotEntity]; [dirtyFields] marks which ones were changed locally.
 */
@Entity(
    tableName = "card",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = StackEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "stackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "stackId"), Index("accountId", "boardId")]
)
data class CardEntity(
    val accountId: Long,
    val id: Long,
    val boardId: Long,
    val stackId: Long,
    val title: String,
    val description: String = "",
    val order: Int = 0,
    val archived: Boolean = false,
    val dueDate: Instant? = null,
    val done: Instant? = null,
    val ownerUid: String? = null,
    val lastModified: Instant? = null,
    val etag: String? = null,
    val deletedAt: Instant? = null,
    /** Bit mask of [com.qtekfun.ultimatedeck.data.local.model.CardField]s changed locally. */
    val dirtyFields: Int = 0,
    /** When the latest local edit happened, compared with the server's lastModified (SPEC §5). */
    val localModifiedAt: Instant? = null,
    /** Deleted on the server while edited here: the user decides whether to keep it (SPEC §5). */
    val deletedOnServer: Boolean = false
)
