// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatedeck.data.local.entity.CardLabelCrossRef
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.model.CardLabelRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(labels: List<LabelEntity>): List<Long>

    @Update
    suspend fun update(labels: List<LabelEntity>)

    /** Inserts new labels and updates existing ones in place, keeping their card links. */
    @Transaction
    suspend fun upsert(labels: List<LabelEntity>) {
        val rowIds = insertIgnoringExisting(labels)
        update(labels.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    @Query("SELECT * FROM label WHERE accountId = :accountId AND boardId = :boardId ORDER BY title")
    fun observeForBoard(accountId: Long, boardId: Long): Flow<List<LabelEntity>>

    @Query("DELETE FROM card_label WHERE accountId = :accountId AND cardId = :cardId")
    suspend fun clearCardLabels(accountId: Long, cardId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCardLabels(links: List<CardLabelCrossRef>)

    /** Replaces the labels of a card. */
    @Transaction
    suspend fun setCardLabels(accountId: Long, cardId: Long, labelIds: List<Long>) {
        clearCardLabels(accountId, cardId)
        insertCardLabels(labelIds.map { CardLabelCrossRef(accountId, cardId, it) })
    }

    /** Labels of every card of a board, for the board view. */
    @Query(
        "SELECT cl.cardId AS cardId, l.id AS labelId, l.title AS title, l.color AS color " +
            "FROM card_label cl JOIN label l ON l.accountId = cl.accountId AND l.id = cl.labelId " +
            "WHERE cl.accountId = :accountId AND l.boardId = :boardId ORDER BY cl.cardId, l.title"
    )
    fun observeCardLabels(accountId: Long, boardId: Long): Flow<List<CardLabelRow>>
}
