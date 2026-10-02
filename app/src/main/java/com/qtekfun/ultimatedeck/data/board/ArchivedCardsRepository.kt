// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.map
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import java.time.Instant
import javax.inject.Inject

/** An archived card as listed: [column] is the title of the column it belongs to. */
data class ArchivedCard(
    val id: Long,
    val boardId: Long,
    val stackId: Long,
    val title: String,
    val column: String,
    val archivedAt: Instant?
)

/**
 * Archived cards of a board (T15b). Sync only brings open cards, so this list is read from the
 * server when needed instead of being kept in Room.
 */
class ArchivedCardsRepository @Inject constructor(
    private val apis: AccountApiProvider,
    private val scheduler: SyncScheduler
) {
    /** Most recently archived first. */
    suspend fun load(boardId: Long): ApiResult<List<ArchivedCard>> {
        val api = apis.api() ?: return ApiResult.Unauthorized
        return apiCall { api.boards.getArchivedStacks(boardId) }.map { stacks ->
            stacks.flatMap { stack ->
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
            }.sortedByDescending { it.archivedAt }
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
