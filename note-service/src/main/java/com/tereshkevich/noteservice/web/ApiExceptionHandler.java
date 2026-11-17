package com.tereshkevich.noteservice.web;

import com.tereshkevich.noteservice.service.NoteNotFoundException;
import com.tereshkevich.noteservice.util.LogMessages;
import com.tereshkevich.noteservice.util.NoteMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoteNotFoundException.class)
    ProblemDetail handleNotFound(NoteNotFoundException ex) {
        log.warn(LogMessages.NOTE_NOT_FOUND, ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle(NoteMessages.NOT_FOUND_TITLE);
        return problem;
    }

}
