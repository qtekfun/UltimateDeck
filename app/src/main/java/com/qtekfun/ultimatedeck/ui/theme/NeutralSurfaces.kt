// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Grey surfaces whatever the accent colors are, so that only what carries information (boards,
 * labels, people, states) has color. Dynamic colors keep the accents.
 */
internal fun ColorScheme.withNeutralSurfaces(dark: Boolean): ColorScheme =
    if (dark) withDarkSurfaces() else withLightSurfaces()

private fun ColorScheme.withDarkSurfaces() = copy(
    background = Color(0xFF121316),
    onBackground = Color(0xFFEDEDF0),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFEDEDF0),
    surfaceVariant = Color(0xFF34363D),
    onSurfaceVariant = Color(0xFFC0C2CA),
    surfaceDim = Color(0xFF121316),
    surfaceBright = Color(0xFF383A41),
    surfaceContainerLowest = Color(0xFF0D0E10),
    surfaceContainerLow = Color(0xFF191A1E),
    surfaceContainer = Color(0xFF1F2025),
    surfaceContainerHigh = Color(0xFF282A30),
    surfaceContainerHighest = Color(0xFF32343B),
    outline = Color(0xFF8C8E97),
    outlineVariant = Color(0xFF43454D)
)

private fun ColorScheme.withLightSurfaces() = copy(
    background = Color(0xFFF4F4F6),
    onBackground = Color(0xFF1A1B1E),
    surface = Color(0xFFF4F4F6),
    onSurface = Color(0xFF1A1B1E),
    surfaceVariant = Color(0xFFE2E3E8),
    onSurfaceVariant = Color(0xFF4A4D55),
    surfaceDim = Color(0xFFDADBDF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFCFD),
    surfaceContainer = Color(0xFFE9EAED),
    surfaceContainerHigh = Color(0xFFE3E4E8),
    surfaceContainerHighest = Color(0xFFDDDEE3),
    outline = Color(0xFF72757E),
    outlineVariant = Color(0xFFC6C8CE)
)
