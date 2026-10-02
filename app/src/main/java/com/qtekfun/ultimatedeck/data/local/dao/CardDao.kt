// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun update(cards: List<CardEntity>)

    /**
     * Inserts new rows and updates existing ones in place. Unlike REPLACE, existing rows are
     * never deleted, so their children are kept.
     */
    @Transaction
    suspend fun upsert(cards: List<CardEntity>) {
        val rowIds = insertIgnoringExisting(cards)
        update(cards.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    /** Cards of a board as shown on it: not archived, not deleted, in column order. */
    @Query(
        "SELECT * FROM card WHERE accountId = :accountId AND boardId = :boardId " +
            "AND archived = 0 AND deletedAt IS NULL ORDER BY stackId, `order`, id"
    )
    fun observeForBoard(accountId: Long, boardId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM card WHERE accountId = :accountId AND id = :id")
    suspend fun get(accountId: Long, id: Long): CardEntity?

    /** Deletes a card and, through foreign keys, its labels, assignees and attachments. */
    @Query("DELETE FROM card WHERE accountId = :accountId AND id = :id")
    suspend fun delete(accountId: Long, id: Long)

    /**
     * Replaces a local (negative) id with the server one. Labels, assignees, attachments and
     * the server snapshot follow through ON UPDATE CASCADE.
     */
    @Query("UPDATE card SET id = :newId WHERE accountId = :accountId AND id = :oldId")
    suspend fun updateId(accountId: Long, oldId: Long, newId: Long)

    /** Every card of a board, archived and deleted ones too, for sync. */
    @Query("SELECT * FROM card WHERE accountId = :accountId AND boardId = :boardId")
    suspend fun allForBoard(accountId: Long, boardId: Long): List<CardEntity>

    @Query("UPDATE card SET deletedOnServer = 1 WHERE accountId = :accountId AND id = :id")
    suspend fun markDeletedOnServer(accountId: Long, id: Long)
}
