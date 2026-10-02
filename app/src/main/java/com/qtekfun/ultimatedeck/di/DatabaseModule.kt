// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Provides the database; repositories take the DAOs they need from it. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "ultimatedeck.db"

    // The spread copies a tiny array once, when the database is created.
    @Suppress("SpreadOperator")
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): UltimateDeckDatabase = Room.databaseBuilder<UltimateDeckDatabase>(context, DATABASE_NAME)
        // The system SQLite keeps the APK small; tests use the bundled build with the same API.
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .addMigrations(*UltimateDeckDatabase.MIGRATIONS)
        .build()
}
