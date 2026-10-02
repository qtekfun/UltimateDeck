// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.qtekfun.ultimatedeck.data.local.entity.BoardMemberCrossRef
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import kotlinx.coroutines.flow.Flow

/** Who belongs to each board, for assigning cards (T16). */
@Dao
interface BoardMemberDao {
    @Query("DELETE FROM board_member WHERE accountId = :accountId AND boardId = :boardId")
    suspend fun clear(accountId: Long, boardId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(members: List<BoardMemberCrossRef>)

    /** Replaces the members of a board; their users must already be stored. */
    @Transaction
    suspend fun setMembers(accountId: Long, boardId: Long, uids: List<String>) {
        clear(accountId, boardId)
        insert(uids.map { BoardMemberCrossRef(accountId, boardId, it) })
    }

    @Query(
        "SELECT u.* FROM deck_user u JOIN board_member m " +
            "ON m.accountId = u.accountId AND m.uid = u.uid " +
            "WHERE m.accountId = :accountId AND m.boardId = :boardId " +
            "ORDER BY u.displayName COLLATE NOCASE"
    )
    fun observeMembers(accountId: Long, boardId: Long): Flow<List<DeckUserEntity>>
}
