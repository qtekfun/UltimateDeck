// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import com.qtekfun.ultimatedeck.data.local.entity.ShownReminderEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShownReminderDaoTest {
    private val db = inMemoryDatabase()
    private val dao = db.shownReminderDao()
    private val old = Instant.parse("2026-10-01T10:00:00Z")
    private val recent = Instant.parse("2026-10-05T10:00:00Z")

    @AfterEach
    fun close() = db.close()

    @Test
    fun `a reminder shown twice is kept once`() = runTest {
        db.seedBoard()

        dao.insert(ShownReminderEntity(ACCOUNT, 1, recent))
        dao.insert(ShownReminderEntity(ACCOUNT, 1, recent))

        assertEquals(listOf(ShownReminderEntity(ACCOUNT, 1, recent)), dao.all(ACCOUNT))
    }

    @Test
    fun `old records are forgotten, recent ones stay`() = runTest {
        db.seedBoard()
        dao.insert(ShownReminderEntity(ACCOUNT, 1, old))
        dao.insert(ShownReminderEntity(ACCOUNT, 2, recent))

        dao.deleteBefore(Instant.parse("2026-10-04T10:00:00Z"))

        assertEquals(listOf(2L), dao.all(ACCOUNT).map { it.cardId })
    }
}
