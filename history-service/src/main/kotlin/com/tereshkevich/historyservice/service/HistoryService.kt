package com.tereshkevich.historyservice.service

import com.tereshkevich.historyservice.client.NotesClient
import com.tereshkevich.historyservice.domain.ActivityLog
import com.tereshkevich.historyservice.event.NoteEvent
import com.tereshkevich.historyservice.repository.ActivityLogRepository
import com.tereshkevich.historyservice.util.LogMessages
import com.tereshkevich.historyservice.util.ServiceInfo
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.*

@Service
class HistoryService(
    private val repository: ActivityLogRepository,
    private val notesClient: NotesClient,
) {

    private val logger = LoggerFactory.getLogger(HistoryService::class.java)

    private companion object {
        const val EVENT_TIMESTAMP = "eventTimestamp"
    }

    fun record(event: NoteEvent): ActivityLog {
        val entry = ActivityLog(
            eventType = event.eventType,
            noteId = event.noteId,
            noteTitle = fetchTitle(event.noteId),
            eventTimestamp = event.timestamp,
            serviceName = ServiceInfo.NAME,
        )
        val saved = repository.save(entry)
        logger.info(LogMessages.LOG_SAVED, saved.id, saved.eventType, saved.noteId)
        return saved
    }

    fun findAll(noteId: UUID?): List<ActivityLog> {
        val records = if (noteId == null) {
            repository.findAll(Sort.by(Sort.Direction.DESC, EVENT_TIMESTAMP))
        } else {
            repository.findByNoteIdOrderByEventTimestampDesc(noteId)
        }
        logger.debug(LogMessages.HISTORY_FETCHED, records.size, noteId)
        return records
    }

    private fun fetchTitle(noteId: UUID): String? =
        try {
            notesClient.getNote(noteId).title
                .also { logger.debug(LogMessages.NOTE_TITLE_FETCHED, it, noteId) }
        } catch (ex: FeignException.NotFound) {
            logger.warn(LogMessages.NOTE_NOT_FOUND, noteId, ex)
            null
        } catch (ex: FeignException) {
            logger.error(LogMessages.NOTE_FETCH_FAILED, noteId, ex)
            null
        }
}
