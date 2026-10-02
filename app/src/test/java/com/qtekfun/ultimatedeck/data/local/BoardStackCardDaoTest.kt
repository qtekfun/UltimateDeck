// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.local.Fixtures.board
import com.qtekfun.ultimatedeck.data.local.Fixtures.card
import com.qtekfun.ultimatedeck.data.local.Fixtures.stack
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BoardStackCardDaoTest {
    private val db = inMemoryDatabase()

    @AfterEach
    fun close() = db.close()

    @Test
    fun `lists only active boards of the account, by title`() = runTest {
        val ana = Fixtures.account(db, "ana")
        val luis = Fixtures.account(db, "luis")
        db.boardDao().upsert(
            listOf(
                board(ana, 1, "zeta"),
                board(ana, 2, "Alpha"),
                board(ana, 3, "archived").copy(archived = true),
                board(ana, 4, "deleted").copy(deletedAt = Instant.EPOCH),
                board(luis, 1, "other account")
            )
        )

        db.boardDao().observeActive(ana).test {
            assertEquals(listOf("Alpha", "zeta"), awaitItem().map { it.title })
        }
    }

    @Test
    fun `same server id in two accounts are different rows`() = runTest {
        val ana = Fixtures.account(db, "ana")
        val luis = Fixtures.account(db, "luis")
        db.boardDao().upsert(listOf(board(ana, 1, "ana's"), board(luis, 1, "luis's")))

        assertEquals("ana's", db.boardDao().get(ana, 1)?.title)
        assertEquals("luis's", db.boardDao().get(luis, 1)?.title)
    }

    @Test
    fun `upserting a board updates it without dropping its columns and cards`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)

        db.boardDao().upsert(listOf(board(accountId, title = "Renamed")))

        assertEquals("Renamed", db.boardDao().get(accountId, 1)?.title)
        assertEquals("Card 100", db.cardDao().get(accountId, 100)?.title)
    }

    @Test
    fun `orders columns and cards and emits on change`() = runTest {
        val accountId = Fixtures.account(db)
        db.boardDao().upsert(listOf(board(accountId)))
        db.stackDao().upsert(
            listOf(stack(accountId, 11, order = 1), stack(accountId, 10, order = 0))
        )

        db.stackDao().observeForBoard(accountId, 1).test {
            assertEquals(listOf(10L, 11L), awaitItem().map { it.id })
        }
        db.cardDao().observeForBoard(accountId, 1).test {
            assertTrue(awaitItem().isEmpty())
            db.cardDao().upsert(
                listOf(card(accountId, 101, order = 1), card(accountId, 100, order = 0))
            )
            assertEquals(listOf(100L, 101L), awaitItem().map { it.id })
        }
    }

    @Test
    fun `hides archived and deleted cards from the board`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        db.cardDao().upsert(
            listOf(
                card(accountId, 101).copy(archived = true),
                card(accountId, 102).copy(deletedAt = Instant.EPOCH)
            )
        )

        db.cardDao().observeForBoard(accountId, 1).test {
            assertEquals(listOf(100L), awaitItem().map { it.id })
        }
    }

    @Test
    fun `stores instants with millisecond precision`() = runTest {
        val accountId = Fixtures.boardWithCards(db)
        val due = Instant.parse("2026-10-04T10:15:30.123Z")
        db.cardDao().upsert(listOf(card(accountId).copy(dueDate = due)))

        assertEquals(due, db.cardDao().get(accountId, 100)?.dueDate)
    }

    @Test
    fun `deleting the account deletes its boards, columns and cards`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)

        db.accountDao().delete(accountId)

        assertNull(db.boardDao().get(accountId, 1))
        assertNull(db.cardDao().get(accountId, 100))
    }

    @Test
    fun `rejects a card whose column does not exist`() = runTest {
        val accountId = Fixtures.boardWithCards(db)

        assertThrows<Exception> { db.cardDao().upsert(listOf(card(accountId, stackId = 999))) }
    }
}
