package com.tereshkevich.noteservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class NoteMessages {

    public final String NOT_FOUND = "Note with id %s not found";
    public final String NOT_FOUND_TITLE = "Note not found";

    public final String TITLE_REQUIRED = "Title must not be blank";
    public final String TITLE_TOO_LONG = "Title must be at most 255 characters";
    public final String CONTENT_TOO_LONG = "Content must be at most 10000 characters";

}
