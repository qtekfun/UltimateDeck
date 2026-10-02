// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.model

/**
 * Editable card fields, each with its bit in `card.dirtyFields`. A set bit means the local value
 * changed since the last sync; the conflict resolver (T08) compares it with the server snapshot.
 */
enum class CardField {
    TITLE,
    DESCRIPTION,
    DUE_DATE,
    POSITION,
    ARCHIVED,
    DONE,
    LABELS,
    ASSIGNEES;

    /** Bit of the field in the mask. Only append new fields: the order is stored in the database. */
    val bit: Int get() = 1 shl ordinal

    companion object {
        /** The fields whose bits are set in [mask]. */
        fun fromMask(mask: Int): Set<CardField> = entries.filterTo(mutableSetOf()) {
            mask and it.bit !=
                0
        }

        fun maskOf(fields: Collection<CardField>): Int = fields.fold(0) { mask, field ->
            mask or
                field.bit
        }
    }
}
