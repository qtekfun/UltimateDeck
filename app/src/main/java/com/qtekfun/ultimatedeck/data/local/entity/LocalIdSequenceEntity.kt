// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import com.qtekfun.ultimatedeck.data.local.model.EntityType

/** Next negative id for entities created offline, per account and entity type. */
@Entity(
    tableName = "local_id_sequence",
    primaryKeys = ["accountId", "entityType"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LocalIdSequenceEntity(val accountId: Long, val entityType: EntityType, val next: Long)
