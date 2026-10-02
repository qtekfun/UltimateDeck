// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** A range of a markdown source, [start] inclusive and [end] exclusive. */
data class SourceRange(val start: Int, val end: Int) {
    init {
        require(start in 0..end) { "Invalid range $start..$end" }
    }

    val length: Int get() = end - start

    operator fun contains(offset: Int): Boolean = offset in start until end
}
