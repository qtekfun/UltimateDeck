// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A board column with its cards. */
@Serializable
data class StackDto(
    val id: Long,
    val title: String,
    val boardId: Long,
    val order: Int = 0,
    val deletedAt: Long = 0,
    val lastModified: Long? = null,
    val cards: List<CardDto> = emptyList(),
    @SerialName("ETag") val etag: String? = null
)
