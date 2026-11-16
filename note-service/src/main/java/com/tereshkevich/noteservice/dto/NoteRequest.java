package com.tereshkevich.noteservice.dto;

import com.tereshkevich.noteservice.util.NoteMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = NoteMessages.TITLE_REQUIRED)
        @Size(max = 255, message = NoteMessages.TITLE_TOO_LONG)
        String title,

        @Size(max = 10_000, message = NoteMessages.CONTENT_TOO_LONG)
        String content
) {
}
