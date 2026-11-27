// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Defaults = Typography()

/** The system font, with heavier titles so that headings stand out without extra color. */
internal val UltimateDeckTypography = Typography(
    headlineSmall = Defaults.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Defaults.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Defaults.titleMedium.copy(fontWeight = FontWeight.Bold)
)
