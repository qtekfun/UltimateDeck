// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

/** Formatting actions; a null action is shown disabled because the editor does not support it. */
data class EditorActions(
    val bold: () -> Unit,
    val italic: () -> Unit,
    val bulletList: () -> Unit,
    val taskList: (() -> Unit)?
)
