// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A card. Dates in [duedate] and [done] are ISO-8601; the others are epoch seconds. */
@Serializable
data class CardDto(
    val id: Long,
    val title: String,
    val stackId: Long,
    val description: String? = null,
    val type: String = "plain",
    val order: Int = 0,
    val archived: Boolean = false,
    val duedate: String? = null,
    val done: String? = null,
    @Serializable(with = UserRefSerializer::class) val owner: UserDto? = null,
    val labels: List<LabelDto>? = null,
    val assignedUsers: List<AssignmentDto>? = null,
    val attachmentCount: Int? = null,
    val createdAt: Long? = null,
    val lastModified: Long? = null,
    val deletedAt: Long = 0,
    @SerialName("ETag") val etag: String? = null
)

/** A user assigned to a card. */
@Serializable
data class AssignmentDto(val participant: UserDto)
