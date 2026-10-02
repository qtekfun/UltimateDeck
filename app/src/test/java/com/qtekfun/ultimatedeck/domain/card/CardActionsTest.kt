// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.BOARD
import com.qtekfun.ultimatedeck.sync.engine.STACK
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import com.qtekfun.ultimatedeck.sync.queue.FixedRandom
import com.qtekfun.ultimatedeck.sync.queue.MutableClock
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CardActionsTest {
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val session = mockk<AccountSession>()
    private val actions = CardActions(session, db, queue, clock, scheduler)

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
    }

    private suspend fun queued() = db.pendingOperationDao().all(ACCOUNT)
        .map { it.entityId to QueuedOperation.decode(it.payload) }

    @Test
    fun `creates a card at the end of its column, pending and queued`() = runTest {
        signedIn()
        db.cardDao().upsert(listOf(card(5).copy(order = 4)))

        actions.create(BOARD, STACK, "  New card ")

        val created = db.cardDao().allForBoard(ACCOUNT, BOARD).single { it.id < 0 }
        assertEquals("New card", created.title)
        assertEquals(5, created.order)
        assertEquals(setOf(CardField.TITLE), CardField.fromMask(created.dirtyFields))
        assertEquals(clock.now, created.localModifiedAt)
        assertEquals(
            listOf(created.id to QueuedOperation.CreateCard(BOARD, STACK, "New card", 5)),
            queued()
        )
        verify { scheduler.requestSync() }
    }

    @Test
    fun `the first card of an empty column gets order zero`() = runTest {
        signedIn()

        actions.create(BOARD, STACK, "First")

        assertEquals(0, db.cardDao().allForBoard(ACCOUNT, BOARD).single().order)
    }

    @Test
    fun `a blank title or no account creates nothing`() = runTest {
        signedIn()
        actions.create(BOARD, STACK, "   ")
        every { session.activeAccount } returns flowOf(null)
        actions.create(BOARD, STACK, "Card")
        actions.setArchived(5, true)
        actions.delete(5)

        assertEquals(emptyList<Any>(), db.cardDao().allForBoard(ACCOUNT, BOARD))
        assertEquals(emptyList<Any>(), queued())
        verify(exactly = 0) { scheduler.requestSync() }
    }

    @Test
    fun `archiving and undoing leaves a single queued change`() = runTest {
        signedIn()
        db.cardDao().upsert(listOf(card(5)))

        actions.setArchived(5, true)
        assertEquals(true, db.cardDao().get(ACCOUNT, 5)?.archived)
        actions.setArchived(5, false)

        assertEquals(false, db.cardDao().get(ACCOUNT, 5)?.archived)
        assertEquals(listOf(5L to QueuedOperation.ArchiveCard(BOARD, STACK, false)), queued())
    }

    @Test
    fun `deleting a card created here leaves no trace`() = runTest {
        signedIn()
        actions.create(BOARD, STACK, "Oops")
        val id = db.cardDao().allForBoard(ACCOUNT, BOARD).single().id

        actions.delete(id)

        assertNull(db.cardDao().get(ACCOUNT, id))
        assertEquals(emptyList<Any>(), queued())
    }

    @Test
    fun `deleting a synced card hides it and queues the deletion`() = runTest {
        signedIn()
        db.cardDao().upsert(listOf(card(5)))

        actions.delete(5)

        assertNotNull(db.cardDao().get(ACCOUNT, 5)?.deletedAt)
        assertEquals(listOf(5L to QueuedOperation.DeleteCard(BOARD, STACK)), queued())
        verify { scheduler.requestSync() }
    }

    @Test
    fun `changes to unknown cards are ignored`() = runTest {
        signedIn()

        actions.setArchived(9, true)
        actions.delete(9)

        assertEquals(emptyList<Any>(), queued())
    }

    @Test
    fun `moving a card saves the new order and queues one move`() = runTest {
        signedIn()
        db.stackDao().upsert(listOf(StackEntity(ACCOUNT, 11, BOARD, "Done", 1)))
        db.cardDao().upsert(
            listOf(
                card(5).copy(order = 999),
                card(6, stackId = 11).copy(order = 50),
                card(7, stackId = 11)
            )
        )

        actions.move(5, toStackId = 11, columnOrder = listOf(6, 5, 7))
        actions.move(5, toStackId = 11, columnOrder = listOf(7, 6, 5))

        val byId = db.cardDao().allForBoard(ACCOUNT, BOARD).associateBy { it.id }
        assertEquals(listOf(11L, 2), byId.getValue(5).let { listOf(it.stackId, it.order.toLong()) })
        assertEquals(setOf(CardField.POSITION), CardField.fromMask(byId.getValue(5).dirtyFields))
        assertEquals(1 to 0, byId.getValue(6).order to byId.getValue(6).dirtyFields)
        assertEquals(0 to 0, byId.getValue(7).order to byId.getValue(7).dirtyFields)
        assertEquals(listOf(5L to QueuedOperation.MoveCard(BOARD, 11, 11, 2)), queued())
        verify(exactly = 2) { scheduler.requestSync() }
    }

    @Test
    fun `a move of an unknown card or outside its column does nothing`() = runTest {
        signedIn()
        db.cardDao().upsert(listOf(card(5)))

        actions.move(9, STACK, listOf(9))
        actions.move(5, STACK, listOf(6))
        every { session.activeAccount } returns flowOf(null)
        actions.move(5, STACK, listOf(5))

        assertEquals(emptyList<Any>(), queued())
        assertEquals(0, db.cardDao().get(ACCOUNT, 5)?.dirtyFields)
    }
}
