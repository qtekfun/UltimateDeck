// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.queue

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.local.model.OperationType
import java.time.Clock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException

/**
 * Persistent queue of local changes waiting for the server (RF-08, SPEC §5). Changes are never
 * lost: an operation only leaves the queue once the server applied it or the user discards it.
 */
class OperationQueue @Inject constructor(
    database: UltimateDeckDatabase,
    private val clock: Clock,
    random: Random
) {
    private val dao = database.pendingOperationDao()
    private val retryDao = database.pendingOperationRetryDao()
    private val backoff = RetryBackoff(random)

    /**
     * Queues [operation] for [entityId]. A repeated change replaces the previous one of the same
     * type while that one was never sent, since each operation carries the final state.
     */
    suspend fun enqueue(accountId: Long, entityId: Long, operation: QueuedOperation) {
        val existing = dao.forEntity(accountId, operation.entityType, entityId)
        if (operation is QueuedOperation.DeleteCard) {
            enqueueDelete(accountId, entityId, operation, existing)
            return
        }
        val mergeable = existing.lastOrNull {
            it.type == operation.type && it.type != OperationType.CREATE && it.neverSent()
        }
        if (mergeable != null) {
            dao.replacePayload(mergeable.id, QueuedOperation.encode(operation))
        } else {
            insert(accountId, entityId, operation)
        }
    }

    /**
     * Deleting drops the changes of the entity that were never sent. If even its creation was
     * never sent, the server never knew it and nothing is queued.
     */
    private suspend fun enqueueDelete(
        accountId: Long,
        entityId: Long,
        operation: QueuedOperation.DeleteCard,
        existing: List<PendingOperationEntity>
    ) {
        dao.delete(existing.filter { it.neverSent() || it.failed }.map { it.id })
        val createdOnlyLocally = existing.any { it.type == OperationType.CREATE && it.neverSent() }
        if (!createdOnlyLocally) insert(accountId, entityId, operation)
    }

    private suspend fun insert(accountId: Long, entityId: Long, operation: QueuedOperation) {
        val now = clock.instant()
        dao.enqueue(
            PendingOperationEntity(
                accountId = accountId,
                type = operation.type,
                entityType = operation.entityType,
                entityId = entityId,
                payload = QueuedOperation.encode(operation),
                createdAt = now
            )
        )
    }

    /**
     * Runs the operations that are due, in the order they were queued. An entity whose operation
     * is waiting or failed keeps its later operations waiting too; other entities go on.
     */
    suspend fun process(accountId: Long, executor: OperationExecutor): ProcessResult {
        val blocked = mutableSetOf<Pair<EntityType, Long>>()
        var result = ProcessResult()
        while (true) {
            val now = clock.instant()
            val next = dao.all(accountId).firstOrNull { op ->
                val key = op.entityType to op.entityId
                val runnable = key !in blocked && !op.failed && !op.nextAttemptAt.isAfter(now)
                if (!runnable) blocked += key
                runnable
            } ?: break
            val outcome = run(next, executor)
            result = result.plus(outcome)
            when (outcome) {
                is ExecutionResult.Done -> {
                    dao.delete(next.id)
                    outcome.serverId?.let {
                        dao.remapEntityId(accountId, next.entityType, next.entityId, it)
                    }
                }

                is ExecutionResult.Retry -> {
                    retryDao.recordFailure(
                        next.id,
                        now.plus(backoff.delay(next.attempts)),
                        outcome.reason
                    )
                    blocked += next.entityType to next.entityId
                }

                is ExecutionResult.Failed -> {
                    retryDao.markFailed(next.id, outcome.reason)
                    blocked += next.entityType to next.entityId
                }
            }
        }
        return result
    }

    /** Unexpected errors count as temporary, so an operation is never dropped by accident. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun run(
        operation: PendingOperationEntity,
        executor: OperationExecutor
    ): ExecutionResult = try {
        executor.execute(operation.entityId, QueuedOperation.decode(operation.payload))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SerializationException) {
        ExecutionResult.Failed(UNREADABLE)
    } catch (error: Exception) {
        ExecutionResult.Retry(error.message)
    }

    /** Makes a failed operation run again on the next sync. */
    suspend fun retry(operationId: Long) = retryDao.resetForRetry(operationId, clock.instant())

    /** Drops a failed operation; the local change stays, it is just not sent. */
    suspend fun discard(operationId: Long) = dao.delete(operationId)

    fun observePendingCount(accountId: Long): Flow<Int> = dao.observeCount(accountId)

    fun observeFailed(accountId: Long): Flow<List<PendingOperationEntity>> =
        retryDao.observeFailed(accountId)

    private fun PendingOperationEntity.neverSent() = attempts == 0 && !failed

    private companion object {
        const val UNREADABLE = "Unreadable operation"
    }
}

/** How many operations a [OperationQueue.process] run sent, postponed and failed. */
data class ProcessResult(val done: Int = 0, val retried: Int = 0, val failed: Int = 0) {
    fun plus(outcome: ExecutionResult) = when (outcome) {
        is ExecutionResult.Done -> copy(done = done + 1)
        is ExecutionResult.Retry -> copy(retried = retried + 1)
        is ExecutionResult.Failed -> copy(failed = failed + 1)
    }
}
