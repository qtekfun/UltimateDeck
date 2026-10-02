// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = Amber40
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = Amber80
)

/**
 * The color scheme for [settings]. [dynamicLight] and [dynamicDark] are the wallpaper
 * colors (Android 12+), or null where they do not exist.
 */
fun colorSchemeFor(
    settings: AppSettings,
    systemDark: Boolean,
    dynamicLight: ColorScheme?,
    dynamicDark: ColorScheme?
): ColorScheme {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dynamic = if (dark) dynamicDark else dynamicLight
    val scheme =
        (if (settings.dynamicColor) dynamic else null) ?: if (dark) DarkColors else LightColors
    return if (dark && settings.amoled) scheme.toAmoled() else scheme
}

/** Pure black behind everything, for OLED screens; containers stay just visible. */
internal fun ColorScheme.toAmoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = AmoledLow,
    surfaceContainer = AmoledContainer,
    surfaceContainerHigh = AmoledHigh,
    surfaceContainerHighest = AmoledHighest,
    surfaceBright = AmoledHighest
)

@Composable
fun UltimateDeckTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = colorSchemeFor(
        settings = settings,
        systemDark = isSystemInDarkTheme(),
        dynamicLight = if (dynamic) dynamicLightColorScheme(context) else null,
        dynamicDark = if (dynamic) dynamicDarkColorScheme(context) else null
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = UltimateDeckTypography,
        content = content
    )
}
