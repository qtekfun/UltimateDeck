// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import kotlinx.coroutines.flow.Flow

/** One card and its last known server state, for the card detail (T14). */
@Dao
interface CardDetailDao {
    @Query("SELECT * FROM card WHERE accountId = :accountId AND id = :id")
    fun observeCard(accountId: Long, id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM card_server_snapshot WHERE accountId = :accountId AND cardId = :id")
    fun observeSnapshot(accountId: Long, id: Long): Flow<CardServerSnapshotEntity?>
}
