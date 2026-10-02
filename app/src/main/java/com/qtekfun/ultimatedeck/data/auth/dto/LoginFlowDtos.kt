// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.auth.dto

import kotlinx.serialization.Serializable

/** `status.php`: present on every Nextcloud server, no login needed. */
@Serializable
data class StatusDto(
    val installed: Boolean = false,
    val maintenance: Boolean = false,
    val version: String? = null,
    val productname: String? = null
)

/** Response of POST index.php/login/v2. */
@Serializable
data class LoginStartDto(val poll: Poll, val login: String) {
    @Serializable
    data class Poll(val token: String, val endpoint: String)
}

/** Response of the poll endpoint once the user has logged in. Returned only once. */
@Serializable
data class LoginResultDto(val server: String, val loginName: String, val appPassword: String)

/** OCS envelope: `{"ocs": {"meta": …, "data": …}}`. */
@Serializable
data class OcsResponse<T>(val ocs: Ocs<T>) {
    @Serializable
    data class Ocs<T>(val data: T)
}

@Serializable
data class CapabilitiesData(val capabilities: Capabilities = Capabilities()) {
    @Serializable
    data class Capabilities(val deck: DeckCapability? = null)

    /** Present only when the Deck app is enabled for the user. */
    @Serializable
    data class DeckCapability(val version: String = "", val apiVersions: List<String> = emptyList())
}
