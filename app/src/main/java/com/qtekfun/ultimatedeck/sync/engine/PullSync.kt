// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.DeckApi
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.dto.BoardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.sync.conflict.ConflictResolver
import javax.inject.Inject

/** How a pull ended. [Failed] keeps the server answer that stopped it. */
sealed interface PullResult {
    data object Done : PullResult

    data class Failed(val cause: ApiResult<*>) : PullResult
}

/**
 * Brings the server state into Room: boards, then the columns and cards of each active board.
 * Lists that did not change since the last pull answer 304 and are skipped (ETag). Cards
 * edited here go through [ConflictResolver]. Each board is written in one transaction, so a
 * pull cut off halfway leaves every board either as before or fully updated.
 */
class PullSync @Inject constructor(private val database: UltimateDeckDatabase) {
    private val accounts = database.accountDao()
    private val boards = database.boardDao()
    private val stacks = database.stackDao()
    private val cards = database.cardDao()
    private val labels = database.labelDao()
    private val users = database.userDao()
    private val merger = CardMerger(database)

    suspend fun pull(api: DeckApi, accountId: Long): PullResult {
        val failure = accounts.get(accountId)?.let { account ->
            pullBoardList(api, accountId, account.boardsEtag) ?: pullActiveBoards(api, accountId)
        }
        return failure?.let(PullResult::Failed) ?: PullResult.Done
    }

    private suspend fun pullBoardList(api: DeckApi, accountId: Long, etag: String?): ApiResult<*>? =
        when (val result = apiCall { api.boards.getBoards(etag = etag) }) {
            is ApiResult.Success -> {
                inTransaction {
                    saveBoards(accountId, result.value)
                    accounts.setBoardsEtag(accountId, result.etag)
                }
                null
            }

            ApiResult.NotModified -> null

            else -> result
        }

    private suspend fun pullActiveBoards(api: DeckApi, accountId: Long): ApiResult<*>? {
        val active = boards.all(accountId).filter { !it.archived && it.deletedAt == null }
        for (board in active) {
            pullBoard(api, accountId, board)?.let { return it }
        }
        return null
    }

    private suspend fun saveBoards(accountId: Long, list: List<BoardDto>) {
        val known = boards.all(accountId).associateBy { it.id }
        boards.upsert(
            list.map { it.toEntity(accountId).copy(stacksEtag = known[it.id]?.stacksEtag) }
        )
        users.upsert(
            list.flatMap { it.users + listOfNotNull(it.owner) }
                .distinctBy { it.uid }
                .map { it.toEntity(accountId) }
        )
        list.forEach { board ->
            labels.upsert(board.labels.map { it.toEntity(accountId, board.id) })
        }
        val onServer = list.map { it.id }.toSet()
        known.keys
            .filter { it !in onServer && !hasLocalChanges(cards.allForBoard(accountId, it)) }
            .forEach { boards.delete(accountId, it) }
    }

    /** Pulls one board; returns the failed answer, or null when it is up to date. */
    private suspend fun pullBoard(
        api: DeckApi,
        accountId: Long,
        board: BoardEntity
    ): ApiResult<*>? =
        when (val result = apiCall { api.boards.getStacks(board.id, etag = board.stacksEtag) }) {
            is ApiResult.Success -> saveColumns(api, accountId, board.id, result.value, result.etag)
            ApiResult.NotModified -> null
            else -> result
        }

    private suspend fun saveColumns(
        api: DeckApi,
        accountId: Long,
        boardId: Long,
        columns: List<StackDto>,
        etag: String?
    ): ApiResult<*>? {
        val listed = columns.flatMap { it.cards }.map { it.id }.toSet()
        val fetched = mutableListOf<CardDto>()
        // Columns only list open cards: an edited card missing from them may just be archived.
        for (card in cards.allForBoard(accountId, boardId)) {
            if (card.id < 0 || card.id in listed || card.dirtyFields == 0) continue
            when (val one = apiCall { api.cards.getCard(boardId, card.stackId, card.id) }) {
                is ApiResult.Success -> one.value.takeIf { it.deletedAt == 0L }?.let(fetched::add)
                ApiResult.NotFound -> Unit
                else -> return one
            }
        }
        inTransaction {
            saveBoard(accountId, boardId, columns, fetched)
            boards.get(accountId, boardId)?.let {
                boards.update(listOf(it.copy(stacksEtag = etag)))
            }
        }
        return null
    }

    private suspend fun saveBoard(
        accountId: Long,
        boardId: Long,
        columns: List<StackDto>,
        extraCards: List<CardDto>
    ) {
        stacks.upsert(columns.map { it.toEntity(accountId) })
        val serverCards = (columns.flatMap { it.cards } + extraCards).associateBy { it.id }
        users.upsert(
            serverCards.values.flatMap { it.assignedUsers.orEmpty() }
                .map { it.participant }
                .distinctBy { it.uid }
                .map { it.toEntity(accountId) }
        )
        val boardLabels = labels.idsForBoard(accountId, boardId).toSet()
        val local = cards.allForBoard(accountId, boardId).associateBy { it.id }
        serverCards.values.forEach { dto ->
            val card = local[dto.id]
            if (card == null) {
                merger.insert(accountId, boardId, dto, boardLabels)
            } else {
                merger.settle(accountId, boardId, card, dto, boardLabels)
            }
        }
        local.values
            .filter { it.id > 0 && it.id !in serverCards }
            .forEach { merger.settle(accountId, boardId, it, null, boardLabels) }
        removeMissingColumns(accountId, boardId, columns.map { it.id }.toSet())
    }

    /** Columns deleted on the server go, with their cards, unless a card has local changes. */
    private suspend fun removeMissingColumns(accountId: Long, boardId: Long, onServer: Set<Long>) {
        val byStack = cards.allForBoard(accountId, boardId).groupBy { it.stackId }
        stacks.forBoard(accountId, boardId)
            .filter { it.id !in onServer && !hasLocalChanges(byStack[it.id].orEmpty()) }
            .forEach { stacks.delete(accountId, it.id) }
    }

    private fun hasLocalChanges(list: List<CardEntity>) = list.any {
        it.id < 0 ||
            it.dirtyFields != 0
    }

    private suspend fun <R> inTransaction(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
