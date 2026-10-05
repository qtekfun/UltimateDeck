// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.model.DueCardRow
import kotlinx.coroutines.flow.Flow

/** Cards that may need a due date reminder (RF-10). */
@Dao
interface ReminderDao {
    /** Open cards with a due date: not archived, deleted or done; [uid] tells which are mine. */
    @Query(DUE_CARDS)
    fun observeDueCards(accountId: Long, uid: String): Flow<List<DueCardRow>>

    /** The same, once. */
    @Query(DUE_CARDS)
    suspend fun dueCards(accountId: Long, uid: String): List<DueCardRow>
}

private const val DUE_CARDS =
    "SELECT c.accountId AS accountId, c.id AS cardId, c.boardId AS boardId, " +
        "b.title AS boardTitle, c.title AS title, " +
        "c.dueDate AS dueDate, EXISTS(SELECT 1 FROM card_assignee a " +
        "WHERE a.accountId = c.accountId AND a.cardId = c.id AND a.uid = :uid) " +
        "AS assignedToMe " +
        "FROM card c JOIN board b ON b.accountId = c.accountId AND b.id = c.boardId " +
        "WHERE c.accountId = :accountId AND c.dueDate IS NOT NULL AND c.archived = 0 " +
        "AND c.deletedAt IS NULL AND c.done IS NULL AND c.deletedOnServer = 0 " +
        "ORDER BY c.dueDate"
