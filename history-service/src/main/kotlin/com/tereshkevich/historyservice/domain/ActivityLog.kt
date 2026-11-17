package com.tereshkevich.historyservice.domain

import com.tereshkevich.historyservice.event.NoteEvent
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant
import java.util.*

@Document(collection = "activity_logs")
data class ActivityLog(
    @Id
    val id: String? = null,
    @Field("eventType")
    val eventType: NoteEvent.EventType,
    @Field("noteId")
    val noteId: UUID,
    @Field("noteTitle")
    val noteTitle: String? = null,
    @Field("eventTimestamp")
    val eventTimestamp: Instant,
    @Field("serviceName")
    val serviceName: String,
) 
