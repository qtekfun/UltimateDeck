// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.Serializable

/** A board label; [color] is hex RGB without the leading #. */
@Serializable
data class LabelDto(val id: Long, val title: String, val color: String)
