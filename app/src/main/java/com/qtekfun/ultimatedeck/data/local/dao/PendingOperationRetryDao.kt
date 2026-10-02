// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Retry state of queued operations: postponed after a temporary error or failed for good. */
@Dao
interface PendingOperationRetryDao {
    @Query(
        "UPDATE pending_operation SET attempts = attempts + 1, nextAttemptAt = :nextAttemptAt, " +
            "lastError = :error WHERE id = :id"
    )
    suspend fun recordFailure(id: Long, nextAttemptAt: Instant, error: String?)

    @Query(
        "UPDATE pending_operation SET failed = 1, attempts = attempts + 1, lastError = :error " +
            "WHERE id = :id"
    )
    suspend fun markFailed(id: Long, error: String?)

    /** Makes a failed or postponed operation run at [now]. */
    @Query("UPDATE pending_operation SET failed = 0, nextAttemptAt = :now WHERE id = :id")
    suspend fun resetForRetry(id: Long, now: Instant)

    @Query(
        "SELECT * FROM pending_operation WHERE accountId = :accountId AND failed = 1 ORDER BY id"
    )
    fun observeFailed(accountId: Long): Flow<List<PendingOperationEntity>>
}
