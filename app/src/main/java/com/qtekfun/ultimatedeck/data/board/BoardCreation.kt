// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.dto.CreateBoardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.CreateStackRequest
import com.qtekfun.ultimatedeck.data.remote.map
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.domain.board.BoardItem
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Creating boards and columns (T15c). Unlike cards, this needs a connection: offline it would
 * leave queued operations pointing at temporary ids of the new board or column, to be
 * rewritten later. It is rare enough that asking for a connection is the safer choice.
 */
class BoardCreation @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val apis: AccountApiProvider
) {
    private val boards = database.boardDao()
    private val stacks = database.stackDao()
    private val users = database.userDao()
    private val members = database.boardMemberDao()

    /** Creates a board on the server and stores it; the result is the new board. */
    suspend fun createBoard(title: String, color: String): ApiResult<BoardItem> {
        val name = title.trim()
        val account = session.activeAccount.first()
        val api = apis.api()
        if (account == null || api == null || name.isEmpty()) return ApiResult.Unauthorized
        return apiCall { api.boards.createBoard(CreateBoardRequest(name, color)) }.map { board ->
            boards.upsert(listOf(board.toEntity(account.id)))
            val owner = board.owner
            if (owner != null) {
                users.upsert(listOf(owner.toEntity(account.id)))
                members.setMembers(account.id, board.id, listOf(owner.uid))
            }
            BoardItem(board.id, board.title, board.color)
        }
    }

    /** Adds a column at the end of a board, on the server and here. */
    suspend fun createColumn(boardId: Long, title: String): ApiResult<Unit> {
        val name = title.trim()
        val accountId = session.activeAccount.first()?.id
        val api = apis.api()
        if (accountId == null || api == null || name.isEmpty()) return ApiResult.Unauthorized
        val order = (stacks.forBoard(accountId, boardId).maxOfOrNull { it.order } ?: -1) + 1
        return apiCall {
            api.boards.createStack(boardId, CreateStackRequest(name, order))
        }.map { stack ->
            stacks.upsert(listOf(stack.toEntity(accountId)))
        }
    }
}
