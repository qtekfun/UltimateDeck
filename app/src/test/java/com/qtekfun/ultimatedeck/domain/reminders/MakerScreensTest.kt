// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MakerScreensTest {
    @Test
    fun `makers with their own auto-start screens list them, newest first`() {
        assertEquals(
            "com.oplus.battery",
            MakerScreens.of(PhoneMaker.COLOROS).first().packageName
        )
        assertEquals(4, MakerScreens.of(PhoneMaker.COLOROS).size)
        assertEquals(3, MakerScreens.of(PhoneMaker.VIVO).size)
        assertEquals(
            listOf(
                Screen(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            MakerScreens.of(PhoneMaker.XIAOMI)
        )
        assertEquals(2, MakerScreens.of(PhoneMaker.HUAWEI).size)
    }

    @Test
    fun `others rely on the app info page`() {
        assertTrue(MakerScreens.of(PhoneMaker.SAMSUNG).isEmpty())
        assertTrue(MakerScreens.of(PhoneMaker.OTHER).isEmpty())
    }
}
