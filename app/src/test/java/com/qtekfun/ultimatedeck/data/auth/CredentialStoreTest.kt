// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.auth

import com.qtekfun.ultimatedeck.data.local.Fixtures
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.Credentials
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CredentialStoreTest {
    private val db = inMemoryDatabase()
    private val store = CredentialStore(db, FakeCipher())

    @AfterEach
    fun close() = db.close()

    @Test
    fun `saves and loads the credentials of an account`() = runTest {
        val accountId = Fixtures.account(db)

        store.save(accountId, Credentials("ana", "app-pässwörd-123"))

        val loaded = store.load(accountId)
        assertEquals("ana", loaded?.loginName)
        assertEquals("app-pässwörd-123", loaded?.appPassword)
    }

    @Test
    fun `stores the app password only encrypted`() = runTest {
        val accountId = Fixtures.account(db)
        val password = "plain-app-password"

        store.save(accountId, Credentials("ana", password))

        val stored = db.credentialsDao().get(accountId)!!
        assertFalse(stored.ciphertext.toString(Charsets.UTF_8).contains(password))
        assertEquals(12, stored.iv.size)
    }

    @Test
    fun `saving again replaces the credentials`() = runTest {
        val accountId = Fixtures.account(db)
        store.save(accountId, Credentials("ana", "old"))

        store.save(accountId, Credentials("ana", "new"))

        assertEquals("new", store.load(accountId)?.appPassword)
    }

    @Test
    fun `credentials are deleted with their account`() = runTest {
        val accountId = Fixtures.account(db)
        store.save(accountId, Credentials("ana", "secret"))

        db.accountDao().delete(accountId)

        assertNull(store.load(accountId))
    }
}
