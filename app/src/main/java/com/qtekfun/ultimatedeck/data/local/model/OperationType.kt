// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.model

/** What a queued operation does to its entity. The queue logic itself comes with T07. */
enum class OperationType {
    CREATE,
    UPDATE,
    MOVE,
    ARCHIVE,
    DELETE,
    SET_LABELS,
    SET_ASSIGNEES,
    UPLOAD
}
