// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.board.NameDialog
import com.qtekfun.ultimatedeck.ui.boards.deckColor

/** Deck's own board colors, as hex RGB without '#'. */
private val BOARD_COLORS = listOf("0082c9", "00a15f", "f1db50", "e9322d", "a14bd6", "6d6d6d")

/** Name and color of a new board (T15c). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewBoardDialog(onCreate: (title: String, color: String) -> Unit, onDismiss: () -> Unit) {
    var color by rememberSaveable { mutableStateOf(BOARD_COLORS.first()) }
    NameDialog(
        title = R.string.board_new_title,
        onCreate = { onCreate(it, color) },
        onDismiss = onDismiss
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BOARD_COLORS.forEachIndexed { index, option ->
                val label = stringResource(R.string.board_color_option, index + 1)
                Box(
                    Modifier
                        .size(48.dp)
                        .selectable(option == color, role = Role.RadioButton) { color = option }
                        .semantics { contentDescription = label }
                ) {
                    Box(
                        Modifier
                            .size(if (option == color) 40.dp else 32.dp)
                            .background(deckColor(option), CircleShape)
                            .then(
                                if (option == color) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}
