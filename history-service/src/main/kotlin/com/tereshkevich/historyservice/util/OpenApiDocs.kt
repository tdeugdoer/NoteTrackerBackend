package com.tereshkevich.historyservice.util

object OpenApiDocs {

    const val TITLE = "NoteTracker · History Service"
    const val DESCRIPTION =
        "Журнал действий над заметками: события из Kafka, обогащение заголовком через notes-service."
    const val VERSION = "0.0.1"

    const val HISTORY_TAG = "History"
    const val HISTORY_TAG_DESCRIPTION = "Просмотр журнала действий"

    const val GET_HISTORY_SUMMARY = "Получить журнал действий (опционально — по noteId)"
}
