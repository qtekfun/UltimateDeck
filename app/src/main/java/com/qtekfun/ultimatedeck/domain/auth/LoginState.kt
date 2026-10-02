// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.auth

/** Steps of the login (RF-01), as shown to the user. */
sealed interface LoginState {
    data object CheckingServer : LoginState

    /** The user must log in at [loginUrl] in the browser; the app polls meanwhile. */
    data class WaitingForBrowser(val loginUrl: String) : LoginState

    data object Verifying : LoginState

    data class LoggedIn(val accountId: Long) : LoginState

    data class Failed(val error: LoginError) : LoginState
}

/** Why a login failed, each with its own clear message in the UI. */
enum class LoginError {
    INVALID_URL,
    INSECURE_URL,
    NOT_NEXTCLOUD,
    UNREACHABLE,
    TLS_ERROR,
    DECK_MISSING,
    DECK_TOO_OLD,
    EXPIRED,
    UNKNOWN
}
