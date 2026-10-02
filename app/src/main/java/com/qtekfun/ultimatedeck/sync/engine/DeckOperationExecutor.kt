// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.DeckApi
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.dto.CreateCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.LabelIdRequest
import com.qtekfun.ultimatedeck.data.remote.dto.ReorderCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UserIdRequest
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import com.qtekfun.ultimatedeck.data.remote.mapper.toSnapshot
import com.qtekfun.ultimatedeck.data.remote.mapper.toUpdateRequest
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult
import com.qtekfun.ultimatedeck.sync.queue.OperationExecutor
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import retrofit2.Response

/**
 * Sends queued operations of one account to Deck. Card values that may have changed since the
 * operation was queued (title, description...) are read from Room when it runs.
 */
class DeckOperationExecutor(
    private val api: DeckApi,
    database: UltimateDeckDatabase,
    private val accountId: Long,
    private val userId: String
) : OperationExecutor {
    private val cards = database.cardDao()
    private val snapshots = database.cardSnapshotDao()

    override suspend fun execute(entityId: Long, operation: QueuedOperation): ExecutionResult =
        when (operation) {
            is QueuedOperation.CreateCard -> create(entityId, operation)

            is QueuedOperation.UpdateCard -> update(entityId, operation)

            is QueuedOperation.MoveCard -> apiCall {
                api.cards.reorderCard(
                    operation.boardId,
                    operation.fromStackId,
                    entityId,
                    ReorderCardRequest(order = operation.order, stackId = operation.stackId)
                )
            }.toExecutionResult()

            is QueuedOperation.ArchiveCard -> apiCall {
                if (operation.archived) {
                    api.cards.archiveCard(operation.boardId, operation.stackId, entityId)
                } else {
                    api.cards.unarchiveCard(operation.boardId, operation.stackId, entityId)
                }
            }.toExecutionResult()

            is QueuedOperation.DeleteCard -> {
                val result = apiCall {
                    api.cards.deleteCard(operation.boardId, operation.stackId, entityId)
                }
                if (result ==
                    ApiResult.NotFound
                ) {
                    ExecutionResult.Done()
                } else {
                    result.toExecutionResult()
                }
            }

            is QueuedOperation.SetLabels -> setLabels(entityId, operation)

            is QueuedOperation.SetAssignees -> setAssignees(entityId, operation)

            is QueuedOperation.UploadAttachment -> ExecutionResult.Failed(
                "attachments are not supported yet"
            )
        }

    /** Creates the card with its current local values and moves the local row to the server id. */
    private suspend fun create(
        localId: Long,
        operation: QueuedOperation.CreateCard
    ): ExecutionResult {
        val local = cards.get(accountId, localId)
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
        if (result !is ApiResult.Success) return result.toExecutionResult()
        val created = result.value
        if (local != null) {
            cards.updateId(accountId, localId, created.id)
            snapshots.put(created.toSnapshot(accountId))
        }
        return ExecutionResult.Done(serverId = created.id)
    }

    /** Sends the whole editable state, as Deck expects; a card deleted here needs nothing. */
    private suspend fun update(
        cardId: Long,
        operation: QueuedOperation.UpdateCard
    ): ExecutionResult {
        val card = cards.get(accountId, cardId) ?: return ExecutionResult.Done()
        return apiCall {
            api.cards.updateCard(
                operation.boardId,
                card.stackId,
                cardId,
                card.toUpdateRequest(userId)
            )
        }.toExecutionResult()
    }

    private suspend fun setLabels(
        cardId: Long,
        operation: QueuedOperation.SetLabels
    ): ExecutionResult {
        val path = CardPath(operation.boardId, operation.stackId, cardId)
        val labels = Members<Long>(
            read = { it.labelIds },
            write = { snapshot, ids -> snapshot.copy(labelIds = ids) },
            add = { id ->
                api.cardMetadata.assignLabel(path.board, path.stack, path.card, LabelIdRequest(id))
            },
            remove = { id ->
                api.cardMetadata.removeLabel(path.board, path.stack, path.card, LabelIdRequest(id))
            }
        )
        return syncMembers(cardId, operation.labelIds.toSet(), labels)
    }

    private suspend fun setAssignees(
        cardId: Long,
        operation: QueuedOperation.SetAssignees
    ): ExecutionResult {
        val path = CardPath(operation.boardId, operation.stackId, cardId)
        val assignees = Members<String>(
            read = { it.assigneeUids },
            write = { snapshot, uids -> snapshot.copy(assigneeUids = uids) },
            add = { uid ->
                api.cardMetadata.assignUser(path.board, path.stack, path.card, UserIdRequest(uid))
            },
            remove = { uid ->
                api.cardMetadata.unassignUser(path.board, path.stack, path.card, UserIdRequest(uid))
            }
        )
        return syncMembers(cardId, operation.uids.toSet(), assignees)
    }

    /**
     * Deck only adds or removes one label or user at a time, and refuses to add one twice. So
     * the difference is taken against the last known server state, and that state is updated
     * after each call: a retry after a partial failure only sends what is still missing.
     */
    private suspend fun <T> syncMembers(
        cardId: Long,
        target: Set<T>,
        members: Members<T>
    ): ExecutionResult {
        val snapshot = snapshots.get(accountId, cardId)
        var onServer = snapshot?.let(members.read).orEmpty().toSet()
        val steps = (target - onServer).map { it to true } + (onServer - target).map { it to false }
        for ((member, adding) in steps) {
            val result = apiCall { if (adding) members.add(member) else members.remove(member) }
            if (result !is ApiResult.Success) return result.toExecutionResult()
            onServer = if (adding) onServer + member else onServer - member
            snapshot?.let { snapshots.put(members.write(it, onServer.toList())) }
        }
        return ExecutionResult.Done()
    }

    /** How to read, store and change one kind of card member (labels or assignees). */
    private class Members<T>(
        val read: (CardServerSnapshotEntity) -> List<T>,
        val write: (CardServerSnapshotEntity, List<T>) -> CardServerSnapshotEntity,
        val add: suspend (T) -> Response<Unit>,
        val remove: suspend (T) -> Response<Unit>
    )

    private data class CardPath(val board: Long, val stack: Long, val card: Long)
}
