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
// Records when the card was edited and which field changed.
private const val EDITED = "localModifiedAt = :modifiedAt, dirtyFields = dirtyFields | :dirtyBit"
private const val WHERE_CARD = " WHERE accountId = :accountId AND id = :id"

@Dao
interface CardLocalEditDao {
    @Query(
        "UPDATE card SET title = :title, " + EDITED + WHERE_CARD
    )
    suspend fun updateTitle(
        accountId: Long,
        id: Long,
        title: String,
        modifiedAt: Instant,
        dirtyBit: Int = CardField.TITLE.bit
    )

    @Query(
        "UPDATE card SET description = :description, " + EDITED + WHERE_CARD
    )
    suspend fun updateDescription(
        accountId: Long,
        id: Long,
        description: String,
        modifiedAt: Instant,
        dirtyBit: Int = CardField.DESCRIPTION.bit
    )

    @Query(
        "UPDATE card SET dueDate = :dueDate, " + EDITED + WHERE_CARD
    )
    suspend fun updateDueDate(
        accountId: Long,
        id: Long,
        dueDate: Instant?,
        modifiedAt: Instant,
        dirtyBit: Int = CardField.DUE_DATE.bit
    )

    @Query(
        "UPDATE card SET stackId = :stackId, `order` = :order, " + EDITED + WHERE_CARD
    )
    suspend fun updatePosition(
        accountId: Long,
        id: Long,
        stackId: Long,
        order: Int,
        modifiedAt: Instant,
        dirtyBit: Int = CardField.POSITION.bit
    )

    @Query(
        "UPDATE card SET archived = :archived, " + EDITED + WHERE_CARD
    )
    suspend fun updateArchived(
        accountId: Long,
        id: Long,
        archived: Boolean,
        modifiedAt: Instant,
        dirtyBit: Int = CardField.ARCHIVED.bit
    )

    /** Marks fields changed outside this DAO, such as labels and assignees. */
    @Query(
        "UPDATE card SET localModifiedAt = :modifiedAt, dirtyFields = dirtyFields | :mask" +
            WHERE_CARD
    )
    suspend fun markDirty(accountId: Long, id: Long, mask: Int, modifiedAt: Instant)

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

    /** Hides a card deleted here until the server confirms the deletion. */
    @Query("UPDATE card SET deletedAt = :at WHERE accountId = :accountId AND id = :id")
    suspend fun markDeleted(accountId: Long, id: Long, at: Instant)
}
