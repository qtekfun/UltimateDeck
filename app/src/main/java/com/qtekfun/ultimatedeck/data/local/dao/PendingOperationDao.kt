// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import kotlinx.coroutines.flow.Flow

/** Storage of the operation queue; ordering, merging and retries live in OperationQueue (T07). */
@Dao
interface PendingOperationDao {
    @Insert
    suspend fun enqueue(operation: PendingOperationEntity): Long

    /** Every operation of the account, failed ones included, in the order they were queued. */
    @Query("SELECT * FROM pending_operation WHERE accountId = :accountId ORDER BY id")
    suspend fun all(accountId: Long): List<PendingOperationEntity>

    @Query(
        "SELECT * FROM pending_operation WHERE accountId = :accountId " +
            "AND entityType = :entityType AND entityId = :entityId ORDER BY id"
    )
    suspend fun forEntity(
        accountId: Long,
        entityType: EntityType,
        entityId: Long
    ): List<PendingOperationEntity>

    /** Replaces the data of an operation that was never sent (merging repeated changes). */
    @Query("UPDATE pending_operation SET payload = :payload WHERE id = :id")
    suspend fun replacePayload(id: Long, payload: String)

    @Query("DELETE FROM pending_operation WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_operation WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    /** Points queued operations at the server id once a local entity was created remotely. */
    @Query(
        "UPDATE pending_operation SET entityId = :newId " +
            "WHERE accountId = :accountId AND entityType = :entityType AND entityId = :oldId"
    )
    suspend fun remapEntityId(accountId: Long, entityType: EntityType, oldId: Long, newId: Long)

    @Query("SELECT COUNT(*) FROM pending_operation WHERE accountId = :accountId")
    fun observeCount(accountId: Long): Flow<Int>
}
