package com.tereshkevich.noteservice.event;

import java.time.Instant;
import java.util.UUID;

public record NoteEvent(EventType eventType, UUID noteId, Instant timestamp) {

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
