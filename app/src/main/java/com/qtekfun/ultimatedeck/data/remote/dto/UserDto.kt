// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject

/** A Nextcloud user as Deck returns it. */
@Serializable
data class UserDto(val uid: String, @SerialName("displayname") val displayName: String? = null)

/** Deck sends a user either as `{"uid": …, "displayname": …}` or just as its uid string. */
object UserRefSerializer : JsonTransformingSerializer<UserDto>(UserDto.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive) buildJsonObject { put("uid", element) } else element
}
