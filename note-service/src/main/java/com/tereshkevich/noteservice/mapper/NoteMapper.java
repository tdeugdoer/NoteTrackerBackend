package com.tereshkevich.noteservice.mapper;

import com.tereshkevich.noteservice.domain.Note;
import com.tereshkevich.noteservice.dto.NoteRequest;
import com.tereshkevich.noteservice.dto.NoteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteMapper {

    NoteResponse toResponse(Note note);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Note toEntity(NoteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(NoteRequest request, @MappingTarget Note note);
}
