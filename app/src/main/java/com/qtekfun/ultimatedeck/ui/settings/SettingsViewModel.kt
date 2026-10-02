// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ReminderLead
import com.qtekfun.ultimatedeck.data.settings.ReminderScope
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.data.settings.ThemeMode
import com.qtekfun.ultimatedeck.notify.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MS = 5_000L

/** Appearance settings (T18); the language is kept by the system, see [AppLanguages]. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    fun setTheme(theme: ThemeMode) = repository.setTheme(theme)

    fun setAmoled(amoled: Boolean) = repository.setAmoled(amoled)

    fun setDynamicColor(enabled: Boolean) = repository.setDynamicColor(enabled)

    fun setFavoriteBoard(boardId: Long?) = repository.setFavoriteBoard(boardId)

    fun setReminders(enabled: Boolean) = repository.setReminders(enabled)

    fun setReminderLead(lead: ReminderLead) = repository.setReminderLead(lead)

    fun setReminderScope(scope: ReminderScope) = repository.setReminderScope(scope)

    fun setReminderAlarmClock(enabled: Boolean) = repository.setReminderAlarmClock(enabled)

    /** Whether reminders can ring at the exact time (Android 12+ asks the user). */
    fun canScheduleExact() = reminderScheduler.canScheduleExact()

    /** The favorite as stored, waiting for it to be read (the state starts with defaults). */
    suspend fun storedFavorite(): Long? = repository.settings.first().favoriteBoardId
}
