// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.domain.board.BoardItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoardRepositoryTest {
    private val db = inMemoryDatabase()
    private val account = MutableStateFlow<AccountEntity?>(null)
    private val session = mockk<AccountSession> { every { activeAccount } returns account }
    private val repository = BoardRepository(session, db)

    @AfterEach
    fun close() = db.close()

    @Test
    fun `lists the active boards of the account by title and follows syncs`() = runTest {
        val ana =
            AccountEntity(
                id = 1,
                serverUrl = "https://c.example/",
                userId = "ana",
                displayName = "Ana"
            )
        db.accountDao().insert(ana)
        db.boardDao().upsert(
            listOf(
                BoardEntity(1, 1, "zeta", "fff"),
                BoardEntity(1, 2, "Alpha", "000"),
                BoardEntity(1, 3, "Old", "fff", archived = true)
            )
        )

        repository.observeBoards().test {
            assertEquals(emptyList<BoardItem>(), awaitItem())
            account.value = ana
            assertEquals(
                listOf(BoardItem(2, "Alpha", "000"), BoardItem(1, "zeta", "fff")),
                awaitItem()
            )
            db.boardDao().upsert(listOf(BoardEntity(1, 4, "New", "abc")))
            assertEquals(listOf("Alpha", "New", "zeta"), awaitItem().map { it.title })
            account.value = null
            assertEquals(emptyList<BoardItem>(), awaitItem())
        }
    }
}
