// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.sync.conflict.TextConflict
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.snapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardConflictsTest {
    @Test
    fun `lists the unresolved conflicts with both versions, title first`() {
        val both = CardField.maskOf(listOf(CardField.DESCRIPTION, CardField.TITLE))
        val mine = card(5, title = "Mine").copy(description = "My notes", conflictFields = both)
        val theirs = snapshot(5, title = "Theirs").copy(description = "Their notes")

        assertEquals(
            listOf(
                TextConflict(CardField.TITLE, "Mine", "Theirs"),
                TextConflict(CardField.DESCRIPTION, "My notes", "Their notes")
            ),
            textConflicts(mine, theirs)
        )
        assertEquals(
            emptyList<TextConflict>(),
            textConflicts(mine.copy(conflictFields = 0), theirs)
        )
        assertEquals(emptyList<TextConflict>(), textConflicts(mine, null))
    }
}
