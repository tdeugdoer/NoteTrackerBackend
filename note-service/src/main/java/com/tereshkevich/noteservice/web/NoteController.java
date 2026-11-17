package com.tereshkevich.noteservice.web;

import com.tereshkevich.noteservice.dto.NoteRequest;
import com.tereshkevich.noteservice.dto.NoteResponse;
import com.tereshkevich.noteservice.mapper.NoteMapper;
import com.tereshkevich.noteservice.service.NoteService;
import com.tereshkevich.noteservice.util.LogMessages;
import com.tereshkevich.noteservice.util.OpenApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notes")
@Tag(name = OpenApiDocs.NOTES_TAG, description = OpenApiDocs.NOTES_TAG_DESCRIPTION)
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = OpenApiDocs.CREATE_SUMMARY)
    public NoteResponse create(@Valid @RequestBody NoteRequest request) {
        log.debug(LogMessages.CREATE_REQUEST_RECEIVED);
        return noteMapper.toResponse(noteService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = OpenApiDocs.GET_SUMMARY)
    public NoteResponse get(@PathVariable UUID id) {
        log.debug(LogMessages.GET_REQUEST_RECEIVED, id);
        return noteMapper.toResponse(noteService.get(id));
    }

    @GetMapping
    @Operation(summary = OpenApiDocs.FIND_ALL_SUMMARY)
    public List<NoteResponse> findAll() {
        log.debug(LogMessages.FIND_ALL_REQUEST_RECEIVED);
        return noteService.findAll().stream()
                .map(noteMapper::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = OpenApiDocs.UPDATE_SUMMARY)
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody NoteRequest request) {
        log.debug(LogMessages.UPDATE_REQUEST_RECEIVED, id);
        return noteMapper.toResponse(noteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = OpenApiDocs.DELETE_SUMMARY)
    public void delete(@PathVariable UUID id) {
        log.debug(LogMessages.DELETE_REQUEST_RECEIVED, id);
        noteService.delete(id);
    }

}
