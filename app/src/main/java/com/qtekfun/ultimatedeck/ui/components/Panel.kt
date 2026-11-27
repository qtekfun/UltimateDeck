// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val DARK_LUMINANCE = 0.5f

/** The color of cards and panels: always one step lighter than what is behind them. */
@Composable
fun panelColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.surface.luminance() < DARK_LUMINANCE) {
        colors.surfaceContainerHigh
    } else {
        colors.surfaceContainerLowest
    }
}

/** A rounded box that groups related content. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = panelColor(),
        border = border
    ) {
        Column(content = content)
    }
}
