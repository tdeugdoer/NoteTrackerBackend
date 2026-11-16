package com.tereshkevich.noteservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class LogMessages {

    public final String EVENT_PUBLISHED = "Published {} event for note {} to {}";
    public final String EVENT_PUBLISH_FAILED = "Failed to publish {} event for note {}";

    public final String NOTE_CREATED = "Created note {} with title '{}'";
    public final String NOTE_UPDATED = "Updated note {}";
    public final String NOTE_DELETED = "Deleted note {}";
    public final String NOTE_FETCHED = "Fetched note {}";
    public final String NOTES_FETCHED = "Fetched {} notes";

    public final String CREATE_REQUEST_RECEIVED = "Received create note request";
    public final String GET_REQUEST_RECEIVED = "Received get note request for {}";
    public final String FIND_ALL_REQUEST_RECEIVED = "Received get all notes request";
    public final String UPDATE_REQUEST_RECEIVED = "Received update note request for {}";
    public final String DELETE_REQUEST_RECEIVED = "Received delete note request for {}";

    public final String NOTE_NOT_FOUND = "Request failed: {}";

}
