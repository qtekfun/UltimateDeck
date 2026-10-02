// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ThemeMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ColorSchemeForTest {
    private val wallpaperLight = lightColorScheme(primary = Color.Red)
    private val wallpaperDark = darkColorScheme(primary = Color.Green)

    @Test
    fun `the theme mode decides between light and dark`() {
        val system = AppSettings(dynamicColor = true)

        assertEquals(
            wallpaperDark,
            colorSchemeFor(system, systemDark = true, wallpaperLight, wallpaperDark)
        )
        assertEquals(
            wallpaperLight,
            colorSchemeFor(
                system.copy(theme = ThemeMode.LIGHT),
                systemDark = true,
                wallpaperLight,
                wallpaperDark
            )
        )
        assertEquals(
            wallpaperDark,
            colorSchemeFor(
                system.copy(theme = ThemeMode.DARK),
                systemDark = false,
                wallpaperLight,
                wallpaperDark
            )
        )
    }

    @Test
    fun `without dynamic colors the app colors are used`() {
        val scheme =
            colorSchemeFor(AppSettings(dynamicColor = false), false, wallpaperLight, wallpaperDark)
        val unsupported = colorSchemeFor(AppSettings(), false, null, null)

        assertNotEquals(Color.Red, scheme.primary)
        assertEquals(scheme, unsupported)
    }

    @Test
    fun `AMOLED turns dark backgrounds pure black but leaves light themes alone`() {
        val amoled = AppSettings(theme = ThemeMode.DARK, amoled = true)

        val dark = colorSchemeFor(amoled, false, wallpaperLight, wallpaperDark)
        val light =
            colorSchemeFor(
                amoled.copy(theme = ThemeMode.LIGHT),
                false,
                wallpaperLight,
                wallpaperDark
            )

        assertEquals(Color.Black, dark.background)
        assertEquals(Color.Black, dark.surface)
        assertEquals(Color.Green, dark.primary)
        assertNotEquals(dark.surfaceContainer, dark.surfaceContainerHigh)
        assertEquals(wallpaperLight, light)
    }
}
