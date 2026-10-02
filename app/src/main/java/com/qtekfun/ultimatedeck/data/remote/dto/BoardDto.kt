// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A board; with `details=true` it also carries its labels and the users it is shared with. */
@Serializable
data class BoardDto(
    val id: Long,
    val title: String,
    val color: String = "",
    val archived: Boolean = false,
    @Serializable(with = UserRefSerializer::class) val owner: UserDto? = null,
    val labels: List<LabelDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    /** Seconds since the epoch, 0 when not deleted. */
    val deletedAt: Long = 0,
    /** Seconds since the epoch. */
    val lastModified: Long? = null,
    @SerialName("ETag") val etag: String? = null
)
