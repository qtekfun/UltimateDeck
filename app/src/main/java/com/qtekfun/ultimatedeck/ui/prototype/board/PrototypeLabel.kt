// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

/** Card label for the drag and drop prototype (T02). Real labels come from Deck in T04. */
data class PrototypeLabel(@param:StringRes val name: Int, val color: Color)
