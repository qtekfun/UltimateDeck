// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.map
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * An archived card as listed: [column] is the title of its column. [local] cards were archived
 * on this device and are restored offline too.
 */
data class ArchivedCard(
    val id: Long,
    val boardId: Long,
    val stackId: Long,
    val title: String,
    val column: String,
    val archivedAt: Instant?,
    val local: Boolean = false
)

/** The list to show, and the server failure that left it incomplete, if any. */
data class ArchivedCards(val cards: List<ArchivedCard>, val failure: ApiResult<*>? = null)

/**
 * Archived cards of a board (T15b). Sync only brings open cards, so the server list is read
 * when needed; cards archived here are added from Room, even before they are synced.
 */
class ArchivedCardsRepository @Inject constructor(
    private val apis: AccountApiProvider,
    private val scheduler: SyncScheduler,
    private val session: AccountSession,
    database: UltimateDeckDatabase
) {
    private val details = database.cardDetailDao()
    private val stacks = database.stackDao()

    /** Most recently archived first; offline, only the cards archived on this device. */
    suspend fun load(boardId: Long): ArchivedCards {
        val local = localCards(boardId)
        val api = apis.api() ?: return ArchivedCards(local, ApiResult.Unauthorized)
        val remote = apiCall { api.boards.getArchivedStacks(boardId) }.map { columns ->
            columns.flatMap { stack ->
                stack.cards.filter { it.deletedAt == 0L }.map { card ->
                    ArchivedCard(
                        id = card.id,
                        boardId = boardId,
                        stackId = stack.id,
                        title = card.title,
                        column = stack.title,
                        archivedAt = DeckDates.fromEpochSeconds(card.lastModified)
                    )
                }
            }
        }
        val server = (remote as? ApiResult.Success)?.value.orEmpty()
        val known = server.map { it.id }.toSet()
        val cards = (local.filter { it.id !in known } + server).sortedByDescending { it.archivedAt }
        return ArchivedCards(cards, remote.takeIf { it !is ApiResult.Success })
    }

    private suspend fun localCards(boardId: Long): List<ArchivedCard> {
        val accountId = session.activeAccount.first()?.id ?: return emptyList()
        val columns = stacks.forBoard(accountId, boardId).associate { it.id to it.title }
        return details.archivedForBoard(accountId, boardId).map { card ->
            ArchivedCard(
                id = card.id,
                boardId = boardId,
                stackId = card.stackId,
                title = card.title,
                column = columns[card.stackId].orEmpty(),
                archivedAt = card.localModifiedAt ?: card.lastModified,
                local = true
            )
        }
    }

    /** Restores a card on the server; a sync then brings it back to the board. */
    suspend fun unarchive(card: ArchivedCard): ApiResult<Unit> {
        val api = apis.api() ?: return ApiResult.Unauthorized
        val result = apiCall { api.cards.unarchiveCard(card.boardId, card.stackId, card.id) }
        if (result is ApiResult.Success) scheduler.requestSync()
        return result.map { }
    }
}
