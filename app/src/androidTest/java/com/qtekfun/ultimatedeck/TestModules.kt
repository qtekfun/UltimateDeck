// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck

import android.content.Context
import android.content.SharedPreferences
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.di.DatabaseModule
import com.qtekfun.ultimatedeck.di.SettingsModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import javax.inject.Singleton

/** A fresh in-memory database per test: the app's real data on the device is never touched. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): UltimateDeckDatabase =
        Room.inMemoryDatabaseBuilder<UltimateDeckDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
}

/** Settings of their own, emptied for each test, so the user's settings stay as they are. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SettingsModule::class])
object TestSettingsModule {
    @Provides
    @Named(SettingsRepository.SETTINGS_PREFERENCES)
    fun settingsPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("ui-test-settings", Context.MODE_PRIVATE)
}
