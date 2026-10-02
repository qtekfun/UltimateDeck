// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v3 (T08): cards remember when they were last edited locally, for conflict resolution. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration2To3 : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE card ADD COLUMN localModifiedAt INTEGER")
    }
}
