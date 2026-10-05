// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.qtekfun.ultimatedeck.data.local.entity.ShownReminderEntity
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Opens a database created with each old exported schema and lets Room migrate and validate it. */
class MigrationTest {
    @TempDir
    lateinit var dir: File

    private val schemas = File("schemas/${UltimateDeckDatabase::class.qualifiedName}")

    /** Creates [file] exactly as version [version] of the exported schema describes it. */
    private fun createFromSchema(file: File, version: Int, extraSql: List<String> = emptyList()) {
        val schema = Json.parseToJsonElement(
            File(schemas, "$version.json").readText()
        ).jsonObject["database"]!!.jsonObject
        val statements = schema["entities"]!!.jsonArray.flatMap { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val create = entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace(
                "\${TABLE_NAME}",
                table
            )
            val indices = entity.jsonObject["indices"]?.jsonArray.orEmpty().map {
                it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)
            }
            listOf(create) + indices
        } + schema["setupQueries"]!!.jsonArray.map { it.jsonPrimitive.content }
        val connection = BundledSQLiteDriver().open(file.path)
        (statements + extraSql + "PRAGMA user_version = $version").forEach(connection::execSQL)
        connection.close()
    }

    private fun open(file: File): UltimateDeckDatabase {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.getDatabasePath(any()) } returns file
        return Room.databaseBuilder<UltimateDeckDatabase>(context, file.name)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*UltimateDeckDatabase.MIGRATIONS)
            .build()
    }

    @Test
    fun `migrates version 1 to the latest and keeps queued operations`() = runTest {
        val file = File(dir, "v1.db")
        createFromSchema(
            file,
            version = 1,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO pending_operation (id, accountId, type, entityType, entityId, " +
                    "payload, createdAt, attempts, nextAttemptAt, lastError) " +
                    "VALUES (1, 1, 'MOVE', 'CARD', 100, '{}', 0, 0, 0, NULL)"
            )
        )

        val db = open(file)
        val operation = db.pendingOperationDao().all(1).single()
        db.close()

        assertEquals(100L, operation.entityId)
        assertFalse(operation.failed)
    }

    @Test
    fun `migrates version 2 to the latest and keeps cards`() = runTest {
        val file = File(dir, "v2.db")
        createFromSchema(
            file,
            version = 2,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO board (accountId, id, title, color, archived) " +
                    "VALUES (1, 1, 'B', 'fff', 0)",
                "INSERT INTO stack (accountId, id, boardId, title, `order`) VALUES (1, 10, 1, 'S', 0)",
                "INSERT INTO card (accountId, id, boardId, stackId, title, description, " +
                    "`order`, archived, dirtyFields) VALUES (1, 100, 1, 10, 'Card', '', 0, 0, 1)"
            )
        )

        val db = open(file)
        val card = db.cardDao().get(1, 100)
        db.close()

        assertEquals("Card", card?.title)
        assertEquals(null, card?.localModifiedAt)
    }

    @Test
    fun `migrates version 3 to the latest with empty sync state`() = runTest {
        val file = File(dir, "v3.db")
        createFromSchema(
            file,
            version = 3,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO board (accountId, id, title, color, archived) " +
                    "VALUES (1, 1, 'B', 'fff', 0)",
                "INSERT INTO stack (accountId, id, boardId, title, `order`) VALUES (1, 10, 1, 'S', 0)",
                "INSERT INTO card (accountId, id, boardId, stackId, title, description, " +
                    "`order`, archived, dirtyFields) VALUES (1, 100, 1, 10, 'Card', '', 0, 0, 0)"
            )
        )

        val db = open(file)
        val account = db.accountDao().get(1)
        val board = db.boardDao().get(1, 1)
        val card = db.cardDao().get(1, 100)
        db.close()

        assertEquals(null, account?.boardsEtag)
        assertEquals(null, board?.stacksEtag)
        assertFalse(card!!.deletedOnServer)
    }

    @Test
    fun `migrates version 4 to the latest without attachments counted`() = runTest {
        val file = File(dir, "v4.db")
        createFromSchema(
            file,
            version = 4,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO board (accountId, id, title, color, archived) " +
                    "VALUES (1, 1, 'B', 'fff', 0)",
                "INSERT INTO stack (accountId, id, boardId, title, `order`) VALUES (1, 10, 1, 'S', 0)",
                "INSERT INTO card (accountId, id, boardId, stackId, title, description, " +
                    "`order`, archived, dirtyFields, deletedOnServer) " +
                    "VALUES (1, 100, 1, 10, 'Card', '', 0, 0, 0, 0)"
            )
        )

        val db = open(file)
        val card = db.cardDao().get(1, 100)
        db.close()

        assertEquals(0, card?.attachmentCount)
    }

    @Test
    fun `migrates version 5 to the latest without conflicts`() = runTest {
        val file = File(dir, "v5.db")
        createFromSchema(
            file,
            version = 5,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO board (accountId, id, title, color, archived) " +
                    "VALUES (1, 1, 'B', 'fff', 0)",
                "INSERT INTO stack (accountId, id, boardId, title, `order`) VALUES (1, 10, 1, 'S', 0)",
                "INSERT INTO card (accountId, id, boardId, stackId, title, description, " +
                    "`order`, archived, dirtyFields, deletedOnServer, attachmentCount) " +
                    "VALUES (1, 100, 1, 10, 'Card', '', 0, 0, 0, 0, 0)"
            )
        )

        val db = open(file)
        val card = db.cardDao().get(1, 100)
        db.close()

        assertEquals(0, card?.conflictFields)
    }

    @Test
    fun `migrates version 6 to the latest with no board members yet`() = runTest {
        val file = File(dir, "v6.db")
        createFromSchema(
            file,
            version = 6,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO board (accountId, id, title, color, archived) " +
                    "VALUES (1, 1, 'B', 'fff', 0)",
                "INSERT INTO deck_user (accountId, uid, displayName) VALUES (1, 'ana', 'Ana')"
            )
        )

        val db = open(file)
        db.boardMemberDao().setMembers(1, 1, listOf("ana"))
        val members = db.boardMemberDao().observeMembers(1, 1).first()
        db.close()

        assertEquals(listOf("ana"), members.map { it.uid })
    }

    @Test
    fun `migrates version 7 to the latest with queued operations not started`() = runTest {
        val file = File(dir, "v7.db")
        createFromSchema(
            file,
            version = 7,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO pending_operation (id, accountId, type, entityType, entityId, " +
                    "payload, createdAt, attempts, nextAttemptAt, lastError, failed) " +
                    "VALUES (1, 1, 'MOVE', 'CARD', 100, '{}', 0, 0, 0, NULL, 0), " +
                    "(2, 1, 'MOVE', 'CARD', 101, '{}', 5000, 2, 0, NULL, 0)"
            )
        )

        val db = open(file)
        val operations = db.pendingOperationDao().all(1)
        db.close()

        assertEquals(listOf(null, Instant.ofEpochMilli(5000)), operations.map { it.startedAt })
    }

    @Test
    fun `migrates version 8 to the latest with attachments of type file`() = runTest {
        val file = File(dir, "v8.db")
        createFromSchema(
            file,
            version = 8,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')",
                "INSERT INTO board (accountId, id, title, color, archived) " +
                    "VALUES (1, 1, 'B', 'fff', 0)",
                "INSERT INTO stack (accountId, id, boardId, title, `order`) VALUES (1, 10, 1, 'S', 0)",
                "INSERT INTO card (accountId, id, boardId, stackId, title, description, `order`, " +
                    "archived, dirtyFields, deletedOnServer, attachmentCount, conflictFields) " +
                    "VALUES (1, 100, 1, 10, 'Card', '', 0, 0, 0, 0, 0, 0)",
                "INSERT INTO attachment (accountId, id, cardId, fileName, size, uploadState) " +
                    "VALUES (1, 7, 100, 'a.png', 3, 'DONE')"
            )
        )

        val db = open(file)
        val attachment = db.attachmentDao().observeForCard(1, 100).first().single()
        db.close()

        assertEquals("file", attachment.type)
    }

    @Test
    fun `migrates version 9 to the latest with a table for shown reminders`() = runTest {
        val file = File(dir, "v9.db")
        createFromSchema(
            file,
            version = 9,
            extraSql = listOf(
                "INSERT INTO account (id, serverUrl, userId, displayName) " +
                    "VALUES (1, 'https://c.example/', 'ana', 'Ana')"
            )
        )

        val db = open(file)
        val at = Instant.ofEpochMilli(5000)
        db.shownReminderDao().insert(ShownReminderEntity(1, 100, at))
        val shown = db.shownReminderDao().all(1)
        db.close()

        assertEquals(listOf(ShownReminderEntity(1, 100, at)), shown)
    }
}
