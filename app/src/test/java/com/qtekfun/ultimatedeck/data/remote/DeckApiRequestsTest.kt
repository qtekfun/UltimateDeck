// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.CreateCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.LabelIdRequest
import com.qtekfun.ultimatedeck.data.remote.dto.ReorderCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UpdateCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UserIdRequest
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.junit5.StartStop
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeckApiRequestsTest {
    @StartStop
    val server = MockWebServer()

    private val api by lazy { testDeckApi(server) }
    private val card = "${API_PATH}boards/1/stacks/2/cards/3"

    private fun taken(): RecordedRequest = server.takeRequest()

    private fun RecordedRequest.bodyText() = body?.utf8().orEmpty()

    @Test
    fun `fetches boards with details and conditional headers`() = runTest {
        server.enqueue(json("[]"))

        apiCall {
            api.boards.getBoards(etag = "\"e1\"", modifiedSince = "Mon, 05 Nov 2018 09:28:00 GMT")
        }

        val request = taken()
        assertEquals("GET", request.method)
        assertEquals("${API_PATH}boards?details=true", request.target)
        assertEquals("\"e1\"", request.headers["If-None-Match"])
        assertEquals("Mon, 05 Nov 2018 09:28:00 GMT", request.headers["If-Modified-Since"])
    }

    @Test
    fun `omits conditional headers when there is nothing to compare`() = runTest {
        server.enqueue(json("[]"))

        apiCall { api.boards.getStacks(boardId = 1) }

        val request = taken()
        assertEquals("${API_PATH}boards/1/stacks", request.target)
        assertEquals(null, request.headers["If-None-Match"])
        assertEquals(null, request.headers["If-Modified-Since"])
    }

    @Test
    fun `fetches a single board and card`() = runTest {
        server.enqueue(json("""{"id": 1, "title": "Board", "color": "0082c9"}"""))
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        val board = apiCall { api.boards.getBoard(1) }
        val fetched = apiCall { api.cards.getCard(1, 2, 3) }

        assertEquals("${API_PATH}boards/1", taken().target)
        assertEquals(card, taken().target)
        assertEquals("Board", (board as ApiResult.Success).value.title)
        assertEquals("Test", (fetched as ApiResult.Success).value.title)
    }

    @Test
    fun `creates a card in a column`() = runTest {
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        val result =
            apiCall { api.cards.createCard(1, 2, CreateCardRequest(title = "New", order = 5)) }

        val request = taken()
        assertEquals("POST", request.method)
        assertEquals("${API_PATH}boards/1/stacks/2/cards", request.target)
        assertEquals("""{"title":"New","order":5,"type":"plain"}""", request.bodyText())
        assertEquals(10L, (result as ApiResult.Success).value.id)
    }

    @Test
    fun `updates a card sending its whole editable state`() = runTest {
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        apiCall {
            api.cards.updateCard(
                1,
                2,
                3,
                UpdateCardRequest(
                    title = "T",
                    owner = "admin",
                    order = 1,
                    description = "D",
                    duedate = "2026-10-04T10:00:00+00:00"
                )
            )
        }

        val request = taken()
        assertEquals("PUT", request.method)
        assertEquals(card, request.target)
        assertEquals(
            """{"title":"T","owner":"admin","order":1,"description":"D","type":"plain",""" +
                """"duedate":"2026-10-04T10:00:00+00:00","archived":false}""",
            request.bodyText()
        )
    }

    @Test
    fun `moves, archives, unarchives and deletes cards`() = runTest {
        server.enqueue(json("[]"))
        repeat(2) { server.enqueue(json(ApiFixtures.read("card_created.json"))) }
        server.enqueue(MockResponse(200))

        apiCall { api.cards.reorderCard(1, 2, 3, ReorderCardRequest(order = 4, stackId = 9)) }
        apiCall { api.cards.archiveCard(1, 2, 3) }
        apiCall { api.cards.unarchiveCard(1, 2, 3) }
        val deleted = apiCall { api.cards.deleteCard(1, 2, 3) }

        taken().let {
            assertEquals("PUT $card/reorder", "${it.method} ${it.target}")
            assertEquals("""{"order":4,"stackId":9}""", it.bodyText())
        }
        assertEquals("PUT $card/archive", taken().let { "${it.method} ${it.target}" })
        assertEquals("PUT $card/unarchive", taken().let { "${it.method} ${it.target}" })
        assertEquals("DELETE $card", taken().let { "${it.method} ${it.target}" })
        assertEquals(ApiResult.Success(Unit), deleted)
    }

    @Test
    fun `assigns and removes labels and users`() = runTest {
        repeat(4) { server.enqueue(MockResponse(200)) }

        apiCall { api.cardMetadata.assignLabel(1, 2, 3, LabelIdRequest(37)) }
        apiCall { api.cardMetadata.removeLabel(1, 2, 3, LabelIdRequest(37)) }
        apiCall { api.cardMetadata.assignUser(1, 2, 3, UserIdRequest("ana")) }
        apiCall { api.cardMetadata.unassignUser(1, 2, 3, UserIdRequest("ana")) }

        assertEquals(
            "$card/assignLabel" to """{"labelId":37}""",
            taken().let {
                it.target to
                    it.bodyText()
            }
        )
        assertEquals(
            "$card/removeLabel" to """{"labelId":37}""",
            taken().let {
                it.target to
                    it.bodyText()
            }
        )
        assertEquals(
            "$card/assignUser" to """{"userId":"ana"}""",
            taken().let {
                it.target to
                    it.bodyText()
            }
        )
        assertEquals(
            "$card/unassignUser" to """{"userId":"ana"}""",
            taken().let {
                it.target to
                    it.bodyText()
            }
        )
    }

    @Test
    fun `lists, uploads, downloads and deletes attachments`() = runTest {
        server.enqueue(json(ApiFixtures.read("attachments.json")))
        server.enqueue(
            json(ApiFixtures.read("attachments.json").trim().removePrefix("[").removeSuffix("]"))
        )
        server.enqueue(MockResponse(200, okhttp3.Headers.headersOf(), "PNGDATA"))
        server.enqueue(MockResponse(200))

        apiCall { api.attachments.getAttachments(1, 2, 3) }
        apiCall {
            api.attachments.upload(
                1,
                2,
                3,
                type = "file".toRequestBody(),
                file = MultipartBody.Part.createFormData(
                    "file",
                    "photo.png",
                    "bytes".toRequestBody("image/png".toMediaType())
                )
            )
        }
        val download = apiCall { api.attachments.download(1, 2, 3, "file", 7) }
        apiCall { api.attachments.delete(1, 2, 3, "file", 7) }

        assertEquals("$card/attachments", taken().target)
        taken().let { upload ->
            assertEquals("POST", upload.method)
            assertTrue(upload.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
            assertTrue(upload.bodyText().contains("name=\"type\""))
            assertTrue(upload.bodyText().contains("filename=\"photo.png\""))
        }
        assertEquals("$card/attachments/file/7", taken().target)
        assertEquals("PNGDATA", (download as ApiResult.Success).value.string())
        assertEquals("DELETE $card/attachments/file/7", taken().let { "${it.method} ${it.target}" })
    }
}
