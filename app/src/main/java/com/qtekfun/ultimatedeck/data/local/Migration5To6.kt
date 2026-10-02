// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v6 (T14): title/description conflicts are kept until the user resolves them. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration5To6 : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE card ADD COLUMN conflictFields INTEGER NOT NULL DEFAULT 0")
    }
}
