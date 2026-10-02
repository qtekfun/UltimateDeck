// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatedeck.data.local.entity.CardAssigneeCrossRef
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.model.CardAssigneeRow
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(users: List<DeckUserEntity>): List<Long>

    @Update
    suspend fun update(users: List<DeckUserEntity>)

    /** Inserts new users and updates existing ones in place, keeping their assignments. */
    @Transaction
    suspend fun upsert(users: List<DeckUserEntity>) {
        val rowIds = insertIgnoringExisting(users)
        update(users.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    @Query("DELETE FROM card_assignee WHERE accountId = :accountId AND cardId = :cardId")
    suspend fun clearAssignees(accountId: Long, cardId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignees(links: List<CardAssigneeCrossRef>)

    /** Replaces the users assigned to a card. */
    @Transaction
    suspend fun setAssignees(accountId: Long, cardId: Long, uids: List<String>) {
        clearAssignees(accountId, cardId)
        insertAssignees(uids.map { CardAssigneeCrossRef(accountId, cardId, it) })
    }

    /** Users assigned to every card of a board, for the board view. */
    @Query(
        "SELECT ca.cardId AS cardId, u.uid AS uid, u.displayName AS displayName " +
            "FROM card_assignee ca " +
            "JOIN deck_user u ON u.accountId = ca.accountId AND u.uid = ca.uid " +
            "JOIN card c ON c.accountId = ca.accountId AND c.id = ca.cardId " +
            "WHERE ca.accountId = :accountId AND c.boardId = :boardId ORDER BY ca.cardId, u.displayName"
    )
    fun observeCardAssignees(accountId: Long, boardId: Long): Flow<List<CardAssigneeRow>>

    @Query("SELECT uid FROM card_assignee WHERE accountId = :accountId AND cardId = :cardId")
    suspend fun assigneeUidsOfCard(accountId: Long, cardId: Long): List<String>
}
