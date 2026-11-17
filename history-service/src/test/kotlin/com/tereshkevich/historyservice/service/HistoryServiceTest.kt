package com.tereshkevich.historyservice.service

import com.tereshkevich.historyservice.client.NotesClient
import com.tereshkevich.historyservice.domain.ActivityLog
import com.tereshkevich.historyservice.dto.NoteResponse
import com.tereshkevich.historyservice.event.NoteEvent
import com.tereshkevich.historyservice.event.NoteEvent.EventType
import com.tereshkevich.historyservice.repository.ActivityLogRepository
import com.tereshkevich.historyservice.util.ServiceInfo
import feign.FeignException
import feign.Request
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.*

private val noteId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

// Настоящий FeignException вместо mockk: сервис логирует исключение, и логбэк
// вызывает у него getMessage() — на строгом моке это падает с MockKException.
@Suppress("DEPRECATION")
private fun noteNotFound() = FeignException.NotFound(
    "note not found",
    Request.create(
        Request.HttpMethod.GET,
        "http://localhost/api/notes/$noteId",
        emptyMap(),
        null,
        Charsets.UTF_8,
    ),
    null,
    emptyMap(),
)

class HistoryServiceTest : FunSpec({

    lateinit var repository: ActivityLogRepository
    lateinit var notesClient: NotesClient
    lateinit var service: HistoryService

    beforeTest {
        repository = mockk()
        notesClient = mockk()
        service = HistoryService(repository, notesClient)
    }

    context("record") {

        test("saves log enriched with the note title") {
            val event = NoteEvent(EventType.CREATED, noteId, Instant.now())
            every { notesClient.getNote(noteId) } returns
                    NoteResponse(noteId, "Title", "Content", Instant.now())
            every { repository.save(any()) } answers { firstArg() }

            val saved = service.record(event)

            saved.noteTitle shouldBe "Title"
            saved.noteId shouldBe noteId
            saved.eventType shouldBe EventType.CREATED
            saved.serviceName shouldBe ServiceInfo.NAME
            verify(exactly = 1) { repository.save(any()) }
        }

        test("saves log without title when the note is already deleted") {
            val event = NoteEvent(EventType.DELETED, noteId, Instant.now())
            every { notesClient.getNote(noteId) } throws noteNotFound()
            every { repository.save(any()) } answers { firstArg() }

            val saved = service.record(event)

            saved.noteTitle.shouldBeNull()
            saved.eventType shouldBe EventType.DELETED
            saved.noteId shouldBe noteId
            verify(exactly = 1) { repository.save(any()) }
        }
    }

    context("findAll") {

        test("returns every record sorted by event time") {
            val expected = listOf(activityLog(EventType.UPDATED))
            every { repository.findAll(any<Sort>()) } returns expected

            service.findAll(null) shouldContainExactly expected
        }

        test("filters records by note id") {
            val expected = listOf(activityLog(EventType.CREATED))
            every { repository.findByNoteIdOrderByEventTimestampDesc(noteId) } returns expected

            service.findAll(noteId) shouldContainExactly expected
        }
    }
})

private fun activityLog(eventType: EventType) = ActivityLog(
    id = "log-id",
    eventType = eventType,
    noteId = noteId,
    noteTitle = "Title",
    eventTimestamp = Instant.now(),
    serviceName = ServiceInfo.NAME,
)
