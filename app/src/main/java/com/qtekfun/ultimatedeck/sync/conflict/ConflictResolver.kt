// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.conflict

import com.qtekfun.ultimatedeck.data.local.model.CardField

/** How a synced card ends up locally. */
sealed interface Resolution {
    /**
     * The card to store. [toSend] are local changes the server still needs; [conflicts] are text
     * fields changed on both sides, kept local and unsent until the user decides; [snapshot] is
     * the server state to remember as the new base.
     */
    data class Merged(
        val state: CardState,
        val toSend: Set<CardField>,
        val conflicts: List<TextConflict>,
        val snapshot: CardState
    ) : Resolution

    /** Deleted on the server while edited here: ask whether to keep a local copy or discard it. */
    data class DeletedOnServer(val local: CardState) : Resolution

    /** Deleted on the server and untouched here: drop it. */
    data object RemoveLocally : Resolution
}

/** Both versions of a title or description, for the "your version / server's" dialog. */
data class TextConflict(val field: CardField, val local: String, val server: String)

/**
 * Card conflict rules of SPEC §5, field by field:
 * - not changed locally: the server wins;
 * - changed locally only: the local value wins and is sent;
 * - changed on both sides to the same value: nothing to do;
 * - title or description changed differently on both sides: nothing is overwritten, it is a
 *   [TextConflict] for the user;
 * - any other field changed differently on both sides: the latest change wins (local edit time
 *   against the server's lastModified; ties and unknown times go to the server).
 * It is a pure function, so a sync interrupted and repeated reaches the same decision.
 */
object ConflictResolver {
    fun resolve(local: LocalCard, base: CardState?, server: ServerCard?): Resolution {
        if (server == null) {
            return if (local.dirty.isEmpty()) {
                Resolution.RemoveLocally
            } else {
                Resolution.DeletedOnServer(
                    local.state
                )
            }
        }
        var state = server.state
        val toSend = mutableSetOf<CardField>()
        val conflicts = mutableListOf<TextConflict>()
        FIELDS.forEach { field ->
            when (decide(field, local, base, server)) {
                Winner.SERVER -> Unit

                Winner.LOCAL -> {
                    state = field.copy(local.state, state)
                    toSend += field.field
                }

                Winner.CONFLICT -> {
                    state = field.copy(local.state, state)
                    conflicts +=
                        TextConflict(field.field, field.text(local.state), field.text(server.state))
                }
            }
        }
        return Resolution.Merged(state, toSend, conflicts, server.state)
    }

    private fun decide(
        field: Field,
        local: LocalCard,
        base: CardState?,
        server: ServerCard
    ): Winner {
        val localValue = field.get(local.state)
        val serverValue = field.get(server.state)
        val serverChanged = base == null || field.get(base) != serverValue
        return when {
            field.field !in local.dirty || localValue == serverValue -> Winner.SERVER
            !serverChanged -> Winner.LOCAL
            field.isText -> Winner.CONFLICT
            isLocalNewer(local, server) -> Winner.LOCAL
            else -> Winner.SERVER
        }
    }

    private fun isLocalNewer(local: LocalCard, server: ServerCard): Boolean {
        val localTime = local.modifiedAt
        val serverTime = server.lastModified
        return localTime != null && serverTime != null && localTime.isAfter(serverTime)
    }

    private enum class Winner { SERVER, LOCAL, CONFLICT }

    /** How to read a field and copy it from one state into another. */
    private class Field(
        val field: CardField,
        val get: (CardState) -> Any?,
        private val set: (CardState, CardState) -> CardState,
        val isText: Boolean = false
    ) {
        fun copy(from: CardState, into: CardState) = set(from, into)

        fun text(state: CardState) = get(state) as String
    }

    private val FIELDS = listOf(
        Field(CardField.TITLE, {
            it.title
        }, { from, into -> into.copy(title = from.title) }, isText = true),
        Field(
            CardField.DESCRIPTION,
            { it.description },
            { from, into -> into.copy(description = from.description) },
            isText = true
        ),
        Field(CardField.DUE_DATE, {
            it.dueDate
        }, { from, into -> into.copy(dueDate = from.dueDate) }),
        Field(
            CardField.POSITION,
            { it.stackId to it.order },
            { from, into -> into.copy(stackId = from.stackId, order = from.order) }
        ),
        Field(CardField.ARCHIVED, {
            it.archived
        }, { from, into -> into.copy(archived = from.archived) }),
        Field(CardField.DONE, { it.done }, { from, into -> into.copy(done = from.done) }),
        Field(CardField.LABELS, {
            it.labelIds
        }, { from, into -> into.copy(labelIds = from.labelIds) }),
        Field(
            CardField.ASSIGNEES,
            { it.assigneeUids },
            { from, into -> into.copy(assigneeUids = from.assigneeUids) }
        )
    )
}
