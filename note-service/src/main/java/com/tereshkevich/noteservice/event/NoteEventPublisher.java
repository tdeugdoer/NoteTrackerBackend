package com.tereshkevich.noteservice.event;

import com.tereshkevich.noteservice.util.KafkaConstants;
import com.tereshkevich.noteservice.util.LogMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void publish(NoteEvent.EventType eventType, UUID noteId) {
        NoteEvent event = new NoteEvent(eventType, noteId, Instant.now());
        kafkaTemplate.send(KafkaConstants.NOTE_EVENTS_TOPIC, noteId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(LogMessages.EVENT_PUBLISH_FAILED, eventType, noteId, ex);
                    } else {
                        log.info(LogMessages.EVENT_PUBLISHED, eventType, noteId, KafkaConstants.NOTE_EVENTS_TOPIC);
                    }
                });
    }

}
