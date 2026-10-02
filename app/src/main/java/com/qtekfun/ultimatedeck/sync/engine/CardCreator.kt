// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.DeckApi
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CreateCardRequest
import com.qtekfun.ultimatedeck.data.remote.map
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import com.qtekfun.ultimatedeck.data.remote.mapper.toSnapshot
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation

/** Sends cards created offline, without creating them twice (T10). */
internal class CardCreator(
    private val api: DeckApi,
    database: UltimateDeckDatabase,
    private val accountId: Long
) {
    private val cards = database.cardDao()
    private val snapshots = database.cardSnapshotDao()

    /**
     * Creates the card with its current local values and moves the local row to the server id.
     * Creating is not idempotent: when an earlier attempt may have reached the server, a card
     * with the same title that this device does not know yet is taken as that one.
     */
    suspend fun create(
        localId: Long,
        operation: QueuedOperation.CreateCard,
        maybeSent: Boolean
    ): ExecutionResult {
        val local = cards.get(accountId, localId)
        val earlier = if (maybeSent) {
            findCreated(operation, setOfNotNull(local?.title, operation.title))
        } else {
            ApiResult.Success(null)
        }
        return when {
            earlier !is ApiResult.Success -> earlier.toExecutionResult()
            earlier.value != null -> adopt(localId, local, earlier.value)
            else -> post(localId, local, operation)
        }
    }

    private suspend fun findCreated(
        operation: QueuedOperation.CreateCard,
        titles: Set<String>
    ): ApiResult<CardDto?> = apiCall { api.boards.getStacks(operation.boardId) }.map { stacks ->
        stacks.firstOrNull { it.id == operation.stackId }?.cards?.firstOrNull {
            it.title in titles && cards.get(accountId, it.id) == null
        }
    }

    private suspend fun post(
        localId: Long,
        local: CardEntity?,
        operation: QueuedOperation.CreateCard
    ): ExecutionResult {
        val result = apiCall {
            api.cards.createCard(
                operation.boardId,
                operation.stackId,
                CreateCardRequest(
                    title = local?.title ?: operation.title,
                    order = operation.order,
                    description = local?.description?.takeIf { it.isNotEmpty() },
                    duedate = DeckDates.toIso(local?.dueDate)
                )
            )
        }
        return if (result is ApiResult.Success) {
            adopt(localId, local, result.value)
        } else {
            result.toExecutionResult()
        }
    }

    /** The local row (if still here) takes the server id; later operations follow it. */
    private suspend fun adopt(
        localId: Long,
        local: CardEntity?,
        created: CardDto
    ): ExecutionResult {
        if (local != null) {
            cards.updateId(accountId, localId, created.id)
            snapshots.put(created.toSnapshot(accountId))
        }
        return ExecutionResult.Done(serverId = created.id)
    }
}
