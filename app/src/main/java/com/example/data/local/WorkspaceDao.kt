package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM entry_receipts WHERE id = :id") suspend fun entryReceipt(id: String): EntryReceipt?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun saveEntryReceipt(receipt: EntryReceipt)
    @Query("DELETE FROM entry_receipts") suspend fun clearEntryReceipts()
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSuggestedProject(project: WorkProject)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSuggestedStep(step: ProjectStep)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAcademicTopic(topic: ExamTopic)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAcademicAttempt(attempt: CodingAttempt)
    @Query("SELECT * FROM day_blocks ORDER BY startMillis") suspend fun blocksSnapshot(): List<DayBlock>
    @Query("SELECT * FROM work_projects") suspend fun projectsSnapshot(): List<WorkProject>
    @Query("SELECT * FROM project_steps") suspend fun stepsSnapshot(): List<ProjectStep>
    @Query("SELECT * FROM exam_topics") suspend fun topicsSnapshot(): List<ExamTopic>
    @Query("SELECT * FROM coding_attempts") suspend fun attemptsSnapshot(): List<CodingAttempt>
    @Query("SELECT * FROM day_blocks WHERE id = :id") suspend fun blockById(id: String): DayBlock?
    @Query("SELECT * FROM project_steps WHERE id = :id") suspend fun stepById(id: String): ProjectStep?
    @Query("SELECT * FROM exam_topics WHERE id = :id") suspend fun topicById(id: String): ExamTopic?
    @Query("SELECT * FROM coding_attempts WHERE id = :id") suspend fun attemptById(id: String): CodingAttempt?
    @Query("SELECT * FROM day_blocks ORDER BY startMillis") fun blocks(): Flow<List<DayBlock>>
    @Upsert suspend fun saveBlock(block: DayBlock)
    @Upsert suspend fun saveBlocks(blocks: List<DayBlock>)
    @Query("DELETE FROM day_blocks WHERE id = :id") suspend fun deleteBlock(id: String)
    @Query("UPDATE day_blocks SET state = 'REVIEWED' WHERE id = :id") suspend fun reviewBlock(id: String)
    @Query("UPDATE day_blocks SET state = :state WHERE sourceType = :type AND sourceId = :source AND fixed = 0 AND state IN ('PLANNED', 'DONE', 'DEFERRED', 'DROPPED')")
    suspend fun sourcePlanState(type: String, source: String, state: String)
    @Query("SELECT * FROM work_sessions ORDER BY endedAt DESC") fun sessions(): Flow<List<WorkSessionRecord>>
    @Query("SELECT * FROM work_sessions WHERE id = :id") suspend fun sessionById(id: String): WorkSessionRecord?
    @Query("UPDATE work_sessions SET resumeNote = :note WHERE id = :id") suspend fun updateSessionNote(id: String, note: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun saveSession(record: WorkSessionRecord)
    @Query("SELECT * FROM work_projects ORDER BY title") fun projects(): Flow<List<WorkProject>>
    @Upsert suspend fun saveProject(project: WorkProject)
    @Query("SELECT * FROM project_steps ORDER BY position") fun steps(): Flow<List<ProjectStep>>
    @Upsert suspend fun saveStep(step: ProjectStep)
    @Query("SELECT * FROM capture_notes ORDER BY createdAt DESC") fun captures(): Flow<List<CaptureNote>>
    @Upsert suspend fun saveCapture(note: CaptureNote)
    @Query("SELECT * FROM exam_topics ORDER BY revisionAt, course") fun topics(): Flow<List<ExamTopic>>
    @Upsert suspend fun saveTopic(topic: ExamTopic)
    @Query("SELECT * FROM coding_attempts ORDER BY createdAt DESC") fun attempts(): Flow<List<CodingAttempt>>
    @Upsert suspend fun saveAttempt(attempt: CodingAttempt)

    @Transaction
    suspend fun finish(record: WorkSessionRecord) {
        saveSession(record)
        // A retry after archive-before-preferences-clear may contain a revised note.
        // Keep the original elapsed-time snapshot, and preserve that explicit note edit.
        updateSessionNote(record.id, record.resumeNote)
        if (record.blockId.isNotBlank()) reviewBlock(record.blockId)
    }

    @Transaction
    suspend fun captureToTask(note: CaptureNote, tasks: TaskDao): Long {
        // Re-read inside the write transaction to make repeated taps idempotent.
        val current = captureById(note.id) ?: return 0L
        if (current.taskId > 0L) return current.taskId
        val id = tasks.insertTask(TaskItem(title = current.text, category = "Capture", quadrant = Quadrant.Q2_SCHEDULE))
        saveCapture(current.copy(taskId = id, archived = true))
        return id
    }
    @Query("SELECT * FROM capture_notes WHERE id = :id") suspend fun captureById(id: String): CaptureNote?

    @Query("DELETE FROM day_blocks") suspend fun clearBlocks()
    @Query("DELETE FROM work_sessions") suspend fun clearSessions()
    @Query("DELETE FROM project_steps") suspend fun clearSteps()
    @Query("DELETE FROM work_projects") suspend fun clearProjects()
    @Query("DELETE FROM capture_notes") suspend fun clearCaptures()
    @Query("DELETE FROM exam_topics") suspend fun clearTopics()
    @Query("DELETE FROM coding_attempts") suspend fun clearAttempts()
    @Transaction suspend fun clearWorkspace() {
        clearBlocks(); clearSessions(); clearSteps(); clearProjects(); clearCaptures(); clearTopics(); clearAttempts(); clearEntryReceipts()
    }
}
