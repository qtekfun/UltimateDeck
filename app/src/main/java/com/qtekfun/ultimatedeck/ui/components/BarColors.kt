// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.components

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.qtekfun.ultimatedeck.ui.theme.contentColorOn

/**
 * A top bar filled with a board's [color], with black or white content (and status bar icons),
 * whichever reads. Without a color the bar stays as the theme draws it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun boardBarColors(color: Color): TopAppBarColors {
    if (color == Color.Unspecified) return TopAppBarDefaults.topAppBarColors()
    val content = contentColorOn(color)
    StatusBarIcons(dark = content == Color.Black)
    return TopAppBarDefaults.topAppBarColors(
        containerColor = color,
        scrolledContainerColor = color,
        navigationIconContentColor = content,
        titleContentColor = content,
        actionIconContentColor = content
    )
}

/** Dark or light status bar icons while on screen; the previous ones come back afterwards. */
@Composable
private fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, dark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = dark
        onDispose { before?.let { controller.isAppearanceLightStatusBars = it } }
    }
}
