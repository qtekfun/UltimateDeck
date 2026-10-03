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
private const val KEY_FAVORITE_BOARD = "favorite_board"
private const val KEY_REMINDER_LEAD = "reminder_lead"
private const val KEY_REMINDER_SCOPE = "reminder_scope"

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

    /** Turns one of the on/off settings on or off. */
    fun setFlag(flag: SettingFlag, value: Boolean) =
        preferences.edit { putBoolean(flag.key, value) }

    /** Marks the board that opens at start; null removes the favorite. */
    fun setFavoriteBoard(boardId: Long?) = preferences.edit {
        if (boardId == null) remove(KEY_FAVORITE_BOARD) else putLong(KEY_FAVORITE_BOARD, boardId)
    }

    fun setReminderLead(lead: ReminderLead) = preferences.edit {
        putString(KEY_REMINDER_LEAD, lead.name)
    }

    fun setReminderScope(scope: ReminderScope) =
        preferences.edit { putString(KEY_REMINDER_SCOPE, scope.name) }

    /** Applies every setting at once, from a backup (T18d). */
    fun restore(restored: AppSettings) = preferences.edit {
        putString(KEY_THEME, restored.theme.name)
        putBoolean(SettingFlag.AMOLED.key, restored.amoled)
        putBoolean(SettingFlag.DYNAMIC_COLOR.key, restored.dynamicColor)
        if (restored.favoriteBoardId == null) {
            remove(KEY_FAVORITE_BOARD)
        } else {
            putLong(KEY_FAVORITE_BOARD, restored.favoriteBoardId)
        }
        putBoolean(SettingFlag.REMINDERS.key, restored.reminders)
        putString(KEY_REMINDER_LEAD, restored.reminderLead.name)
        putString(KEY_REMINDER_SCOPE, restored.reminderScope.name)
        putBoolean(SettingFlag.REMINDER_ALARM_CLOCK.key, restored.reminderAlarmClock)
        putBoolean(SettingFlag.ALLOW_DELETING.key, restored.allowDeleting)
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        val theme = preferences.getString(KEY_THEME, null)
        return AppSettings(
            theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: defaults.theme,
            amoled = preferences.getBoolean(SettingFlag.AMOLED.key, defaults.amoled),
            dynamicColor = preferences.getBoolean(
                SettingFlag.DYNAMIC_COLOR.key,
                defaults.dynamicColor
            ),
            favoriteBoardId = KEY_FAVORITE_BOARD.takeIf(preferences::contains)
                ?.let { preferences.getLong(it, 0) },
            reminders = preferences.getBoolean(SettingFlag.REMINDERS.key, defaults.reminders),
            reminderLead = enumValue(KEY_REMINDER_LEAD, defaults.reminderLead),
            reminderScope = enumValue(KEY_REMINDER_SCOPE, defaults.reminderScope),
            reminderAlarmClock =
                preferences.getBoolean(
                    SettingFlag.REMINDER_ALARM_CLOCK.key,
                    defaults.reminderAlarmClock
                ),
            allowDeleting = preferences.getBoolean(
                SettingFlag.ALLOW_DELETING.key,
                defaults.allowDeleting
            )
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

/** The on/off settings, each with its key in the preferences file. */
enum class SettingFlag(internal val key: String) {
    AMOLED("amoled"),
    DYNAMIC_COLOR("dynamic_color"),
    REMINDERS("reminders"),
    REMINDER_ALARM_CLOCK("reminder_alarm_clock"),
    ALLOW_DELETING("allow_deleting")
}
