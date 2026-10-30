package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EntryKind(val label: String, val destination: Destination, val prompt: String, val example: String) {
    TASK("Task", Destination.TASKS, "What do you want to do?", "Finish the Python lab record"),
    PROJECT("Project", Destination.PROJECTS, "What are you building?", "My portfolio — or paste a GitHub repo URL"),
    NOTE("Note", Destination.CAPTURE, "What do you want to keep?", "An idea, a link, or a question for class"),
    DEADLINE("Deadline", Destination.COLLEGE, "What needs handing in?", "Graphics sheet 4"),
    CLASS("Class", Destination.COLLEGE, "Which class is this?", "Choose a course below, or type its name"),
    TOPIC("Study topic", Destination.EXAMS, "What needs another pass?", "Chain rule — implicit differentiation"),
    CODING("Coding problem", Destination.PRACTICE, "What will you solve?", "Two sum — or paste the problem URL"),
    HABIT("Habit", Destination.TODAY, "What would you like to repeat?", "Read a few pages"),
    WATER("Water", Destination.FITNESS, "How much did you drink?", "250"),
    WEIGHT("Body weight", Destination.FITNESS, "What did the scale show?", "Enter your actual weight in kg"),
    TRAINING("Training", Destination.REVIEW, "What training did you do?", "Push session"),
    SET("Exercise set", Destination.FITNESS, "Which exercise?", "Dumbbell press")
}

data class EntryDraft(val id: String = UUID.randomUUID().toString(), val kind: EntryKind = EntryKind.TASK,
    val values: Map<String, String> = emptyMap()) {
    fun get(key: String) = values["${kind.name}.$key"].orEmpty()
    fun edit(key: String, value: String) = copy(values = values + ("${kind.name}.$key" to value))
}

@Entity(tableName = "entry_receipts")
data class EntryReceipt(@PrimaryKey val id: String, val kind: String, val recordId: String)

data class EntryResult(val receipt: EntryReceipt, val destination: Destination, val title: String, val detail: String = "")
data class ProjectSuggestion(val id: String, val name: String, val repo: String, val revision: String,
    val evidence: String, val why: String, val goal: String, val steps: List<String>)

object EntryRules {
    fun repository(text: String): String? = Regex("^(?:https?://(?:www\\.)?github\\.com/)?([A-Za-z0-9_-]+/[A-Za-z0-9_.-]+?)(?:\\.git)?/?$", RegexOption.IGNORE_CASE)
        .matchEntire(text.trim())?.groupValues?.get(1)
    fun repositoryFromTitle(text: String): String? = if (Regex("^https?://(?:www\\.)?github\\.com/", RegexOption.IGNORE_CASE).containsMatchIn(text.trim())) repository(text) else null
    fun projectTitle(text: String) = repositoryFromTitle(text)?.substringAfter('/') ?: text.trim()
    fun actions(text: String) = text.lines().map { it.trim().removePrefix("- ").trim() }.filter { it.isNotBlank() }
    fun meal(values: List<String>, current: List<Int>): List<Int>? {
        if (values.size != 4 || current.size != 4) return null
        val amounts = values.map { it.toIntOrNull() ?: return null }
        if (amounts.any { it !in 0..100000 } || amounts.indices.any { current[it].toLong() + amounts[it] > Int.MAX_VALUE }) return null
        return amounts
    }
    fun time(text: String): Int? {
        if (!Regex("[0-2][0-9]:[0-5][0-9]").matches(text)) return null
        val hour = text.substringBefore(':').toInt()
        return if (hour < 24) hour * 60 + text.substringAfter(':').toInt() else null
    }
    fun error(draft: EntryDraft): String? {
        fun whole(key: String, range: IntRange) = draft.get(key).toIntOrNull()?.let { it in range } == true
        fun decimal(key: String, allowZero: Boolean = false) = draft.get(key).toFloatOrNull()?.let { it.isFinite() && if (allowZero) it >= 0f else it > 0f } == true
        if (draft.get("title").isBlank()) return draft.kind.prompt
        return when (draft.kind) {
            EntryKind.PROJECT -> when {
                actions(draft.get("steps")).size > 40 -> "Keep this batch to 40 next actions; you can add more later."
                draft.get("link").isNotBlank() && repository(draft.get("link")) == null && runCatching { java.net.URI(draft.get("link")).let { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() } }.getOrDefault(false).not() -> "Use a GitHub repository or a full http(s) link, or leave it blank."
                else -> null
            }
            EntryKind.CLASS -> when {
                draft.get("course").isBlank() -> "Choose a course or enter its code."
                !whole("day", 1..7) -> "Choose the weekday this class happens."
                time(draft.get("start")) == null || time(draft.get("end")) == null -> "Pick both the class start and end time."
                time(draft.get("end"))!! <= time(draft.get("start"))!! -> "The class must end after it starts, on the same day."
                else -> null
            }
            EntryKind.DEADLINE -> if (runCatching { AcademicDates.millis(draft.get("date")) }.isFailure) "Choose the actual due date." else if (draft.get("weight").isNotBlank() && !whole("weight", 0..100)) "Marks weight must be 0–100%, or leave it blank." else null
            EntryKind.HABIT -> if (draft.get("days").isNotBlank() && !whole("days", 1..7)) "Choose 1–7 days per week." else null
            EntryKind.WATER -> if (!whole("title", 1..10000)) "Enter 1–10000 ml actually drunk, as a whole number." else null
            EntryKind.WEIGHT -> if (!decimal("title")) "Enter a positive, finite weight in kg." else null
            EntryKind.TRAINING -> if (!whole("minutes", 1..360)) "Enter 1–360 actual training minutes." else null
            EntryKind.SET -> if (!whole("reps", 1..1000) || !decimal("kg", true)) "Enter planned reps (1–1000) and load in kg (0 for bodyweight)." else if (draft.get("number").isNotBlank() && !whole("number", 1..99)) "Set number must be 1–99, or leave it blank." else null
            EntryKind.TASK -> if (draft.get("priority").isNotBlank() && Quadrant.entries.none { it.name == draft.get("priority") }) "Choose one of the task priorities." else null
            else -> null
        }
    }
}
