// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v9 (T17): attachments keep their Deck type, needed to download them. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration8To9 : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE attachment ADD COLUMN type TEXT NOT NULL DEFAULT 'file'")
    }
}
