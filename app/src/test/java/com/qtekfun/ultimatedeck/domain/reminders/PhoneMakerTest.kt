// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhoneMakerTest {
    @Test
    fun `manufacturers map to their system, ignoring case and spaces`() {
        mapOf(
            "OPPO" to PhoneMaker.COLOROS,
            "realme" to PhoneMaker.COLOROS,
            " OnePlus " to PhoneMaker.COLOROS,
            "Xiaomi" to PhoneMaker.XIAOMI,
            "Redmi" to PhoneMaker.XIAOMI,
            "POCO" to PhoneMaker.XIAOMI,
            "HUAWEI" to PhoneMaker.HUAWEI,
            "HONOR" to PhoneMaker.HUAWEI,
            "samsung" to PhoneMaker.SAMSUNG,
            "vivo" to PhoneMaker.VIVO,
            "iQOO" to PhoneMaker.VIVO,
            "Google" to PhoneMaker.OTHER,
            "" to PhoneMaker.OTHER
        ).forEach { (manufacturer, maker) ->
            assertEquals(maker, PhoneMaker.of(manufacturer), manufacturer)
        }
    }
}
