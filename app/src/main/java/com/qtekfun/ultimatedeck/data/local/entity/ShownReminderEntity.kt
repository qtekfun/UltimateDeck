// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import java.time.Instant

/** A reminder that was shown, so the ones that were not can be told apart (RF-10). */
@Entity(
    tableName = "shown_reminder",
    primaryKeys = ["accountId", "cardId", "at"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShownReminderEntity(val accountId: Long, val cardId: Long, val at: Instant)
