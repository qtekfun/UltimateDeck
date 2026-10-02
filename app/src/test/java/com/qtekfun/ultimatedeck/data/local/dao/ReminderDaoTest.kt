// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.DueCardRow
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReminderDaoTest {
    private val db = inMemoryDatabase()
    private val soon = Instant.parse("2026-10-03T10:00:00Z")
    private val later = Instant.parse("2026-10-04T10:00:00Z")

    @AfterEach
    fun close() = db.close()

    @Test
    fun `lists open cards with a date, their board and whether they are mine`() = runTest {
        db.seedBoard()
        db.userDao().upsert(
            listOf(DeckUserEntity(ACCOUNT, "ana", "Ana"), DeckUserEntity(ACCOUNT, "bob", "Bob"))
        )
        db.cardDao().upsert(
            listOf(
                card(1).copy(dueDate = later),
                card(2).copy(dueDate = soon),
                card(3),
                card(4).copy(dueDate = soon, archived = true),
                card(5).copy(dueDate = soon, done = soon),
                card(6).copy(dueDate = soon, deletedAt = soon),
                card(7).copy(dueDate = soon, deletedOnServer = true)
            )
        )
        db.userDao().setAssignees(ACCOUNT, 2, listOf("ana"))
        db.userDao().setAssignees(ACCOUNT, 1, listOf("bob"))

        val due = db.reminderDao().observeDueCards(ACCOUNT, "ana").first()

        assertEquals(
            listOf(
                DueCardRow(2, 1, "Board", "Card", soon, assignedToMe = true),
                DueCardRow(1, 1, "Board", "Card", later, assignedToMe = false)
            ),
            due
        )
    }
}
