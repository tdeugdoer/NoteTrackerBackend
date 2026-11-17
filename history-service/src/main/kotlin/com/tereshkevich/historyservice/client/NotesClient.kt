package com.tereshkevich.historyservice.client

import com.tereshkevich.historyservice.dto.NoteResponse
import com.tereshkevich.historyservice.util.ApiPaths
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.*

@FeignClient(name = NotesClient.NAME, url = $$"${notes-service.url}")
interface NotesClient {

    @GetMapping(ApiPaths.NOTE_BY_ID)
    fun getNote(@PathVariable("id") id: UUID): NoteResponse

    companion object {
        const val NAME = "note-service"
    }
}
