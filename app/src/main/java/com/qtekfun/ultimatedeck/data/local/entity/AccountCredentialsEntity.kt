// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/**
 * The app password of an account, encrypted with a Keystore key; only [loginName] is plain.
 * Deleted together with its account.
 */
@Entity(
    tableName = "account_credentials",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class AccountCredentialsEntity(
    @PrimaryKey val accountId: Long,
    val loginName: String,
    val ciphertext: ByteArray,
    val iv: ByteArray
)
