// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings

/** How long before the due date a reminder is shown (RF-10). */
enum class ReminderLead { AT_DUE, ONE_HOUR, ONE_DAY }

/** Which cards get reminders (RF-10). */
enum class ReminderScope { ASSIGNED_TO_ME, ALL }

/** Light, dark, or whatever the system uses. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Preferences of this device (RF-09). [amoled] only applies to dark themes; the board with
 * [favoriteBoardId] opens when the app starts (T18b).
 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = true,
    val favoriteBoardId: Long? = null,
    val reminders: Boolean = false,
    val reminderLead: ReminderLead = ReminderLead.AT_DUE,
    val reminderScope: ReminderScope = ReminderScope.ASSIGNED_TO_ME,
    /** Aggressive mode: reminders are set like an alarm clock, which no battery saver delays. */
    val reminderAlarmClock: Boolean = false,
    /** Deleting boards and columns is hidden until turned on here, so it is never done by accident. */
    val allowDeleting: Boolean = false
)
