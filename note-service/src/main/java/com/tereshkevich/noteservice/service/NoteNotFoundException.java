package com.tereshkevich.noteservice.service;

import com.tereshkevich.noteservice.util.NoteMessages;

import java.util.UUID;

public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(UUID id) {
        super(NoteMessages.NOT_FOUND.formatted(id));
    }

}
