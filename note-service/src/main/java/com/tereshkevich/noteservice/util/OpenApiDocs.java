package com.tereshkevich.noteservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OpenApiDocs {

    public final String TITLE = "NoteTracker · Notes Service";
    public final String DESCRIPTION = """
            REST API для управления заметками.
            Каждое изменение публикует событие в Kafka (топик note-events).""";
    public final String VERSION = "0.0.1";

    public final String NOTES_TAG = "Notes";
    public final String NOTES_TAG_DESCRIPTION = "Создание, чтение, обновление и удаление заметок";

    public final String CREATE_SUMMARY = "Создать заметку";
    public final String GET_SUMMARY = "Получить заметку по id";
    public final String FIND_ALL_SUMMARY = "Список всех заметок";
    public final String UPDATE_SUMMARY = "Обновить заметку";
    public final String DELETE_SUMMARY = "Удалить заметку";

}
