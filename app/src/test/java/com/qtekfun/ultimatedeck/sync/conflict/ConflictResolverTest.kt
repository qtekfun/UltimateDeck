// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.conflict

import com.qtekfun.ultimatedeck.data.local.model.CardField
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConflictResolverTest {
    private val t0 = Instant.parse("2026-10-01T10:00:00Z")
    private val t1 = Instant.parse("2026-10-01T11:00:00Z")
    private val t2 = Instant.parse("2026-10-01T12:00:00Z")

    private val base = CardState(
        title = "Title",
        description = "Desc",
        dueDate = null,
        stackId = 1,
        order = 0,
        archived = false,
        done = null,
        labelIds = setOf(1),
        assigneeUids = setOf("ana")
    )

    private fun local(state: CardState, vararg dirty: CardField, at: Instant? = t1) =
        LocalCard(state, dirty.toSet(), at)

    private fun server(state: CardState, at: Instant? = t1) = ServerCard(state, at)

    private fun merged(local: LocalCard, base: CardState?, server: ServerCard) =
        ConflictResolver.resolve(local, base, server) as Resolution.Merged

    @Test
    fun `fields not changed locally take the server value`() {
        val remote = base.copy(title = "Server", dueDate = t0, labelIds = setOf(2))

        val result = merged(local(base), base, server(remote))

        assertEquals(Resolution.Merged(remote, emptySet(), emptyList(), remote), result)
    }

    @Test
    fun `every field changed only locally wins and is sent`() {
        val edited = CardState(
            title = "Mine",
            description = "My desc",
            dueDate = t2,
            stackId = 2,
            order = 5,
            archived = true,
            done = t2,
            labelIds = setOf(3),
            assigneeUids = setOf("bob")
        )

        val result = merged(local(edited, *CardField.entries.toTypedArray()), base, server(base))

        assertEquals(edited, result.state)
        assertEquals(CardField.entries.toSet(), result.toSend)
        assertEquals(emptyList<TextConflict>(), result.conflicts)
        assertEquals(base, result.snapshot)
    }

    @Test
    fun `the same change on both sides needs nothing more`() {
        val both = base.copy(title = "Same", dueDate = t2, assigneeUids = setOf("bob"))
        val dirty = arrayOf(CardField.TITLE, CardField.DUE_DATE, CardField.ASSIGNEES)

        val result = merged(local(both, *dirty), base, server(both))

        assertEquals(Resolution.Merged(both, emptySet(), emptyList(), both), result)
    }

    @Test
    fun `title and description changed differently on both sides become conflicts`() {
        val mine = base.copy(title = "Mine", description = "My desc")
        val theirs = base.copy(title = "Theirs", description = "Their desc")

        val result =
            merged(
                local(mine, CardField.TITLE, CardField.DESCRIPTION, at = t2),
                base,
                server(theirs)
            )

        assertEquals(mine, result.state)
        assertEquals(emptySet<CardField>(), result.toSend)
        assertEquals(
            listOf(
                TextConflict(CardField.TITLE, "Mine", "Theirs"),
                TextConflict(CardField.DESCRIPTION, "My desc", "Their desc")
            ),
            result.conflicts
        )
    }

    @Test
    fun `scalar and set fields changed on both sides keep the latest change`() {
        val mine = base.copy(dueDate = t2, labelIds = setOf(5))
        val theirs = base.copy(dueDate = t0, labelIds = setOf(6))
        val dirty = arrayOf(CardField.DUE_DATE, CardField.LABELS)

        val localNewer = merged(local(mine, *dirty, at = t2), base, server(theirs, at = t1))
        val serverNewer = merged(local(mine, *dirty, at = t0), base, server(theirs, at = t1))

        assertEquals(Resolution.Merged(mine, dirty.toSet(), emptyList(), theirs), localNewer)
        assertEquals(Resolution.Merged(theirs, emptySet(), emptyList(), theirs), serverNewer)
    }

    @Test
    fun `a tie or an unknown time goes to the server`() {
        val mine = base.copy(archived = true)
        val theirs = base.copy(done = t0, archived = false, order = 9)
        val changed = base.copy(archived = true, order = 3)
        val cases = listOf(
            local(changed, CardField.POSITION, at = t1) to server(theirs, at = t1),
            local(changed, CardField.POSITION, at = null) to server(theirs, at = t1),
            local(changed, CardField.POSITION, at = t2) to server(theirs, at = null)
        )

        cases.forEach { (mineCard, theirsCard) ->
            assertEquals(theirs, merged(mineCard, base.copy(order = 1), theirsCard).state)
        }
        assertEquals(mine, merged(local(mine, CardField.ARCHIVED), base, server(base)).state)
    }

    @Test
    fun `without a base every server value counts as changed`() {
        val mine = base.copy(title = "Mine", stackId = 7)
        val theirs = base.copy(title = "Theirs", stackId = 8)

        val result =
            merged(
                local(mine, CardField.TITLE, CardField.POSITION, at = t2),
                null,
                server(theirs, at = t1)
            )

        assertEquals(mine, result.state)
        assertEquals(setOf(CardField.POSITION), result.toSend)
        assertEquals(listOf(TextConflict(CardField.TITLE, "Mine", "Theirs")), result.conflicts)
    }

    @Test
    fun `combines a text conflict, a newer local date and server labels`() {
        val mine = base.copy(title = "Mine", dueDate = t2)
        val theirs = base.copy(title = "Theirs", dueDate = t0, labelIds = setOf(9))

        val result =
            merged(local(mine, CardField.TITLE, CardField.DUE_DATE, at = t2), base, server(theirs))

        assertEquals(theirs.copy(title = "Mine", dueDate = t2), result.state)
        assertEquals(setOf(CardField.DUE_DATE), result.toSend)
        assertEquals(listOf(TextConflict(CardField.TITLE, "Mine", "Theirs")), result.conflicts)
        assertEquals(theirs, result.snapshot)
    }

    @Test
    fun `a card deleted on the server is dropped unless it was edited here`() {
        val edited = base.copy(title = "Mine")

        assertEquals(Resolution.RemoveLocally, ConflictResolver.resolve(local(base), base, null))
        assertEquals(
            Resolution.DeletedOnServer(edited),
            ConflictResolver.resolve(local(edited, CardField.TITLE), base, null)
        )
    }

    @Test
    fun `repeating an interrupted resolution reaches the same decision`() {
        val input = Triple(
            local(
                base.copy(title = "Mine", order = 4),
                CardField.TITLE,
                CardField.POSITION,
                at = t2
            ),
            base,
            server(base.copy(title = "Theirs", order = 2))
        )

        val first = ConflictResolver.resolve(input.first, input.second, input.third)
        val again = ConflictResolver.resolve(input.first, input.second, input.third)

        assertEquals(first, again)
    }
}
