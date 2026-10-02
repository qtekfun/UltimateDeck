// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v7 (T16): who belongs to each board, so only members are offered for assigning. */
// Database version numbers are the migration itself, not magic numbers.
@Suppress("MagicNumber")
object Migration6To7 : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `board_member` (`accountId` INTEGER NOT NULL, " +
                "`boardId` INTEGER NOT NULL, `uid` TEXT NOT NULL, " +
                "PRIMARY KEY(`accountId`, `boardId`, `uid`), " +
                "FOREIGN KEY(`accountId`, `boardId`) REFERENCES `board`(`accountId`, `id`) " +
                "ON UPDATE CASCADE ON DELETE CASCADE , " +
                "FOREIGN KEY(`accountId`, `uid`) REFERENCES `deck_user`(`accountId`, `uid`) " +
                "ON UPDATE CASCADE ON DELETE CASCADE )"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_board_member_accountId_uid` " +
                "ON `board_member` (`accountId`, `uid`)"
        )
    }
}
