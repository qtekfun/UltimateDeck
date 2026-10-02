// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.local.model.OperationType
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocalIdsAndQueueTest {
    private val db = inMemoryDatabase()
    private val ids = db.localIdDao()
    private val queue = db.pendingOperationDao()
    private val t0 = Instant.parse("2026-10-01T10:00:00Z")

    @AfterEach
    fun close() = db.close()

    private fun operation(accountId: Long, entityId: Long, createdAt: Instant = t0) =
        PendingOperationEntity(
            accountId = accountId,
            type = OperationType.UPDATE,
            entityType = EntityType.CARD,
            entityId = entityId,
            payload = "{}",
            createdAt = createdAt
        )

    @Test
    fun `hands out decreasing negative ids per account and entity type`() = runTest {
        val ana = Fixtures.account(db, "ana")
        val luis = Fixtures.account(db, "luis")

        assertEquals(-1L, ids.nextId(ana, EntityType.CARD))
        assertEquals(-2L, ids.nextId(ana, EntityType.CARD))
        assertEquals(-1L, ids.nextId(ana, EntityType.ATTACHMENT))
        assertEquals(-1L, ids.nextId(luis, EntityType.CARD))
        assertEquals(-3L, ids.nextId(ana, EntityType.CARD))
    }

    @Test
    fun `replacing a local card id carries its links, attachments and snapshot along`() = runTest {
        val accountId = Fixtures.boardWithCards(db, -1)
        db.labelDao().upsert(
            listOf(LabelEntity(accountId, 1, boardId = 1, title = "Bug", color = "f00"))
        )
        db.labelDao().setCardLabels(accountId, -1, listOf(1))
        db.userDao().upsert(listOf(DeckUserEntity(accountId, "ana", "Ana")))
        db.userDao().setAssignees(accountId, -1, listOf("ana"))
        db.attachmentDao().upsert(
            listOf(
                AttachmentEntity(
                    accountId,
                    7,
                    cardId = -1,
                    fileName = "a",
                    mimeType = null,
                    size = 1
                )
            )
        )
        db.cardSnapshotDao().put(Fixtures.snapshot(accountId, -1))

        db.cardDao().updateId(accountId, oldId = -1, newId = 555)

        assertNull(db.cardDao().get(accountId, -1))
        assertNotNull(db.cardDao().get(accountId, 555))
        db.labelDao().observeCardLabels(accountId, 1).test {
            assertEquals(listOf(555L), awaitItem().map { it.cardId })
        }
        db.userDao().observeCardAssignees(accountId, 1).test {
            assertEquals(listOf(555L), awaitItem().map { it.cardId })
        }
        db.attachmentDao().observeForCard(accountId, 555).test {
            assertEquals(listOf(7L), awaitItem().map { it.id })
        }
        assertNotNull(db.cardSnapshotDao().get(accountId, 555))
    }

    @Test
    fun `replacing a local board id carries its columns, labels and cards along`() = runTest {
        val accountId = Fixtures.account(db)
        db.boardDao().upsert(listOf(Fixtures.board(accountId, id = -1)))
        db.stackDao().upsert(listOf(Fixtures.stack(accountId, boardId = -1)))
        db.cardDao().upsert(listOf(Fixtures.card(accountId, boardId = -1)))
        db.labelDao().upsert(
            listOf(LabelEntity(accountId, 1, boardId = -1, title = "Bug", color = "f00"))
        )

        db.boardDao().updateId(accountId, oldId = -1, newId = 42)

        db.stackDao().observeForBoard(accountId, 42).test {
            assertEquals(listOf(10L), awaitItem().map { it.id })
        }
        db.cardDao().observeForBoard(accountId, 42).test {
            assertEquals(listOf(100L), awaitItem().map { it.id })
        }
        db.labelDao().observeForBoard(accountId, 42).test {
            assertEquals(listOf(1L), awaitItem().map { it.id })
        }
    }

    @Test
    fun `lists the operations of the account in queue order`() = runTest {
        val ana = Fixtures.account(db, "ana")
        val luis = Fixtures.account(db, "luis")
        val first = queue.enqueue(operation(ana, 100))
        val second = queue.enqueue(operation(ana, 101, createdAt = t0.plusSeconds(60)))
        queue.enqueue(operation(luis, 100))

        assertEquals(listOf(first, second), queue.all(ana).map { it.id })
        assertEquals(listOf(100L), queue.forEntity(ana, EntityType.CARD, 100).map { it.entityId })
    }

    @Test
    fun `records postponed and failed operations, retries and deletes them`() = runTest {
        val accountId = Fixtures.account(db)
        val id = queue.enqueue(operation(accountId, 100))
        val retries = db.pendingOperationRetryDao()

        retries.recordFailure(id, nextAttemptAt = t0.plusSeconds(30), error = "HTTP 503")
        val postponed = queue.all(accountId).single()
        assertEquals(1 to "HTTP 503", postponed.attempts to postponed.lastError)
        assertEquals(t0.plusSeconds(30), postponed.nextAttemptAt)

        retries.observeFailed(accountId).test {
            assertEquals(emptyList<PendingOperationEntity>(), awaitItem())
            retries.markFailed(id, "HTTP 413")
            assertEquals(2, awaitItem().single().attempts)
            retries.resetForRetry(id, t0)
            assertEquals(emptyList<PendingOperationEntity>(), awaitItem())
        }
        assertEquals(t0, queue.all(accountId).single().nextAttemptAt)

        queue.replacePayload(id, "{\"changed\":true}")
        assertEquals("{\"changed\":true}", queue.all(accountId).single().payload)
        queue.observeCount(accountId).test {
            assertEquals(1, awaitItem())
            queue.delete(listOf(id))
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `points queued operations at the server id`() = runTest {
        val accountId = Fixtures.account(db)
        queue.enqueue(operation(accountId, -1))
        queue.enqueue(operation(accountId, -2))

        queue.remapEntityId(accountId, EntityType.CARD, oldId = -1, newId = 555)

        assertEquals(listOf(555L, -2L), queue.all(accountId).map { it.entityId })
    }

    @Test
    fun `deleting the account empties its queue and id sequences`() = runTest {
        val accountId = Fixtures.account(db)
        queue.enqueue(operation(accountId, 1))
        ids.nextId(accountId, EntityType.CARD)

        db.accountDao().delete(accountId)
        val again = Fixtures.account(db)

        assertEquals(emptyList<PendingOperationEntity>(), queue.all(accountId))
        assertEquals(-1L, ids.nextId(again, EntityType.CARD))
    }
}
