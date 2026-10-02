// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v4 (T09): ETags of the last pull, and cards deleted on the server while edited here. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration3To4 : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE account ADD COLUMN boardsEtag TEXT")
        connection.execSQL("ALTER TABLE board ADD COLUMN stacksEtag TEXT")
        connection.execSQL("ALTER TABLE card ADD COLUMN deletedOnServer INTEGER NOT NULL DEFAULT 0")
    }
}
