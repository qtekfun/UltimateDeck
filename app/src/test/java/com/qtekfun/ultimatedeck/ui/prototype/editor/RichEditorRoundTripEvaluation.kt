// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import com.mohamedrejeb.richeditor.model.RichTextState
import com.qtekfun.ultimatedeck.domain.editor.MarkdownCorpus
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import java.io.File
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Editor evaluation (T03), run on demand with `-PeditorEvaluation`. It does not assert: it
 * records how each option round-trips the markdown corpus and writes a report for SPEC.md.
 */
@Tag("evaluation")
class RichEditorRoundTripEvaluation {

    @Test
    fun `writes the round-trip report for both editor options`() {
        val rows = MarkdownCorpus.names.map { name ->
            val source = MarkdownCorpus.read(name)
            val live = MarkdownDocument.parse(source).toMarkdown()
            val rich = runCatching { RichTextState().apply { setMarkdown(source) }.toMarkdown() }
                .getOrElse { "<error: ${it::class.simpleName}: ${it.message}>" }
            Row(name, source, live == source, rich)
        }
        val report = buildString {
            appendLine("# Editor round-trip evaluation")
            appendLine()
            appendLine("| Corpus file | A: live markdown | B: compose-rich-editor |")
            appendLine("|---|---|---|")
            rows.forEach {
                appendLine(
                    "| ${it.name} | ${verdict(it.liveIdentical)} | ${verdict(it.richIdentical)} |"
                )
            }
            appendLine()
            rows.filterNot { it.richIdentical }.forEach { row ->
                appendLine("## ${row.name}: first difference in B")
                appendLine()
                appendLine(firstDifference(row.source, row.rich))
                appendLine()
            }
        }
        val file = File("build/reports/editor-evaluation/report.md")
        file.parentFile?.mkdirs()
        file.writeText(report)
        println(report)
    }

    private data class Row(
        val name: String,
        val source: String,
        val liveIdentical: Boolean,
        val rich: String
    ) {
        val richIdentical: Boolean get() = rich == source
    }

    private fun verdict(identical: Boolean) = if (identical) "identical" else "**changed**"

    private fun firstDifference(expected: String, actual: String): String {
        val expectedLines = expected.lines()
        val actualLines = actual.lines()
        val index =
            expectedLines.indices.firstOrNull {
                it >= actualLines.size ||
                    expectedLines[it] != actualLines[it]
            }
                ?: expectedLines.size
        val before =
            expectedLines.getOrNull(index)?.let { "`${it.replace("|", "\\|")}`" } ?: "(end of file)"
        val after =
            actualLines.getOrNull(index)?.let { "`${it.replace("|", "\\|")}`" } ?: "(end of file)"
        return "- line ${index + 1}\n- original: $before\n- after B: $after"
    }
}
