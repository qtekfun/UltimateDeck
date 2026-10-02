// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.Serializable

/** A card attachment (API v1.1). [type] is `deck_file` or `file`; [data] is the file name. */
@Serializable
data class AttachmentDto(
    val id: Long,
    val cardId: Long,
    val type: String,
    val data: String,
    val createdAt: Long? = null,
    val deletedAt: Long = 0,
    val extendedData: ExtendedData? = null
) {
    @Serializable
    data class ExtendedData(val filesize: Long? = null, val mimetype: String? = null)
}
