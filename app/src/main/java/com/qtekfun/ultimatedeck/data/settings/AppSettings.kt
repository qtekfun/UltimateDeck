// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings

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
    val favoriteBoardId: Long? = null
)
