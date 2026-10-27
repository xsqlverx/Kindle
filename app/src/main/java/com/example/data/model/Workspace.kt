package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** IDs are persisted, independent of enum order and visible labels. */
enum class Destination(val id: String, val title: String, val purpose: String) {
    TODAY("today", "Today", "Now, next & recovery"),
    PLAN("plan", "Plan", "Make room for what matters"),
    COLLEGE("college", "College", "Classes, attendance & deadlines"),
    STUDY("study", "Study", "Material, focus & continuation"),
    FITNESS("fitness", "Fitness", "Training, nutrition & health"),
    TASKS("tasks", "Tasks", "Actions & priorities"),
    PROJECTS("projects", "Projects", "Milestones & next steps"),
    CAPTURE("capture", "Capture", "An inbox for thoughts & links"),
    EXAMS("exams", "Exam Prep", "Topics & revision queue"),
    PRACTICE("practice", "Coding Practice", "Attempts, lessons & retries"),
    REVIEW("review", "Review", "A trail of actual work"),
    RESET("reset", "Daily Reset", "Close today, prepare tomorrow"),
    SETTINGS("settings", "Settings", "Make this space yours");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id }
        val defaults = listOf(TODAY, TASKS, COLLEGE, FITNESS)
        fun validateSlots(ids: List<String>): List<Destination> =
            (ids.mapNotNull(::fromId) + defaults + entries).distinct().take(4)

        /** Choosing an assigned destination swaps the two slots; no duplicate slots. */
        fun assign(slots: List<Destination>, index: Int, destination: Destination): List<Destination> {
            require(index in 0..3 && slots.size == 4)
            val result = slots.toMutableList()
            val previous = result.indexOf(destination)
            if (previous >= 0) result[previous] = result[index]
            result[index] = destination
            return result
        }
    }
}

@Entity(tableName = "day_blocks")
data class DayBlock(
    @PrimaryKey val id: String,
    val title: String,
    val startMillis: Long,
    val durationMinutes: Int,
    val fixed: Boolean = false,
    val sourceType: String = "CUSTOM",
    val sourceId: String = "",
    val material: String = "",
    val firstStep: String = "",
    val state: String = "PLANNED"
) {
    @get:androidx.room.Ignore
    val endMillis: Long
        get() = startMillis + durationMinutes * 60_000L
}

@Entity(tableName = "work_sessions")
data class WorkSessionRecord(
    @PrimaryKey val id: String,
    val title: String,
    val sourceType: String,
    val sourceId: String,
    val blockId: String,
    val material: String,
    val resumeNote: String,
    val startedAt: Long,
    val endedAt: Long,
    val elapsedSeconds: Int,
    val outcome: String
)

@Entity(tableName = "work_projects")
data class WorkProject(
    @PrimaryKey val id: String,
    val title: String,
    val goal: String,
    val blocker: String = "",
    val archived: Boolean = false
)

@Entity(tableName = "project_steps")
data class ProjectStep(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val position: Int,
    val done: Boolean = false
)

@Entity(tableName = "capture_notes")
data class CaptureNote(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val taskId: Long = 0L,
    val archived: Boolean = false
)

@Entity(tableName = "exam_topics")
data class ExamTopic(
    @PrimaryKey val id: String,
    val course: String,
    val title: String,
    val material: String,
    val confidence: Int = 0,
    val revisionAt: Long = 0L
)

@Entity(tableName = "coding_attempts")
data class CodingAttempt(
    @PrimaryKey val id: String,
    val problem: String,
    val link: String,
    val difficulty: String,
    val outcome: String = "Unattempted",
    val notes: String = "",
    val retryAt: Long = 0L,
    val createdAt: Long
)

/** Half-open intervals allow work to end exactly when a fixed commitment starts. */
object DayPlanner {
    fun nextWindow(from: Long, until: Long, minutes: Int, occupied: List<Pair<Long, Long>>): Long? {
        if (minutes !in 1..240 || until <= from) return null
        val duration = minutes * 60_000L
        var candidate = from
        occupied.filter { it.second > from && it.first < until }.sortedBy { it.first }.forEach { (start, end) ->
            if (candidate + duration <= start) return candidate.takeIf { it + duration <= until }
            if (candidate < end) candidate = end
        }
        return candidate.takeIf { it + duration <= until }
    }
}
