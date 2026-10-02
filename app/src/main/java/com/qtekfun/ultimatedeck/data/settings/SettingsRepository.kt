// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val KEY_THEME = "theme"
private const val KEY_AMOLED = "amoled"
private const val KEY_DYNAMIC_COLOR = "dynamic_color"
private const val KEY_FAVORITE_BOARD = "favorite_board"
private const val KEY_REMINDERS = "reminders"
private const val KEY_REMINDER_LEAD = "reminder_lead"
private const val KEY_REMINDER_SCOPE = "reminder_scope"
private const val KEY_REMINDER_ALARM_CLOCK = "reminder_alarm_clock"

/**
 * Per-device preferences (T18). They are not Deck data, so they live in SharedPreferences
 * rather than in Room, and need no extra library.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @Named(SETTINGS_PREFERENCES) private val preferences: SharedPreferences
) {
    /** The current settings, and every change after. */
    val settings: Flow<AppSettings> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(read())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setTheme(theme: ThemeMode) = preferences.edit { putString(KEY_THEME, theme.name) }

    fun setAmoled(amoled: Boolean) = preferences.edit { putBoolean(KEY_AMOLED, amoled) }

    fun setDynamicColor(enabled: Boolean) = preferences.edit {
        putBoolean(KEY_DYNAMIC_COLOR, enabled)
    }

    /** Marks the board that opens at start; null removes the favorite. */
    fun setFavoriteBoard(boardId: Long?) = preferences.edit {
        if (boardId == null) remove(KEY_FAVORITE_BOARD) else putLong(KEY_FAVORITE_BOARD, boardId)
    }

    fun setReminders(enabled: Boolean) = preferences.edit { putBoolean(KEY_REMINDERS, enabled) }

    fun setReminderLead(lead: ReminderLead) = preferences.edit {
        putString(KEY_REMINDER_LEAD, lead.name)
    }

    fun setReminderScope(scope: ReminderScope) =
        preferences.edit { putString(KEY_REMINDER_SCOPE, scope.name) }

    fun setReminderAlarmClock(enabled: Boolean) =
        preferences.edit { putBoolean(KEY_REMINDER_ALARM_CLOCK, enabled) }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        val theme = preferences.getString(KEY_THEME, null)
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: defaults.theme,
            amoled = preferences.getBoolean(KEY_AMOLED, defaults.amoled),
            dynamicColor = preferences.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            favoriteBoardId = KEY_FAVORITE_BOARD.takeIf(preferences::contains)
                ?.let { preferences.getLong(it, 0) },
            reminders = preferences.getBoolean(KEY_REMINDERS, defaults.reminders),
            reminderLead = enumValue(KEY_REMINDER_LEAD, defaults.reminderLead),
            reminderScope = enumValue(KEY_REMINDER_SCOPE, defaults.reminderScope),
            reminderAlarmClock =
                preferences.getBoolean(KEY_REMINDER_ALARM_CLOCK, defaults.reminderAlarmClock)
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(key: String, default: T): T {
        val stored = preferences.getString(key, null)
        return enumValues<T>().firstOrNull { it.name == stored } ?: default
    }

    companion object {
        const val SETTINGS_PREFERENCES = "settings"
    }
}
