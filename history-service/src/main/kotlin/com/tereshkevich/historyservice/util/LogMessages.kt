package com.tereshkevich.historyservice.util

object LogMessages {

    const val EVENT_RECEIVED = "Received {} event for note {}"
    const val LOG_SAVED = "Saved activity log {}: {} for note {}"
    const val NOTE_TITLE_FETCHED = "Fetched title '{}' for note {}"
    const val NOTE_NOT_FOUND = "Note {} is already deleted, title is not available"
    const val NOTE_FETCH_FAILED = "Failed to fetch note {} from notes-service"
    const val HISTORY_FETCHED = "Fetched {} activity log records (noteId={})"
}
