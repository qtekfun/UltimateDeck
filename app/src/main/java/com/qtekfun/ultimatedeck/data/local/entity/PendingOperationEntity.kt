// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.local.model.OperationType
import java.time.Instant

/**
 * A local change waiting to be sent to the server (RF-08). Operations run in [id] order per
 * account; [payload] holds the operation data as JSON.
 */
@Entity(
    tableName = "pending_operation",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "nextAttemptAt"), Index("accountId", "entityType", "entityId")]
)
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: OperationType,
    val entityType: EntityType,
    val entityId: Long,
    val payload: String,
    val createdAt: Instant,
    val attempts: Int = 0,
    val nextAttemptAt: Instant = createdAt,
    val lastError: String? = null,
    /** Refused for good by the server: no automatic retries until the user retries or discards it. */
    @ColumnInfo(defaultValue = "0")
    val failed: Boolean = false,
    /** When it was last handed to the server; it may have arrived even without an answer (T10). */
    val startedAt: Instant? = null
)
