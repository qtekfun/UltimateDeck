// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.qtekfun.ultimatedeck.data.local.dao.AccountDao
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity

/** Local source of truth (SPEC RF-08). Schemas are exported to app/schemas and versioned. */
@Database(entities = [AccountEntity::class], version = 1, exportSchema = true)
abstract class UltimateDeckDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
