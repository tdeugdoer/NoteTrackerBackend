package com.tereshkevich.noteservice.service;

import com.tereshkevich.noteservice.domain.Note;
import com.tereshkevich.noteservice.dto.NoteRequest;
import com.tereshkevich.noteservice.event.NoteEvent;
import com.tereshkevich.noteservice.event.NoteEventPublisher;
import com.tereshkevich.noteservice.mapper.NoteMapper;
import com.tereshkevich.noteservice.repository.NoteRepository;
import com.tereshkevich.noteservice.util.LogMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository repository;
    private final NoteEventPublisher publisher;
    private final NoteMapper mapper;

    @Transactional
    public Note create(NoteRequest request) {
        Note note = repository.save(mapper.toEntity(request));
        log.info(LogMessages.NOTE_CREATED, note.getId(), note.getTitle());
        publisher.publish(NoteEvent.EventType.CREATED, note.getId());
        return note;
    }

    @Transactional(readOnly = true)
    public Note get(UUID id) {
        Note note = repository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
        log.debug(LogMessages.NOTE_FETCHED, note.getId());
        return note;
    }

    @Transactional(readOnly = true)
    public List<Note> findAll() {
        List<Note> notes = repository.findAll();
        log.debug(LogMessages.NOTES_FETCHED, notes.size());
        return notes;
    }

    @Transactional
    public Note update(UUID id, NoteRequest request) {
        Note note = repository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
        mapper.updateEntity(request, note);
        log.info(LogMessages.NOTE_UPDATED, note.getId());
        publisher.publish(NoteEvent.EventType.UPDATED, note.getId());
        return note;
    }

    @Transactional
    public void delete(UUID id) {
        Note note = repository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
        repository.delete(note);
        log.info(LogMessages.NOTE_DELETED, id);
        publisher.publish(NoteEvent.EventType.DELETED, id);
    }
}
