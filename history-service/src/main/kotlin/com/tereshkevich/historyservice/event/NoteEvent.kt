package com.tereshkevich.historyservice.event

import java.time.Instant
import java.util.*

data class NoteEvent(
    val eventType: EventType,
    val noteId: UUID,
    val timestamp: Instant,
) {
    enum class EventType {
        CREATED,
        UPDATED,
        DELETED,
    }
}
