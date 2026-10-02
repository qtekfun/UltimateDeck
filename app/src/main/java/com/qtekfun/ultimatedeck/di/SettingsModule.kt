// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.di

import android.content.Context
import android.content.SharedPreferences
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Named(SettingsRepository.SETTINGS_PREFERENCES)
    fun settingsPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(SettingsRepository.SETTINGS_PREFERENCES, Context.MODE_PRIVATE)
}
