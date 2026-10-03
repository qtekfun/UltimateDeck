// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.map
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Deleting boards and columns (T15d), only offered when turned on in Settings. Like creating
 * them, it needs a connection; what is deleted on the server goes from here with its cards.
 */
class BoardDeletion @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val apis: AccountApiProvider
) {
    private val boards = database.boardDao()
    private val stacks = database.stackDao()

    suspend fun deleteBoard(boardId: Long): ApiResult<Unit> {
        val accountId = session.activeAccount.first()?.id
        val api = apis.api()
        if (accountId == null || api == null) return ApiResult.Unauthorized
        return apiCall { api.boards.deleteBoard(boardId) }.map { boards.delete(accountId, boardId) }
    }

    suspend fun deleteColumn(boardId: Long, columnId: Long): ApiResult<Unit> {
        val accountId = session.activeAccount.first()?.id
        val api = apis.api()
        if (accountId == null || api == null) return ApiResult.Unauthorized
        return apiCall {
            api.boards.deleteStack(boardId, columnId)
        }.map { stacks.delete(accountId, columnId) }
    }
}
