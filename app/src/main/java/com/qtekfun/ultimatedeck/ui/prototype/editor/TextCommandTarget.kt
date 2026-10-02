// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import com.qtekfun.ultimatedeck.domain.editor.EditResult
import com.qtekfun.ultimatedeck.domain.editor.SourceRange

/** The focused text field, which the toolbar applies its formatting commands to. */
fun interface TextCommandTarget {
    fun apply(command: (String, SourceRange) -> EditResult)
}
