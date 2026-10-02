// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(stacks: List<StackEntity>): List<Long>

    @Update
    suspend fun update(stacks: List<StackEntity>)

    /**
     * Inserts new rows and updates existing ones in place. Unlike REPLACE, existing rows are
     * never deleted, so their children are kept.
     */
    @Transaction
    suspend fun upsert(stacks: List<StackEntity>) {
        val rowIds = insertIgnoringExisting(stacks)
        update(stacks.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    @Query(
        "SELECT * FROM stack WHERE accountId = :accountId AND boardId = :boardId " +
            "AND deletedAt IS NULL ORDER BY `order`, id"
    )
    fun observeForBoard(accountId: Long, boardId: Long): Flow<List<StackEntity>>

    @Query("SELECT * FROM stack WHERE accountId = :accountId AND boardId = :boardId")
    suspend fun forBoard(accountId: Long, boardId: Long): List<StackEntity>

    /** Deletes a column with its cards, through foreign keys. */
    @Query("DELETE FROM stack WHERE accountId = :accountId AND id = :id")
    suspend fun delete(accountId: Long, id: Long)
}
