package com.tereshkevich.historyservice.mapper

import com.tereshkevich.historyservice.domain.ActivityLog
import com.tereshkevich.historyservice.dto.ActivityLogResponse

fun ActivityLog.toResponse(): ActivityLogResponse = ActivityLogResponse(
    id = id,
    eventType = eventType,
    noteId = noteId,
    noteTitle = noteTitle,
    eventTimestamp = eventTimestamp,
    serviceName = serviceName,
)
