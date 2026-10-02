// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity

/** Last known server state of cards (SPEC §5). */
@Dao
interface CardSnapshotDao {
    /** Snapshots have no children, so replacing the row is safe. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(snapshot: CardServerSnapshotEntity)

    @Query("SELECT * FROM card_server_snapshot WHERE accountId = :accountId AND cardId = :cardId")
    suspend fun get(accountId: Long, cardId: Long): CardServerSnapshotEntity?
}
