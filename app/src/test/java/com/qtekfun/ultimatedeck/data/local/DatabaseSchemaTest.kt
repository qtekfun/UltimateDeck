// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DatabaseSchemaTest {
    private val schemas = File("schemas/${UltimateDeckDatabase::class.qualifiedName}")

    @Test
    fun `every database version has its exported schema committed`() {
        (1..UltimateDeckDatabase.VERSION).forEach { version ->
            assertTrue(File(schemas, "$version.json").isFile, "missing schema for version $version")
        }
    }

    @Test
    fun `the latest exported schema matches the database version`() {
        val latest = File(schemas, "${UltimateDeckDatabase.VERSION}.json").readText()

        assertTrue(latest.contains("\"version\": ${UltimateDeckDatabase.VERSION},"))
    }

    @Test
    fun `every version after the first is reached by a migration`() {
        val steps = UltimateDeckDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }.toSet()

        assertEquals((2..UltimateDeckDatabase.VERSION).map { it - 1 to it }.toSet(), steps)
    }
}
