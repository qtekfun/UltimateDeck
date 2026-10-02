// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.qtekfun.ultimatedeck.data.local.entity.LocalIdSequenceEntity
import com.qtekfun.ultimatedeck.data.local.model.EntityType

/** Hands out negative ids (-1, -2...) for entities created offline. */
@Dao
interface LocalIdDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun start(sequence: LocalIdSequenceEntity)

    @Query(
        "SELECT next FROM local_id_sequence WHERE accountId = :accountId AND entityType = :entityType"
    )
    suspend fun current(accountId: Long, entityType: EntityType): Long

    @Query(
        "UPDATE local_id_sequence SET next = next - 1 WHERE accountId = :accountId AND entityType = :entityType"
    )
    suspend fun advance(accountId: Long, entityType: EntityType)

    /** Returns a new negative id, unique for the account and entity type. */
    @Transaction
    suspend fun nextId(accountId: Long, entityType: EntityType): Long {
        start(LocalIdSequenceEntity(accountId, entityType, next = -1))
        val id = current(accountId, entityType)
        advance(accountId, entityType)
        return id
    }
}
