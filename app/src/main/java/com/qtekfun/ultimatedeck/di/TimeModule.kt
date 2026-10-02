// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlin.random.Random

/** Time and randomness, injected so tests can fix them. */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun clock(): Clock = Clock.systemUTC()

    @Provides
    fun random(): Random = Random.Default
}
