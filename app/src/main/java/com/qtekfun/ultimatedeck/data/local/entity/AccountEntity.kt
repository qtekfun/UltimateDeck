// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A Nextcloud account. Credentials are not stored here but in the Android Keystore (T06). */
@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverUrl: String,
    val userId: String,
    val displayName: String,
    /** ETag of the last board list pulled, to skip it when nothing changed (T09). */
    val boardsEtag: String? = null
)
