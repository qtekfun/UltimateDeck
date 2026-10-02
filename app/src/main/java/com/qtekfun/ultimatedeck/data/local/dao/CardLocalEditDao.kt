// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.model.CardField
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Local edits of cards. Each one changes the value and sets the bit of its [CardField] in
 * `dirtyFields`, so the edit is applied at once and synced later (RF-04, RF-08).
 */
@Dao
interface CardLocalEditDao {
    @Query(
        "UPDATE card SET title = :title, dirtyFields = dirtyFields | :dirtyBit " +
            "WHERE accountId = :accountId AND id = :id"
    )
    suspend fun updateTitle(
        accountId: Long,
        id: Long,
        title: String,
        dirtyBit: Int = CardField.TITLE.bit
    )

    @Query(
        "UPDATE card SET description = :description, dirtyFields = dirtyFields | :dirtyBit " +
            "WHERE accountId = :accountId AND id = :id"
    )
    suspend fun updateDescription(
        accountId: Long,
        id: Long,
        description: String,
        dirtyBit: Int = CardField.DESCRIPTION.bit
    )

    @Query(
        "UPDATE card SET dueDate = :dueDate, dirtyFields = dirtyFields | :dirtyBit " +
            "WHERE accountId = :accountId AND id = :id"
    )
    suspend fun updateDueDate(
        accountId: Long,
        id: Long,
        dueDate: Instant?,
        dirtyBit: Int = CardField.DUE_DATE.bit
    )

    @Query(
        "UPDATE card SET stackId = :stackId, `order` = :order, " +
            "dirtyFields = dirtyFields | :dirtyBit WHERE accountId = :accountId AND id = :id"
    )
    suspend fun updatePosition(
        accountId: Long,
        id: Long,
        stackId: Long,
        order: Int,
        dirtyBit: Int = CardField.POSITION.bit
    )

    @Query(
        "UPDATE card SET archived = :archived, dirtyFields = dirtyFields | :dirtyBit " +
            "WHERE accountId = :accountId AND id = :id"
    )
    suspend fun updateArchived(
        accountId: Long,
        id: Long,
        archived: Boolean,
        dirtyBit: Int = CardField.ARCHIVED.bit
    )

    /** Marks fields changed outside this DAO, such as labels and assignees. */
    @Query(
        "UPDATE card SET dirtyFields = dirtyFields | :mask WHERE accountId = :accountId AND id = :id"
    )
    suspend fun markDirty(accountId: Long, id: Long, mask: Int)

    /** Clears the bits in [mask] once those fields are synced. */
    @Query(
        "UPDATE card SET dirtyFields = dirtyFields & ~:mask WHERE accountId = :accountId AND id = :id"
    )
    suspend fun clearDirty(accountId: Long, id: Long, mask: Int)

    /** Cards of a board with unsynced changes, for the "pending sync" marker (RF-08). */
    @Query(
        "SELECT id FROM card WHERE accountId = :accountId AND boardId = :boardId AND dirtyFields != 0"
    )
    fun observeDirtyCardIds(accountId: Long, boardId: Long): Flow<List<Long>>
}
