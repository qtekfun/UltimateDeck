// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.board

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MoveItemTest {

    private val board = listOf(
        listOf("a", "b", "c"),
        listOf("d"),
        emptyList()
    )

    @Test
    fun `moves an item down inside its own column`() {
        val result = board.moveItem(CardPosition(0, 0), CardPosition(0, 2))

        assertEquals(listOf("b", "c", "a"), result[0])
    }

    @Test
    fun `moves an item up inside its own column`() {
        val result = board.moveItem(CardPosition(0, 2), CardPosition(0, 0))

        assertEquals(listOf("c", "a", "b"), result[0])
    }

    @Test
    fun `moves an item to another column at the requested index`() {
        val result = board.moveItem(CardPosition(0, 1), CardPosition(1, 0))

        assertEquals(listOf(listOf("a", "c"), listOf("b", "d"), emptyList()), result)
    }

    @Test
    fun `moves an item into an empty column`() {
        val result = board.moveItem(CardPosition(1, 0), CardPosition(2, 0))

        assertEquals(listOf(listOf("a", "b", "c"), emptyList(), listOf("d")), result)
    }

    @Test
    fun `clamps a target index past the end of the column`() {
        val result = board.moveItem(CardPosition(0, 0), CardPosition(1, 99))

        assertEquals(listOf("d", "a"), result[1])
    }

    @Test
    fun `moving an item onto its own position leaves the board unchanged`() {
        val result = board.moveItem(CardPosition(0, 1), CardPosition(0, 1))

        assertEquals(board, result)
    }

    @Test
    fun `does not modify the original board`() {
        board.moveItem(CardPosition(0, 0), CardPosition(1, 0))

        assertEquals(listOf("a", "b", "c"), board[0])
        assertEquals(listOf("d"), board[1])
    }

    @Test
    fun `rejects a source column that does not exist`() {
        assertThrows<IllegalArgumentException> {
            board.moveItem(CardPosition(5, 0), CardPosition(0, 0))
        }
    }

    @Test
    fun `rejects a source index that does not exist`() {
        assertThrows<IllegalArgumentException> {
            board.moveItem(CardPosition(2, 0), CardPosition(0, 0))
        }
    }

    @Test
    fun `rejects a target column that does not exist`() {
        assertThrows<IllegalArgumentException> {
            board.moveItem(CardPosition(0, 0), CardPosition(3, 0))
        }
    }

    @Test
    fun `rejects a negative target index`() {
        assertThrows<IllegalArgumentException> {
            board.moveItem(CardPosition(0, 0), CardPosition(1, -1))
        }
    }
}
