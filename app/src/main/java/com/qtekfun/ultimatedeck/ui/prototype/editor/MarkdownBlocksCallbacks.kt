// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

/** What the editor reports back: a new source, the focused text field, a tap to start editing. */
data class MarkdownBlocksCallbacks(
    val onSourceChange: (String) -> Unit,
    val onActiveText: (TextCommandTarget) -> Unit,
    val onTap: () -> Unit
)
