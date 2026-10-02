// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import com.qtekfun.ultimatedeck.data.local.dao.AccountDao
import com.qtekfun.ultimatedeck.data.local.dao.AttachmentDao
import com.qtekfun.ultimatedeck.data.local.dao.BoardDao
import com.qtekfun.ultimatedeck.data.local.dao.CardDao
import com.qtekfun.ultimatedeck.data.local.dao.CardLocalEditDao
import com.qtekfun.ultimatedeck.data.local.dao.CardSnapshotDao
import com.qtekfun.ultimatedeck.data.local.dao.CredentialsDao
import com.qtekfun.ultimatedeck.data.local.dao.LabelDao
import com.qtekfun.ultimatedeck.data.local.dao.LocalIdDao
import com.qtekfun.ultimatedeck.data.local.dao.PendingOperationDao
import com.qtekfun.ultimatedeck.data.local.dao.StackDao
import com.qtekfun.ultimatedeck.data.local.dao.UserDao
import com.qtekfun.ultimatedeck.data.local.entity.AccountCredentialsEntity
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardAssigneeCrossRef
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardLabelCrossRef
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.entity.LocalIdSequenceEntity
import com.qtekfun.ultimatedeck.data.local.entity.PendingOperationEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity

/** Local source of truth (SPEC RF-08). Schemas are exported to app/schemas and versioned. */
@Database(
    entities = [
        AccountEntity::class,
        BoardEntity::class,
        StackEntity::class,
        CardEntity::class,
        LabelEntity::class,
        CardLabelCrossRef::class,
        DeckUserEntity::class,
        CardAssigneeCrossRef::class,
        AttachmentEntity::class,
        CardServerSnapshotEntity::class,
        PendingOperationEntity::class,
        LocalIdSequenceEntity::class,
        AccountCredentialsEntity::class
    ],
    version = UltimateDeckDatabase.VERSION,
    exportSchema = true
)
@ColumnTypeConverters(Converters::class)
abstract class UltimateDeckDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 1

        /**
         * Migrations from each released version to the next. There is no destructive fallback:
         * raising [VERSION] requires adding its migration here (checked by DatabaseSchemaTest).
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }

    abstract fun accountDao(): AccountDao

    abstract fun boardDao(): BoardDao

    abstract fun stackDao(): StackDao

    abstract fun cardDao(): CardDao

    abstract fun labelDao(): LabelDao

    abstract fun userDao(): UserDao

    abstract fun attachmentDao(): AttachmentDao

    abstract fun cardLocalEditDao(): CardLocalEditDao

    abstract fun cardSnapshotDao(): CardSnapshotDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun localIdDao(): LocalIdDao

    abstract fun credentialsDao(): CredentialsDao
}
