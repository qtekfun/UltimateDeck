// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Storage of the operation queue; retries and ordering rules live in the queue (T07). */
@Dao
interface PendingOperationDao {
    @Insert
    suspend fun enqueue(operation: PendingOperationEntity): Long

    /** Operations of the account that may run at [now], in the order they were queued. */
    @Query(
        "SELECT * FROM pending_operation WHERE accountId = :accountId AND nextAttemptAt <= :now ORDER BY id"
    )
    suspend fun ready(accountId: Long, now: Instant): List<PendingOperationEntity>

    @Query(
        "UPDATE pending_operation SET attempts = attempts + 1, nextAttemptAt = :nextAttemptAt, " +
            "lastError = :error WHERE id = :id"
    )
    suspend fun recordFailure(id: Long, nextAttemptAt: Instant, error: String?)

    @Query("DELETE FROM pending_operation WHERE id = :id")
    suspend fun delete(id: Long)

    /** Points queued operations at the server id once a local entity was created remotely. */
    @Query(
        "UPDATE pending_operation SET entityId = :newId " +
            "WHERE accountId = :accountId AND entityType = :entityType AND entityId = :oldId"
    )
    suspend fun remapEntityId(accountId: Long, entityType: EntityType, oldId: Long, newId: Long)

    @Query("SELECT COUNT(*) FROM pending_operation WHERE accountId = :accountId")
    fun observeCount(accountId: Long): Flow<Int>
}
