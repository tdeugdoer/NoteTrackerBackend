package com.tereshkevich.historyservice.event

import com.tereshkevich.historyservice.service.HistoryService
import com.tereshkevich.historyservice.util.KafkaConstants
import com.tereshkevich.historyservice.util.LogMessages
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class NoteEventListener(
    private val historyService: HistoryService,
) {

    private val logger = LoggerFactory.getLogger(NoteEventListener::class.java)

    @KafkaListener(topics = [KafkaConstants.NOTE_EVENTS_TOPIC])
    fun onNoteEvent(event: NoteEvent) {
        logger.info(LogMessages.EVENT_RECEIVED, event.eventType, event.noteId)
        historyService.record(event)
    }
}
