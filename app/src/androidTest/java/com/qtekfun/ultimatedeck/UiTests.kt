// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import com.qtekfun.ultimatedeck.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ACCOUNT = 1L
private const val BOARD = 1L
private const val TODO = 10L
private const val DONE = 11L

/**
 * Key flows on a real device (T20, SPEC §7): login, moving a card and editing a card offline.
 * The server is never reached: the board is seeded in an in-memory database and every change
 * must land in Room and in the sync queue.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UiTests {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject
    lateinit var database: UltimateDeckDatabase

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hilt.inject()
        context.getSharedPreferences(
            "ui-test-settings",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** A signed-in account with a board of two columns and one card, as after a sync. */
    private fun seedBoard() = runBlocking {
        database.accountDao().insert(AccountEntity(ACCOUNT, "https://cloud.example/", "ana", "Ana"))
        database.boardDao().upsert(listOf(BoardEntity(ACCOUNT, BOARD, "Team board", "0082c9")))
        database.stackDao().upsert(
            listOf(
                StackEntity(ACCOUNT, TODO, BOARD, "To do", 0),
                StackEntity(ACCOUNT, DONE, BOARD, "Done", 1)
            )
        )
        database.cardDao().upsert(
            listOf(
                CardEntity(
                    accountId = ACCOUNT,
                    id = 5,
                    boardId = BOARD,
                    stackId = TODO,
                    title = "Write tests"
                )
            )
        )
    }

    private fun queued() = runBlocking {
        database.pendingOperationDao().all(ACCOUNT).map {
            it.entityId to
                QueuedOperation.decode(it.payload)
        }
    }

    private fun card() = runBlocking { database.cardDao().get(ACCOUNT, 5)!! }

    private fun openBoard() {
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText("Team board").fetchSemanticsNodes().isNotEmpty()
        }
        // The side menu lists the board too, even closed (off screen, as a tab): open it from the list.
        compose.onNode(
            hasText("Team board") and
                !SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )
            .performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText("Write tests").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun loginRefusesAnUnencryptedServer() {
        launch()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(
                text(R.string.login_button)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(hasSetTextAction()).performTextInput("http://cloud.example")
        compose.onNodeWithText(text(R.string.login_button)).performClick()

        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(
                text(R.string.login_error_insecure)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun movingACardOfflineSavesItAndQueuesIt() {
        seedBoard()
        launch()
        openBoard()

        compose.onNodeWithText("Write tests").performCustomAccessibilityActionWithLabel(
            text(R.string.board_action_move_to, "Done")
        )

        compose.waitUntil(TIMEOUT) { card().stackId == DONE }
        assertEquals(setOf(CardField.POSITION), CardField.fromMask(card().dirtyFields))
        assertEquals(listOf(5L to QueuedOperation.MoveCard(BOARD, TODO, DONE, 0)), queued())
    }

    @Test
    fun editingACardOfflineSavesTitleAndDescription() {
        seedBoard()
        launch()
        openBoard()
        compose.onNodeWithText("Write tests").performClick()

        compose.onNode(
            hasSetTextAction() and hasText("Write tests")
        ).performTextReplacement("Write UI tests")
        compose.onNodeWithText(text(R.string.editor_empty)).performClick()
        // The description editor: the text field that is not the title.
        compose.onNode(hasSetTextAction() and !hasText("Write UI tests"))
            .performTextInput("Login, move and edit")
        // Let Compose apply the typing before leaving, as a person would.
        compose.waitForIdle()
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.waitUntil(TIMEOUT) { card().title == "Write UI tests" }
        compose.waitUntil(TIMEOUT) { card().description.isNotEmpty() }
        assertEquals("Login, move and edit", card().description.trim())
        assertEquals(
            setOf(CardField.TITLE, CardField.DESCRIPTION),
            CardField.fromMask(card().dirtyFields)
        )
        assertEquals(listOf(5L to QueuedOperation.UpdateCard(BOARD, TODO)), queued())
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
