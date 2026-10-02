// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of POST …/cards. */
@Serializable
data class CreateCardRequest(
    val title: String,
    val order: Int,
    val type: String = "plain",
    val description: String? = null,
    val duedate: String? = null
)

/** Body of PUT …/cards/{id}. Deck requires the whole editable state, including owner and type. */
@Serializable
data class UpdateCardRequest(
    val title: String,
    val owner: String,
    val order: Int,
    val description: String,
    val type: String = "plain",
    val duedate: String? = null,
    val archived: Boolean = false,
    val done: String? = null
)

@Serializable
data class ReorderCardRequest(val order: Int, val stackId: Long)

@Serializable
data class LabelIdRequest(val labelId: Long)

@Serializable
data class UserIdRequest(val userId: String)
