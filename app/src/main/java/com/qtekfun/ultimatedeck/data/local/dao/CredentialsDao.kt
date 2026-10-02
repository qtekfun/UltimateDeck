// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.AccountCredentialsEntity

@Dao
interface CredentialsDao {
    /** Credentials have no children, so replacing the row is safe. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(credentials: AccountCredentialsEntity)

    @Query("SELECT * FROM account_credentials WHERE accountId = :accountId")
    suspend fun get(accountId: Long): AccountCredentialsEntity?
}
