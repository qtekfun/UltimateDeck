// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.theme.LabelAmber
import com.qtekfun.ultimatedeck.ui.theme.LabelBlue
import com.qtekfun.ultimatedeck.ui.theme.LabelGreen
import com.qtekfun.ultimatedeck.ui.theme.LabelPurple
import com.qtekfun.ultimatedeck.ui.theme.LabelRed
import java.time.LocalDate

/** Sample board shown by the drag and drop prototype (T02). */
object FakeBoard {
    private val bug = PrototypeLabel(R.string.prototype_label_bug, LabelRed)
    private val feature = PrototypeLabel(R.string.prototype_label_feature, LabelBlue)
    private val design = PrototypeLabel(R.string.prototype_label_design, LabelPurple)
    private val urgent = PrototypeLabel(R.string.prototype_label_urgent, LabelAmber)
    private val docs = PrototypeLabel(R.string.prototype_label_docs, LabelGreen)

    private const val ANA = "Ana García"
    private const val LUIS = "Luis Pérez"
    private const val MARTA = "Marta Ruiz"
    private const val IKER = "Iker Sola"

    fun columns(today: LocalDate): List<PrototypeColumn> = listOf(
        PrototypeColumn(1, R.string.prototype_column_todo, todo(today)),
        PrototypeColumn(2, R.string.prototype_column_in_progress, inProgress(today)),
        PrototypeColumn(3, R.string.prototype_column_review, review(today)),
        PrototypeColumn(4, R.string.prototype_column_done, done())
    )

    private fun todo(today: LocalDate) = listOf(
        PrototypeCard(
            id = 101,
            title = R.string.prototype_card_crash_server,
            labels = listOf(bug, urgent),
            assignees = listOf(LUIS),
            dueDate = today.minusDays(1),
            attachments = 2
        ),
        PrototypeCard(
            id = 102,
            title = R.string.prototype_card_attachments,
            labels = listOf(feature),
            dueDate = today.plusDays(9),
            checklistDone = 0,
            checklistTotal = 4
        ),
        PrototypeCard(
            id = 103,
            title = R.string.prototype_card_onboarding,
            labels = listOf(design),
            assignees = listOf(MARTA, ANA)
        ),
        PrototypeCard(
            id = 104,
            title = R.string.prototype_card_fdroid,
            labels = listOf(docs)
        )
    )

    private fun inProgress(today: LocalDate) = listOf(
        PrototypeCard(
            id = 201,
            title = R.string.prototype_card_drag_drop,
            labels = listOf(feature, design),
            assignees = listOf(ANA, IKER, MARTA),
            dueDate = today.plusDays(2),
            attachments = 1,
            checklistDone = 3,
            checklistTotal = 5
        ),
        PrototypeCard(
            id = 202,
            title = R.string.prototype_card_offline_cache,
            labels = listOf(feature),
            assignees = listOf(LUIS),
            checklistDone = 1,
            checklistTotal = 6
        ),
        PrototypeCard(
            id = 203,
            title = R.string.prototype_card_markdown_editor,
            labels = listOf(feature, urgent),
            assignees = listOf(IKER),
            dueDate = today.plusDays(5),
            attachments = 3
        )
    )

    private fun review(today: LocalDate) = listOf(
        PrototypeCard(
            id = 301,
            title = R.string.prototype_card_conflicts,
            labels = listOf(feature),
            assignees = listOf(ANA),
            dueDate = today,
            checklistDone = 4,
            checklistTotal = 4
        ),
        PrototypeCard(
            id = 302,
            title = R.string.prototype_card_accessibility,
            labels = listOf(design),
            assignees = listOf(MARTA)
        )
    )

    private fun done() = listOf(
        PrototypeCard(
            id = 401,
            title = R.string.prototype_card_login_flow,
            labels = listOf(feature),
            assignees = listOf(LUIS, ANA),
            checklistDone = 5,
            checklistTotal = 5
        ),
        PrototypeCard(
            id = 402,
            title = R.string.prototype_card_translations,
            labels = listOf(docs),
            assignees = listOf(IKER)
        ),
        PrototypeCard(
            id = 403,
            title = R.string.prototype_card_dark_mode,
            labels = listOf(design),
            attachments = 4
        )
    )
}
