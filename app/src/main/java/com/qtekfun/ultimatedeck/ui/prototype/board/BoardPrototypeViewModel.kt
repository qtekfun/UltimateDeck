// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.lifecycle.ViewModel
import com.qtekfun.ultimatedeck.domain.board.CardPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Holds the fake board of the drag and drop prototype so moves survive rotation. */
@HiltViewModel
class BoardPrototypeViewModel @Inject constructor() : ViewModel() {
    private val mutableColumns = MutableStateFlow(FakeBoard.columns(LocalDate.now()))
    val columns: StateFlow<List<PrototypeColumn>> = mutableColumns.asStateFlow()

    fun moveCard(from: CardPosition, to: CardPosition) {
        mutableColumns.update { it.withCardMoved(from, to) }
    }

    /** Moves a card to the end of another column; used by accessibility actions. */
    fun moveCardToColumn(cardId: Long, column: Int) {
        val from = mutableColumns.value.positionOf(cardId) ?: return
        moveCard(from, CardPosition(column, Int.MAX_VALUE))
    }
}
