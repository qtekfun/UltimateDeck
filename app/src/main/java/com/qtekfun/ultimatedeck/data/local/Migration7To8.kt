// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v8 (T10): queued operations remember when they were last sent, to avoid duplicates. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration7To8 : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE pending_operation ADD COLUMN startedAt INTEGER")
        // Operations already attempted may have reached the server.
        connection.execSQL("UPDATE pending_operation SET startedAt = createdAt WHERE attempts > 0")
    }
}
