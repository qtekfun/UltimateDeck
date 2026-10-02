// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.remote

import androidx.compose.ui.graphics.Color
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.LabelDto
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import com.qtekfun.ultimatedeck.ui.prototype.board.PrototypeCard
import com.qtekfun.ultimatedeck.ui.prototype.board.PrototypeColumn
import com.qtekfun.ultimatedeck.ui.prototype.board.PrototypeLabel
import java.time.ZoneId

/**
 * Online, read-only view of the account's boards (T06), until the offline board list of T11.
 * Maps API responses to the board prototype models.
 */
data class BoardSummary(val id: Long, val title: String, val color: Color)

/** Why loading from the server failed, each with its own message. */
enum class RemoteError { UNAUTHORIZED, UNREACHABLE, OTHER }

/** Loading state of a remote screen. */
sealed interface RemoteLoad<out T> {
    data object Loading : RemoteLoad<Nothing>

    data class Loaded<T>(val value: T) : RemoteLoad<T>

    data class Failed(val error: RemoteError) : RemoteLoad<Nothing>
}

private const val OPAQUE = 0xFF000000
private const val NEXTCLOUD_BLUE = 0xFF0082C9
private const val RGB_DIGITS = 6
private const val HEX = 16

/** Deck colors are hex RGB without '#'; anything else falls back to the Nextcloud blue. */
fun deckColor(hex: String): Color = hex.removePrefix("#")
    .takeIf { it.length == RGB_DIGITS }
    ?.toLongOrNull(HEX)
    ?.let { Color(OPAQUE or it) }
    ?: Color(NEXTCLOUD_BLUE)

fun ApiResult<*>.toRemoteError(): RemoteError = when (this) {
    ApiResult.Unauthorized -> RemoteError.UNAUTHORIZED
    is ApiResult.NetworkError -> RemoteError.UNREACHABLE
    else -> RemoteError.OTHER
}

/** Columns in order, with their visible cards in order. */
fun List<StackDto>.toColumns(zone: ZoneId = ZoneId.systemDefault()): List<PrototypeColumn> =
    filter { it.deletedAt == 0L }
        .sortedWith(compareBy({ it.order }, { it.id }))
        .map { stack ->
            PrototypeColumn(
                id = stack.id,
                title = stack.title,
                cards = stack.cards
                    .filter { !it.archived && it.deletedAt == 0L }
                    .sortedWith(compareBy({ it.order }, { it.id }))
                    .map { it.toPrototypeCard(zone) }
            )
        }

/** The checklist progress comes from the task items of the description. */
fun CardDto.toPrototypeCard(zone: ZoneId): PrototypeCard {
    val description = description.orEmpty()
    val tasks = MarkdownDocument.parse(description).tasks
    return PrototypeCard(
        id = id,
        title = title,
        description = description,
        labels = labels.orEmpty().map(LabelDto::toPrototypeLabel),
        assignees = assignedUsers.orEmpty().map {
            it.participant.displayName ?: it.participant.uid
        },
        dueDate = DeckDates.fromIso(duedate)?.atZone(zone)?.toLocalDate(),
        attachments = attachmentCount ?: 0,
        checklistDone = tasks.count { it.checked },
        checklistTotal = tasks.size
    )
}

private fun LabelDto.toPrototypeLabel() = PrototypeLabel(title, deckColor(color))
