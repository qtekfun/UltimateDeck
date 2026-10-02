// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity

const val ACCOUNT = 1L
const val BOARD = 1L
const val STACK = 10L

/** An account with one board and one column, the minimum a card needs. */
suspend fun UltimateDeckDatabase.seedBoard() {
    accountDao().insert(
        AccountEntity(
            id = ACCOUNT,
            serverUrl = "https://c.example/",
            userId = "ana",
            displayName = "Ana"
        )
    )
    boardDao().upsert(
        listOf(BoardEntity(accountId = ACCOUNT, id = BOARD, title = "Board", color = "fff"))
    )
    stackDao().upsert(
        listOf(
            StackEntity(
                accountId = ACCOUNT,
                id = STACK,
                boardId = BOARD,
                title = "To do",
                order = 0
            )
        )
    )
}

fun card(id: Long, title: String = "Card", stackId: Long = STACK, dirty: Int = 0) = CardEntity(
    accountId = ACCOUNT,
    id = id,
    boardId = BOARD,
    stackId = stackId,
    title = title,
    dirtyFields = dirty
)

fun snapshot(
    cardId: Long,
    title: String = "Card",
    labelIds: List<Long> = emptyList(),
    uids: List<String> = emptyList()
) = CardServerSnapshotEntity(
    accountId = ACCOUNT,
    cardId = cardId,
    title = title,
    description = "",
    stackId = STACK,
    order = 0,
    archived = false,
    dueDate = null,
    done = null,
    labelIds = labelIds,
    assigneeUids = uids,
    lastModified = null,
    etag = null
)
