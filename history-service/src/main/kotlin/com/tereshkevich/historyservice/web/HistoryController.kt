package com.tereshkevich.historyservice.web

import com.tereshkevich.historyservice.dto.ActivityLogResponse
import com.tereshkevich.historyservice.mapper.toResponse
import com.tereshkevich.historyservice.service.HistoryService
import com.tereshkevich.historyservice.util.ApiPaths
import com.tereshkevich.historyservice.util.OpenApiDocs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(ApiPaths.HISTORY)
@Tag(name = OpenApiDocs.HISTORY_TAG, description = OpenApiDocs.HISTORY_TAG_DESCRIPTION)
class HistoryController(
    private val historyService: HistoryService,
) {

    @GetMapping
    @Operation(summary = OpenApiDocs.GET_HISTORY_SUMMARY)
    fun getHistory(@RequestParam(required = false) noteId: UUID?): List<ActivityLogResponse> =
        historyService.findAll(noteId).map { it.toResponse() }
}
