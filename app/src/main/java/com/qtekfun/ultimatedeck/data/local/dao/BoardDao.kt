// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(boards: List<BoardEntity>): List<Long>

    @Update
    suspend fun update(boards: List<BoardEntity>)

    /**
     * Inserts new rows and updates existing ones in place. Unlike REPLACE, existing rows are
     * never deleted, so their children are kept.
     */
    @Transaction
    suspend fun upsert(boards: List<BoardEntity>) {
        val rowIds = insertIgnoringExisting(boards)
        update(boards.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    /** Boards shown in the list: not archived and not deleted (RF-02). */
    @Query(
        "SELECT * FROM board WHERE accountId = :accountId AND archived = 0 AND deletedAt IS NULL " +
            "ORDER BY title COLLATE NOCASE"
    )
    fun observeActive(accountId: Long): Flow<List<BoardEntity>>

    @Query("SELECT * FROM board WHERE accountId = :accountId AND id = :id")
    suspend fun get(accountId: Long, id: Long): BoardEntity?

    @Query("UPDATE board SET id = :newId WHERE accountId = :accountId AND id = :oldId")
    suspend fun updateBoardRowId(accountId: Long, oldId: Long, newId: Long)

    @Query("UPDATE card SET boardId = :newId WHERE accountId = :accountId AND boardId = :oldId")
    suspend fun updateCardsBoardId(accountId: Long, oldId: Long, newId: Long)

    /**
     * Replaces a local (negative) board id with the server one. Columns and labels follow
     * through ON UPDATE CASCADE; cards only reference the board for queries, so they are
     * updated here.
     */
    @Transaction
    suspend fun updateId(accountId: Long, oldId: Long, newId: Long) {
        updateBoardRowId(accountId, oldId, newId)
        updateCardsBoardId(accountId, oldId, newId)
    }

    @Query("SELECT * FROM board WHERE accountId = :accountId")
    suspend fun all(accountId: Long): List<BoardEntity>

    /** Deletes a board with its columns and cards, through foreign keys. */
    @Query("DELETE FROM board WHERE accountId = :accountId AND id = :id")
    suspend fun delete(accountId: Long, id: Long)
}
