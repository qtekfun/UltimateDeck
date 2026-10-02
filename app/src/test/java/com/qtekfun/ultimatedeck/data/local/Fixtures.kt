// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity

/** Small builders for test rows. */
object Fixtures {
    suspend fun account(db: UltimateDeckDatabase, user: String = "ana"): Long =
        db.accountDao().insert(
            AccountEntity(serverUrl = "https://cloud.example", userId = user, displayName = user)
        )

    fun board(accountId: Long, id: Long = 1, title: String = "Board $id") =
        BoardEntity(accountId = accountId, id = id, title = title, color = "0082c9")

    fun stack(accountId: Long, id: Long = 10, boardId: Long = 1, order: Int = 0) = StackEntity(
        accountId = accountId,
        id = id,
        boardId = boardId,
        title = "Stack $id",
        order = order
    )

    fun card(
        accountId: Long,
        id: Long = 100,
        stackId: Long = 10,
        boardId: Long = 1,
        order: Int = 0
    ) = CardEntity(
        accountId = accountId,
        id = id,
        boardId = boardId,
        stackId = stackId,
        title = "Card $id",
        order = order
    )

    /** Inserts an account with one board, one stack and the given cards; returns the account id. */
    suspend fun boardWithCards(db: UltimateDeckDatabase, vararg cardIds: Long): Long {
        val accountId = account(db)
        db.boardDao().upsert(listOf(board(accountId)))
        db.stackDao().upsert(listOf(stack(accountId)))
        db.cardDao().upsert(cardIds.mapIndexed { index, id -> card(accountId, id, order = index) })
        return accountId
    }
}
