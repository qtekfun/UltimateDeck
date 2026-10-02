// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v2 (T07): queued operations can be marked as permanently failed. */
object Migration1To2 : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE pending_operation ADD COLUMN failed INTEGER NOT NULL DEFAULT 0"
        )
    }
}
