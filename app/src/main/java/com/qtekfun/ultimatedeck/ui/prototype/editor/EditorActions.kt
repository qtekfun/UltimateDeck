// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

/** Formatting actions of the editor toolbar. */
data class EditorActions(
    val bold: () -> Unit,
    val italic: () -> Unit,
    val heading: () -> Unit,
    val bulletList: () -> Unit,
    val taskList: () -> Unit,
    val addTable: () -> Unit
)
