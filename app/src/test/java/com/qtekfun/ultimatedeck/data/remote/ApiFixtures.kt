// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

/** Deck API responses in src/test/resources/deck-api, based on the documented examples. */
object ApiFixtures {
    fun read(name: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("deck-api/$name")) {
            "Missing fixture $name"
        }
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
