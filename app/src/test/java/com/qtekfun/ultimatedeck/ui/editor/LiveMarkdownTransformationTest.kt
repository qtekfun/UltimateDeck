// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.sp
import com.qtekfun.ultimatedeck.domain.editor.MarkdownCorpus
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class LiveMarkdownTransformationTest {
    private val styles = LiveMarkdownStyles(
        marker = SpanStyle(),
        strong = SpanStyle(),
        emphasis = SpanStyle(),
        strikethrough = SpanStyle(),
        code = SpanStyle(),
        link = SpanStyle(),
        monospaceBlock = SpanStyle(),
        checkbox = SpanStyle(),
        quote = SpanStyle(),
        headingSizes = listOf(24.sp, 20.sp, 16.sp)
    )

    private fun display(
        source: String,
        cursor: Int = source.length,
        reveal: Boolean = true
    ): String {
        val document = MarkdownDocument.parse(source)
        return LiveMarkdownTransformation(document, TextRange(cursor), styles, reveal)
            .filter(AnnotatedString(source)).text.text
    }

    @Test
    fun `hides inline markers when the cursor is elsewhere`() {
        assertEquals(
            "Ship bold and italic now\nend",
            display("Ship **bold** and _italic_ now\nend")
        )
    }

    @Test
    fun `shows the markers of the span that holds the cursor`() {
        assertEquals("Ship **bold** and italic", display("Ship **bold** and _italic_", cursor = 8))
    }

    @Test
    fun `draws tasks as ballot boxes without their bullet`() {
        assertEquals("\u2610 todo\n\u2611 done\n", display("- [ ] todo\n- [x] done\n", cursor = 0))
    }

    @Test
    fun `draws plain bullets as dots and hides quote markers`() {
        assertEquals("\u2022 item\n\nquoted\n", display("- item\n\n> quoted\n", cursor = 0))
    }

    @Test
    fun `hides heading hashes away from the cursor`() {
        assertEquals("Title\n\ntext", display("## Title\n\ntext"))
    }

    @Test
    fun `never shows markers when revealing is off, even with the cursor inside`() {
        val source = "## Title\n\nShip **bold** and _italic_ ~~old~~\n- [ ] task\n"

        assertEquals(
            "Title\n\nShip bold and italic old\n\u2610 task\n",
            display(source, cursor = source.indexOf("bold") + 1, reveal = false)
        )
    }

    @ParameterizedTest
    @MethodSource("corpus")
    fun `hides every inline marker of the corpus when revealing is off`(name: String) {
        val source = MarkdownCorpus.read(name)
        val document = MarkdownDocument.parse(source)
        val shown = LiveMarkdownTransformation(
            document,
            TextRange(0),
            styles,
            revealMarkers = false
        )
            .filter(AnnotatedString(source))

        document.inlineSpans.flatMap { it.markers }.forEach { marker ->
            val start = shown.offsetMapping.originalToTransformed(marker.start)
            val end = shown.offsetMapping.originalToTransformed(marker.end)
            assertEquals(start, end, "marker $marker of $name is still displayed")
        }
    }

    @ParameterizedTest
    @MethodSource("corpus")
    fun `produces a valid offset mapping for every cursor position`(name: String) {
        val source = MarkdownCorpus.read(name)
        val document = MarkdownDocument.parse(source)
        listOf(0, source.length / 2, source.length).forEach { cursor ->
            val transformed = LiveMarkdownTransformation(document, TextRange(cursor), styles)
                .filter(AnnotatedString(source))
            val length = transformed.text.length
            (0..source.length).forEach {
                assertTrue(transformed.offsetMapping.originalToTransformed(it) in 0..length)
            }
            (0..length).forEach {
                assertTrue(transformed.offsetMapping.transformedToOriginal(it) in 0..source.length)
            }
        }
    }

    companion object {
        @JvmStatic
        fun corpus(): List<String> = MarkdownCorpus.names
    }
}
