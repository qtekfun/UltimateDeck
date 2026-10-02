// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import java.time.Instant

/** A Deck board. [id] is the server id, or a negative local id until it is synced. */
@Entity(
    tableName = "board",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class BoardEntity(
    val accountId: Long,
    val id: Long,
    val title: String,
    val color: String,
    val archived: Boolean = false,
    val ownerUid: String? = null,
    val lastModified: Instant? = null,
    val etag: String? = null,
    val deletedAt: Instant? = null,
    /** ETag of the last columns and cards pulled for this board (T09). */
    val stacksEtag: String? = null
)
