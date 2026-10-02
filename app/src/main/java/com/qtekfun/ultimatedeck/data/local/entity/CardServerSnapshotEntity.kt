// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import java.time.Instant

/**
 * The last state of a card known from the server (SPEC §5). Together with the local values in
 * [CardEntity] and its dirty fields, it lets the resolver tell local and remote changes apart.
 */
@Entity(
    tableName = "card_server_snapshot",
    primaryKeys = ["accountId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "cardId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class CardServerSnapshotEntity(
    val accountId: Long,
    val cardId: Long,
    val title: String,
    val description: String,
    val stackId: Long,
    val order: Int,
    val archived: Boolean,
    val dueDate: Instant?,
    val done: Instant?,
    val labelIds: List<Long>,
    val assigneeUids: List<String>,
    val lastModified: Instant?,
    val etag: String?
)
