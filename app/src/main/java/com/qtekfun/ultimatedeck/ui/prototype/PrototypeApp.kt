// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.qtekfun.ultimatedeck.ui.prototype.board.BoardPrototypeScreen
import com.qtekfun.ultimatedeck.ui.prototype.editor.EditorPrototypeScreen

/** Minimal navigation between the prototypes until real navigation arrives with T11. */
@Composable
fun PrototypeApp() {
    var openCard by rememberSaveable { mutableStateOf<Long?>(null) }
    if (openCard == null) {
        BoardPrototypeScreen(onOpenCard = { openCard = it })
    } else {
        EditorPrototypeScreen(onBack = { openCard = null })
    }
}
