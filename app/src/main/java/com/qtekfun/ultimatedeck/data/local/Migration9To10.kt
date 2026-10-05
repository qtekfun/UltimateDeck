// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v10 (T24): the reminders that were shown, to bring back the ones that were not. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration9To10 : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `shown_reminder` (`accountId` INTEGER NOT NULL, " +
                "`cardId` INTEGER NOT NULL, `at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`accountId`, `cardId`, `at`), " +
                "FOREIGN KEY(`accountId`) REFERENCES `account`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
    }
}
