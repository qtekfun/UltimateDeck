// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DragGeometryTest {

    @Nested
    inner class DropIndex {
        @Test
        fun `drops at the top of an empty column`() {
            assertEquals(0, dropIndex(500f, emptyList()))
        }

        @Test
        fun `drops before the first card whose center is below the pointer`() {
            assertEquals(2, dropIndex(250f, listOf(50f, 150f, 300f, 400f)))
        }

        @Test
        fun `drops at the end when the pointer is below every card`() {
            assertEquals(3, dropIndex(900f, listOf(50f, 150f, 300f)))
        }

        @Test
        fun `counts cards scrolled away above the visible ones`() {
            assertEquals(3, dropIndex(250f, listOf(null, null, 200f, 300f)))
        }

        @Test
        fun `stops at cards scrolled away below the visible ones`() {
            assertEquals(2, dropIndex(900f, listOf(100f, 200f, null, null)))
        }
    }

    @Nested
    inner class ColumnAt {
        private val columns = mapOf(0 to 0f..300f, 1 to 320f..620f, 2 to 640f..940f)

        @Test
        fun `finds the column under the pointer`() {
            assertEquals(1, columnAt(400f, columns))
        }

        @Test
        fun `picks the closest column in the gap between two`() {
            assertEquals(1, columnAt(318f, columns))
        }

        @Test
        fun `picks the closest column outside every column`() {
            assertEquals(2, columnAt(2000f, columns))
            assertEquals(0, columnAt(-50f, columns))
        }

        @Test
        fun `returns null when no column is laid out`() {
            assertNull(columnAt(10f, emptyMap()))
        }
    }

    @Nested
    inner class AutoScrollVelocity {
        private fun velocity(position: Float) = autoScrollVelocity(
            position = position,
            start = 0f,
            end = 1000f,
            edge = 100f,
            maxSpeed = 20f
        )

        @Test
        fun `does not scroll away from the edges`() {
            assertEquals(0f, velocity(500f))
            assertEquals(0f, velocity(100f))
            assertEquals(0f, velocity(900f))
        }

        @Test
        fun `scrolls backwards near the start, faster closer to it`() {
            assertEquals(-10f, velocity(50f))
            assertEquals(-20f, velocity(0f))
        }

        @Test
        fun `scrolls forwards near the end, faster closer to it`() {
            assertEquals(10f, velocity(950f))
            assertEquals(20f, velocity(1000f))
        }

        @Test
        fun `caps the speed beyond the edges`() {
            assertEquals(-20f, velocity(-300f))
            assertEquals(20f, velocity(1500f))
        }
    }
}
