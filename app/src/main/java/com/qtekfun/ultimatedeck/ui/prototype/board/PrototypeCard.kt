// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.annotation.StringRes
import java.time.LocalDate

/** Fake card for the drag and drop prototype (T02). */
data class PrototypeCard(
    val id: Long,
    @param:StringRes val title: Int,
    val labels: List<PrototypeLabel> = emptyList(),
    val assignees: List<String> = emptyList(),
    val dueDate: LocalDate? = null,
    val attachments: Int = 0,
    val checklistDone: Int = 0,
    val checklistTotal: Int = 0
)
