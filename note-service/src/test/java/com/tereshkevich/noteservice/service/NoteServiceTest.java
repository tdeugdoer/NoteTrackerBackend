package com.tereshkevich.noteservice.service;

import com.tereshkevich.noteservice.domain.Note;
import com.tereshkevich.noteservice.dto.NoteRequest;
import com.tereshkevich.noteservice.event.NoteEvent;
import com.tereshkevich.noteservice.event.NoteEventPublisher;
import com.tereshkevich.noteservice.mapper.NoteMapper;
import com.tereshkevich.noteservice.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final UUID NOTE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private NoteRepository repository;

    @Mock
    private NoteEventPublisher publisher;

    @Mock
    private NoteMapper mapper;

    @InjectMocks
    private NoteService service;

    @Test
    void createMapsRequestSavesNoteAndPublishesCreatedEvent() {
        NoteRequest request = new NoteRequest("Title", "Content");
        Note mapped = new Note("Title", "Content");
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(repository.save(mapped)).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            ReflectionTestUtils.setField(note, "id", NOTE_ID);
            return note;
        });

        Note result = service.create(request);

        assertThat(result.getId()).isEqualTo(NOTE_ID);
        verify(publisher).publish(NoteEvent.EventType.CREATED, NOTE_ID);
    }

    @Test
    void updateMapsRequestOntoExistingNoteAndPublishesUpdatedEvent() {
        NoteRequest request = new NoteRequest("New title", "New content");
        Note note = new Note("Old title", "Old content");
        ReflectionTestUtils.setField(note, "id", NOTE_ID);
        when(repository.findById(NOTE_ID)).thenReturn(Optional.of(note));

        Note result = service.update(NOTE_ID, request);

        assertThat(result).isSameAs(note);
        verify(mapper).updateEntity(request, note);
        verify(publisher).publish(NoteEvent.EventType.UPDATED, NOTE_ID);
    }

    @Test
    void updateThrowsWhenNoteIsMissing() {
        when(repository.findById(NOTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(NOTE_ID, new NoteRequest("Title", "Content")))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessageContaining(NOTE_ID.toString());

        verifyNoInteractions(publisher);
    }

    @Test
    void deleteRemovesNoteAndPublishesDeletedEvent() {
        Note note = new Note("Title", "Content");
        when(repository.findById(NOTE_ID)).thenReturn(Optional.of(note));

        service.delete(NOTE_ID);

        verify(repository).delete(note);
        verify(publisher).publish(NoteEvent.EventType.DELETED, NOTE_ID);
    }
}
