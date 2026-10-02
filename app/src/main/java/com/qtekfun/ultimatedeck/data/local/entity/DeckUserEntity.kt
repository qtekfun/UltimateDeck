// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey

/** A Nextcloud user that can be assigned to cards, identified by its [uid]. */
@Entity(
    tableName = "deck_user",
    primaryKeys = ["accountId", "uid"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DeckUserEntity(val accountId: Long, val uid: String, val displayName: String)
