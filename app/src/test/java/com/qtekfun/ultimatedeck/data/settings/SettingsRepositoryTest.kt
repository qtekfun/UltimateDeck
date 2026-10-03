// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings

import android.content.SharedPreferences
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsRepositoryTest {
    private val preferences = FakePreferences()
    private val repository = SettingsRepository(preferences)

    @Test
    fun `starts with the defaults and follows every change`() = runTest {
        repository.settings.test {
            assertEquals(AppSettings(), awaitItem())
            repository.setTheme(ThemeMode.DARK)
            assertEquals(AppSettings(theme = ThemeMode.DARK), awaitItem())
            repository.setFlag(SettingFlag.AMOLED, true)
            assertEquals(AppSettings(theme = ThemeMode.DARK, amoled = true), awaitItem())
            repository.setFlag(SettingFlag.DYNAMIC_COLOR, false)
            assertEquals(
                AppSettings(ThemeMode.DARK, amoled = true, dynamicColor = false),
                awaitItem()
            )
        }
    }

    @Test
    fun `an unknown stored theme falls back to the system one`() = runTest {
        preferences.values["theme"] = "NEON"

        repository.settings.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().theme)
        }
    }

    @Test
    fun `the favorite board is kept, changed and removed`() = runTest {
        repository.settings.test {
            assertEquals(null, awaitItem().favoriteBoardId)
            repository.setFavoriteBoard(19)
            assertEquals(19L, awaitItem().favoriteBoardId)
            repository.setFavoriteBoard(18)
            assertEquals(18L, awaitItem().favoriteBoardId)
            repository.setFavoriteBoard(null)
            assertEquals(null, awaitItem().favoriteBoardId)
        }
    }

    @Test
    fun `reminder preferences are kept`() = runTest {
        repository.settings.test {
            assertEquals(AppSettings(), awaitItem())
            repository.setFlag(SettingFlag.REMINDERS, true)
            awaitItem()
            repository.setReminderLead(ReminderLead.ONE_DAY)
            awaitItem()
            repository.setReminderScope(ReminderScope.ALL)
            assertEquals(
                AppSettings(
                    reminders = true,
                    reminderLead = ReminderLead.ONE_DAY,
                    reminderScope = ReminderScope.ALL
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `the aggressive reminder mode is kept`() = runTest {
        repository.settings.test {
            assertEquals(false, awaitItem().reminderAlarmClock)
            repository.setFlag(SettingFlag.REMINDER_ALARM_CLOCK, true)
            assertEquals(true, awaitItem().reminderAlarmClock)
        }
    }

    @Test
    fun `deleting boards and columns is off until turned on`() = runTest {
        repository.settings.test {
            assertEquals(false, awaitItem().allowDeleting)
            repository.setFlag(SettingFlag.ALLOW_DELETING, true)
            assertEquals(true, awaitItem().allowDeleting)
        }
    }
}
