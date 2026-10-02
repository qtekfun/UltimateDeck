// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.model.CardAssigneeRow
import com.qtekfun.ultimatedeck.data.local.model.CardLabelRow
import com.qtekfun.ultimatedeck.domain.board.BoardColumn
import com.qtekfun.ultimatedeck.domain.board.CardItem
import com.qtekfun.ultimatedeck.domain.board.CardLabel
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** The columns and cards of a board, read from Room: they work offline and follow every sync. */
class BoardContentRepository @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase
) {
    private val stacks = database.stackDao()
    private val cards = database.cardDao()
    private val labels = database.labelDao()
    private val users = database.userDao()
    private val edits = database.cardLocalEditDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeBoard(boardId: Long): Flow<List<BoardColumn>> =
        session.activeAccount.flatMapLatest { account ->
            if (account == null) {
                flowOf(emptyList())
            } else {
                combine(
                    stacks.observeForBoard(account.id, boardId),
                    cards.observeForBoard(account.id, boardId),
                    labels.observeCardLabels(account.id, boardId),
                    users.observeCardAssignees(account.id, boardId),
                    edits.observeDirtyCardIds(account.id, boardId)
                ) { stacks, cards, labels, assignees, dirty ->
                    buildColumns(stacks, cards, labels, assignees, dirty.toSet())
                }
            }
        }
}

/**
 * Puts each card in its column, keeping the order the queries give (columns by order, cards by
 * order). The checklist progress comes from the task items of the description.
 */
fun buildColumns(
    stacks: List<StackEntity>,
    cards: List<CardEntity>,
    labels: List<CardLabelRow>,
    assignees: List<CardAssigneeRow>,
    pending: Set<Long>
): List<BoardColumn> {
    val labelsByCard = labels.groupBy({ it.cardId }, { CardLabel(it.title, it.color) })
    val peopleByCard = assignees.groupBy({ it.cardId }, { it.displayName })
    val cardsByStack = cards.groupBy { it.stackId }
    return stacks.map { stack ->
        BoardColumn(
            id = stack.id,
            title = stack.title,
            cards = cardsByStack[stack.id].orEmpty().map { card ->
                val tasks = MarkdownDocument.parse(card.description).tasks
                CardItem(
                    id = card.id,
                    title = card.title,
                    description = card.description,
                    labels = labelsByCard[card.id].orEmpty(),
                    assignees = peopleByCard[card.id].orEmpty(),
                    dueDate = card.dueDate,
                    attachments = card.attachmentCount,
                    checklistDone = tasks.count { it.checked },
                    checklistTotal = tasks.size,
                    pendingSync = card.id in pending
                )
            }
        )
    }
}
