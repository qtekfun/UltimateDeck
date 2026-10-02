// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.queue

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.local.Fixtures
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.local.model.OperationType
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation.CreateCard
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation.DeleteCard
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation.MoveCard
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation.SetLabels
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation.UpdateCard
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OperationQueueTest {
    private val db = inMemoryDatabase()
    private val dao = db.pendingOperationDao()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val executor = ScriptedExecutor()

    @AfterEach
    fun close() = db.close()

    private suspend fun account() = Fixtures.account(db)

    private suspend fun queued(accountId: Long): List<PendingOperationEntity> = dao.all(accountId)

    private suspend fun operations(accountId: Long) = queued(accountId).map {
        QueuedOperation.decode(it.payload)
    }

    private fun move(order: Int) =
        MoveCard(boardId = 1, fromStackId = 10, stackId = 11, order = order)

    @Nested
    inner class Enqueue {
        @Test
        fun `stores the operation with its type, entity and time`() = runTest {
            val accountId = account()

            queue.enqueue(accountId, 100, move(3))

            val stored = queued(accountId).single()
            assertEquals(OperationType.MOVE to EntityType.CARD, stored.type to stored.entityType)
            assertEquals(100L, stored.entityId)
            assertEquals(clock.now, stored.createdAt)
            assertEquals(listOf(move(3)), operations(accountId))
        }

        @Test
        fun `merges repeated changes of the same kind into the last state`() = runTest {
            val accountId = account()

            queue.enqueue(accountId, 100, move(1))
            queue.enqueue(accountId, 100, move(2))
            queue.enqueue(accountId, 100, move(5))

            assertEquals(listOf(move(5)), operations(accountId))
        }

        @Test
        fun `keeps different kinds and different cards apart`() = runTest {
            val accountId = account()

            queue.enqueue(accountId, 100, move(1))
            queue.enqueue(accountId, 100, SetLabels(1, 11, listOf(7)))
            queue.enqueue(accountId, 101, move(2))

            assertEquals(3, queued(accountId).size)
        }

        @Test
        fun `does not merge into an operation that was already sent`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, move(1))
            executor.on(100, { ExecutionResult.Retry("timeout") })
            queue.process(accountId, executor)

            queue.enqueue(accountId, 100, move(2))

            assertEquals(listOf(move(1), move(2)), operations(accountId))
        }

        @Test
        fun `never merges creations`() = runTest {
            val accountId = account()

            queue.enqueue(accountId, -1, CreateCard(1, 10, "A", 0))
            queue.enqueue(accountId, -1, CreateCard(1, 10, "A", 0))

            assertEquals(2, queued(accountId).size)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `a card created and deleted offline sends nothing`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, -1, CreateCard(1, 10, "Draft", 0))
            queue.enqueue(accountId, -1, move(2))

            queue.enqueue(accountId, -1, DeleteCard(1, 11))

            assertEquals(emptyList<PendingOperationEntity>(), queued(accountId))
        }

        @Test
        fun `deleting a server card drops its unsent changes and sends only the delete`() =
            runTest {
                val accountId = account()
                queue.enqueue(accountId, 100, move(2))
                queue.enqueue(accountId, 100, UpdateCard(1, 11))

                queue.enqueue(accountId, 100, DeleteCard(1, 11))

                assertEquals(listOf(DeleteCard(1, 11)), operations(accountId))
            }

        @Test
        fun `keeps changes already in flight and drops failed ones`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, move(1))
            queue.enqueue(accountId, 101, UpdateCard(1, 10))
            executor.on(100, { ExecutionResult.Retry(null) })
            executor.on(101, { ExecutionResult.Failed("400") })
            queue.process(accountId, executor)

            queue.enqueue(accountId, 100, DeleteCard(1, 11))
            queue.enqueue(accountId, 101, DeleteCard(1, 10))

            assertEquals(
                listOf(move(1), DeleteCard(1, 11), DeleteCard(1, 10)),
                operations(accountId)
            )
        }

        @Test
        fun `a creation that may have reached the server is deleted remotely too`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, -1, CreateCard(1, 10, "Sent?", 0))
            executor.on(-1, { ExecutionResult.Retry("connection reset") })
            queue.process(accountId, executor)

            queue.enqueue(accountId, -1, DeleteCard(1, 10))

            assertEquals(
                listOf(CreateCard(1, 10, "Sent?", 0), DeleteCard(1, 10)),
                operations(accountId)
            )
        }
    }

    @Nested
    inner class Process {
        @Test
        fun `runs operations in queue order and removes them when done`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 101, move(1))
            queue.enqueue(accountId, 100, UpdateCard(1, 10))
            queue.enqueue(accountId, 101, SetLabels(1, 11, emptyList()))

            val result = queue.process(accountId, executor)

            assertEquals(listOf(101L, 100L, 101L), executor.calls.map { it.first })
            assertEquals(ProcessResult(done = 3), result)
            assertEquals(emptyList<PendingOperationEntity>(), queued(accountId))
        }

        @Test
        fun `a retried card waits while other cards go on`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, move(1))
            queue.enqueue(accountId, 100, UpdateCard(1, 11))
            queue.enqueue(accountId, 101, move(2))
            executor.on(100, { ExecutionResult.Retry("503") })

            val result = queue.process(accountId, executor)

            assertEquals(listOf(100L, 101L), executor.calls.map { it.first })
            assertEquals(ProcessResult(done = 1, retried = 1), result)
            val waiting = queued(accountId).first()
            assertEquals(1, waiting.attempts)
            assertEquals("503", waiting.lastError)
            assertEquals(clock.now.plusSeconds(5), waiting.nextAttemptAt)
        }

        @Test
        fun `does not run operations before their retry time`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, move(1))
            executor.on(100, { ExecutionResult.Retry(null) })
            queue.process(accountId, executor)

            clock.advance(Duration.ofSeconds(4))
            queue.process(accountId, executor)
            clock.advance(Duration.ofSeconds(1))
            queue.process(accountId, executor)

            assertEquals(2, executor.calls.size)
            assertEquals(emptyList<PendingOperationEntity>(), queued(accountId))
        }

        @Test
        fun `points later operations at the server id of a created card`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, -1, CreateCard(1, 10, "New", 0))
            queue.enqueue(accountId, -1, SetLabels(1, 10, listOf(7)))
            executor.on(-1, { ExecutionResult.Done(serverId = 555) })

            queue.process(accountId, executor)

            assertEquals(listOf(-1L, 555L), executor.calls.map { it.first })
        }

        @Test
        fun `treats an unexpected executor error as temporary`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, move(1))
            executor.on(100, { throw IllegalStateException("boom") })

            val result = queue.process(accountId, executor)

            assertEquals(ProcessResult(retried = 1), result)
            assertEquals("boom", queued(accountId).single().lastError)
        }

        @Test
        fun `fails an operation whose payload cannot be read`() = runTest {
            val accountId = account()
            dao.enqueue(
                PendingOperationEntity(
                    accountId = accountId,
                    type = OperationType.UPDATE,
                    entityType = EntityType.CARD,
                    entityId = 100,
                    payload = "{not json",
                    createdAt = clock.now
                )
            )

            val result = queue.process(accountId, executor)

            assertEquals(ProcessResult(failed = 1), result)
            assertTrue(queued(accountId).single().failed)
            assertTrue(executor.calls.isEmpty())
        }

        @Test
        fun `keeps an operation interrupted by cancellation`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, move(1))
            executor.on(100, { throw CancellationException("app closed") })

            assertThrows<CancellationException> { queue.process(accountId, executor) }

            val kept = queued(accountId).single()
            assertEquals(0, kept.attempts)
            assertEquals(listOf(move(1)), operations(accountId))
        }

        @Test
        fun `repeats an operation sent before a crash, which is safe because it is idempotent`() =
            runTest {
                val accountId = account()
                queue.enqueue(accountId, 100, move(4))
                executor.on(100, { ExecutionResult.Retry("connection lost after sending") })
                queue.process(accountId, executor)
                clock.advance(Duration.ofSeconds(5))

                queue.process(accountId, executor)

                assertEquals(listOf(move(4), move(4)), executor.calls.map { it.second })
                assertEquals(emptyList<PendingOperationEntity>(), queued(accountId))
            }

        @Test
        fun `only processes the given account`() = runTest {
            val ana = account()
            val luis = Fixtures.account(db, "luis")
            queue.enqueue(luis, 100, move(1))

            assertEquals(ProcessResult(), queue.process(ana, executor))
            assertEquals(1, queued(luis).size)
        }
    }

    @Nested
    inner class Failures {
        @Test
        fun `keeps permanently failed operations and blocks their card`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, UpdateCard(1, 10))
            queue.enqueue(accountId, 100, move(2))
            executor.on(100, { ExecutionResult.Failed("413 too large") })

            val result = queue.process(accountId, executor)
            clock.advance(Duration.ofDays(1))
            queue.process(accountId, executor)

            assertEquals(ProcessResult(failed = 1), result)
            assertEquals(1, executor.calls.size)
            val failed = queued(accountId).first()
            assertTrue(failed.failed)
            assertEquals("413 too large", failed.lastError)
        }

        @Test
        fun `retry makes a failed operation run on the next sync`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, UpdateCard(1, 10))
            executor.on(100, { ExecutionResult.Failed("400") })
            queue.process(accountId, executor)

            queue.retry(queued(accountId).single().id)
            queue.process(accountId, executor)

            assertEquals(emptyList<PendingOperationEntity>(), queued(accountId))
        }

        @Test
        fun `discard drops a failed operation`() = runTest {
            val accountId = account()
            queue.enqueue(accountId, 100, UpdateCard(1, 10))
            executor.on(100, { ExecutionResult.Failed("400") })
            queue.process(accountId, executor)

            queue.observeFailed(accountId).test {
                assertEquals(1, awaitItem().size)
                queue.discard(queued(accountId).single().id)
                assertEquals(0, awaitItem().size)
            }
        }

        @Test
        fun `counts pending operations`() = runTest {
            val accountId = account()

            queue.observePendingCount(accountId).test {
                assertEquals(0, awaitItem())
                queue.enqueue(accountId, 100, move(1))
                assertEquals(1, awaitItem())
            }
        }
    }

    @Nested
    inner class Backoff {
        @Test
        fun `doubles from 5 seconds up to one hour`() {
            val backoff = RetryBackoff(FixedRandom(0.5))
            val delays = (0..12).map { backoff.delay(it).seconds }

            assertEquals(
                listOf(5L, 10L, 20L, 40L, 80L, 160L, 320L, 640L, 1280L, 2560L, 3600L, 3600L, 3600L),
                delays
            )
            assertEquals(3600L, backoff.delay(Int.MAX_VALUE).seconds)
        }

        @Test
        fun `spreads delays by up to 20 percent either way`() {
            val low = RetryBackoff(FixedRandom(0.0)).delay(1)
            val high = RetryBackoff(FixedRandom(1.0)).delay(1)

            assertEquals(Duration.ofSeconds(8), low)
            assertEquals(Duration.ofSeconds(12), high)
        }
    }

    @Test
    fun `result counts each outcome`() {
        val result = ProcessResult()
            .plus(ExecutionResult.Done())
            .plus(ExecutionResult.Retry(null))
            .plus(ExecutionResult.Failed(null))

        assertEquals(ProcessResult(done = 1, retried = 1, failed = 1), result)
    }
}
