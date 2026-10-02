// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountDaoTest {
    private val db = inMemoryDatabase()
    private val dao = db.accountDao()

    @AfterEach
    fun close() = db.close()

    @Test
    fun `inserts and reads an account`() = runTest {
        val id = dao.insert(
            AccountEntity(serverUrl = "https://cloud.example", userId = "ana", displayName = "Ana")
        )

        assertEquals("ana", dao.get(id)?.userId)
        dao.delete(id)
        assertNull(dao.get(id))
    }
}
