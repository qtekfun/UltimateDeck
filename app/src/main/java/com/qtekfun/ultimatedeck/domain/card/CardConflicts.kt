// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.sync.conflict.TextConflict

/** The unresolved title/description conflicts of [card], with both versions, title first. */
fun textConflicts(card: CardEntity, server: CardServerSnapshotEntity?): List<TextConflict> {
    if (server == null) return emptyList()
    val fields = CardField.fromMask(card.conflictFields)
    return listOfNotNull(
        TextConflict(CardField.TITLE, card.title, server.title).takeIf {
            CardField.TITLE in fields
        },
        TextConflict(CardField.DESCRIPTION, card.description, server.description)
            .takeIf { CardField.DESCRIPTION in fields }
    )
}
