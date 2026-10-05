// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.di

import com.qtekfun.ultimatedeck.notify.ReminderBeat
import com.qtekfun.ultimatedeck.notify.ReminderCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** What a beat does for now: set every reminder again. */
@Module
@InstallIn(SingletonComponent::class)
object ReminderBeatModule {
    @Provides
    fun reminderBeat(coordinator: ReminderCoordinator): ReminderBeat =
        ReminderBeat { coordinator.replan() }
}
