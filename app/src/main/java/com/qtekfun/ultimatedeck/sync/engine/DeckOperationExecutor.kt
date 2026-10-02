// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.DeckApi
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.LabelIdRequest
import com.qtekfun.ultimatedeck.data.remote.dto.ReorderCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UserIdRequest
import com.qtekfun.ultimatedeck.data.remote.map
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
    private val creator = CardCreator(api, database, accountId)
    private val uploader = AttachmentUploader(api, database, accountId)

    override suspend fun execute(
        entityId: Long,
        operation: QueuedOperation,
        maybeSent: Boolean
    ): ExecutionResult = when (operation) {
        is QueuedOperation.CreateCard -> creator.create(entityId, operation, maybeSent)

        is QueuedOperation.UpdateCard -> update(entityId, operation)

        is QueuedOperation.MoveCard -> apiCall {
            api.cards.reorderCard(
                operation.boardId,
                // Deck takes the stack from the path, not the body: it must be the target.
                operation.stackId,
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

        is QueuedOperation.UploadAttachment -> uploader.upload(entityId)
    }

    /**
     * Sends the whole editable state, as Deck expects; a card deleted here needs nothing. The
     * server version is read first: a title or description changed there too is not overwritten
     * (SPEC §5) but marked as a conflict, and its server value is sent back unchanged.
     */
    private suspend fun update(
        cardId: Long,
        operation: QueuedOperation.UpdateCard
    ): ExecutionResult {
        val card = cards.get(accountId, cardId) ?: return ExecutionResult.Done()
        val current = apiCall { api.cards.getCard(operation.boardId, card.stackId, cardId) }
        return if (current is ApiResult.Success) {
            send(card, current.value, operation.boardId)
        } else {
            current.toExecutionResult()
        }
    }

    private suspend fun send(card: CardEntity, server: CardDto, boardId: Long): ExecutionResult {
        val cardId = card.id
        val conflicts = card.conflictFields or newConflicts(card, server)
        if (conflicts != card.conflictFields) {
            cards.update(listOf(card.copy(conflictFields = conflicts)))
            val known = snapshots.get(accountId, cardId)
                ?: server.toSnapshot(accountId).copy(cardId = cardId)
            snapshots.put(
                known.copy(title = server.title, description = server.description.orEmpty())
            )
        }
        // Only fields changed here (and not in conflict) take the local value; the rest keep
        // what the server has now, so changes made by others meanwhile are not undone.
        val mine = CardField.fromMask(card.dirtyFields) - CardField.fromMask(conflicts)
        val sent = card.copy(
            title = pick(CardField.TITLE in mine, card.title, server.title),
            description = pick(
                CardField.DESCRIPTION in mine,
                card.description,
                server.description.orEmpty()
            ),
            dueDate = pick(
                CardField.DUE_DATE in mine,
                card.dueDate,
                DeckDates.fromIso(server.duedate)
            ),
            done = pick(CardField.DONE in mine, card.done, DeckDates.fromIso(server.done)),
            order = pick(CardField.POSITION in mine, card.order, server.order),
            archived = pick(CardField.ARCHIVED in mine, card.archived, server.archived)
        )
        val result = apiCall {
            api.cards.updateCard(
                boardId,
                card.stackId,
                cardId,
                sent.toUpdateRequest(userId)
            )
        }
        // The server now has what was sent: later changes are measured from it, so text typed
        // while a sync runs is not taken for someone else's change.
        if (result is ApiResult.Success) {
            snapshots.get(accountId, cardId)?.let { known ->
                snapshots.put(
                    known.copy(
                        title = sent.title,
                        description = sent.description,
                        dueDate = sent.dueDate,
                        archived = sent.archived,
                        done = sent.done
                    )
                )
            }
        }
        return result.toExecutionResult()
    }

    /** Text fields edited here that the server also changed, to something else, since the last sync. */
    private suspend fun newConflicts(card: CardEntity, server: CardDto): Int {
        val known = snapshots.get(accountId, card.id)
        val dirty = CardField.fromMask(card.dirtyFields)
        val title =
            CardField.TITLE in dirty && server.title != known?.title && server.title != card.title
        val serverDescription = server.description.orEmpty()
        val description = CardField.DESCRIPTION in dirty &&
            serverDescription != known?.description && serverDescription != card.description
        return (if (title) CardField.TITLE.bit else 0) or
            (if (description) CardField.DESCRIPTION.bit else 0)
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

private fun <T> pick(useMine: Boolean, mine: T, theirs: T): T = if (useMine) mine else theirs
