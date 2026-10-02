// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
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
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardMetadataActionsTest {
    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val session = mockk<AccountSession>()
    private val actions = CardMetadataActions(session, db, queue, clock, scheduler)
    private val due = Instant.parse("2026-10-04T10:00:00Z")

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        db.labelDao().upsert((1L..2L).map { LabelEntity(ACCOUNT, it, BOARD, "L$it", "fff") })
        db.userDao().upsert(
            listOf(DeckUserEntity(ACCOUNT, "bob", "Bob"), DeckUserEntity(ACCOUNT, "eve", "Eve"))
        )
        db.cardDao().upsert(listOf(card(5)))
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
    }

    private suspend fun queued() = db.pendingOperationDao().all(ACCOUNT)
        .map { it.entityId to QueuedOperation.decode(it.payload) }

    private suspend fun dirty() = CardField.fromMask(db.cardDao().get(ACCOUNT, 5)!!.dirtyFields)

    @Test
    fun `setting and clearing the due date is saved and queued once`() = runTest {
        signedIn()

        actions.setDueDate(5, due)
        actions.setDueDate(5, due)
        assertEquals(due, db.cardDao().get(ACCOUNT, 5)?.dueDate)
        actions.setDueDate(5, null)

        assertEquals(null, db.cardDao().get(ACCOUNT, 5)?.dueDate)
        assertEquals(setOf(CardField.DUE_DATE), dirty())
        assertEquals(listOf(5L to QueuedOperation.UpdateCard(BOARD, STACK)), queued())
        verify(exactly = 2) { scheduler.requestSync() }
    }

    @Test
    fun `labels are replaced, marked and queued with their final set`() = runTest {
        signedIn()

        actions.setLabels(5, setOf(2, 1))
        actions.setLabels(5, setOf(1, 2))
        actions.setLabels(5, setOf(2))

        assertEquals(listOf(2L), db.labelDao().labelIdsOfCard(ACCOUNT, 5))
        assertEquals(setOf(CardField.LABELS), dirty())
        assertEquals(listOf(5L to QueuedOperation.SetLabels(BOARD, STACK, listOf(2))), queued())
    }

    @Test
    fun `assignees are replaced, marked and queued with their final set`() = runTest {
        signedIn()

        actions.setAssignees(5, setOf("eve", "bob"))
        actions.setAssignees(5, setOf("bob", "eve"))

        assertEquals(setOf("bob", "eve"), db.userDao().assigneeUidsOfCard(ACCOUNT, 5).toSet())
        assertEquals(setOf(CardField.ASSIGNEES), dirty())
        assertEquals(
            listOf(5L to QueuedOperation.SetAssignees(BOARD, STACK, listOf("bob", "eve"))),
            queued()
        )
        verify(exactly = 1) { scheduler.requestSync() }
    }

    @Test
    fun `changes to unknown cards or without an account do nothing`() = runTest {
        signedIn()

        actions.setDueDate(9, due)
        every { session.activeAccount } returns flowOf(null)
        actions.setLabels(5, setOf(1))

        assertEquals(emptyList<Any>(), queued())
        verify(exactly = 0) { scheduler.requestSync() }
    }
}
