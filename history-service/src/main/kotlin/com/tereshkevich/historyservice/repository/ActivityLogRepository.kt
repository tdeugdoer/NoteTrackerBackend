package com.tereshkevich.historyservice.repository

import com.tereshkevich.historyservice.domain.ActivityLog
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActivityLogRepository : MongoRepository<ActivityLog, String> {

    fun findByNoteIdOrderByEventTimestampDesc(noteId: UUID): List<ActivityLog>
}
