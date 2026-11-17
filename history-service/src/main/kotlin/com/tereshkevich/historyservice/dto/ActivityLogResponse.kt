package com.tereshkevich.historyservice.dto

import com.tereshkevich.historyservice.event.NoteEvent
import java.time.Instant
import java.util.*

data class ActivityLogResponse(
    val id: String?,
    val eventType: NoteEvent.EventType,
    val noteId: UUID,
    val noteTitle: String?,
    val eventTimestamp: Instant,
    val serviceName: String,
)
