// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v5 (T12): cards keep the number of attachments the server reports, for the board view. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration4To5 : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE card ADD COLUMN attachmentCount INTEGER NOT NULL DEFAULT 0")
    }
}
