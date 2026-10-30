package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.LifeOsDatabase
import com.example.data.model.*
import org.json.JSONObject

/** Draft identities and receipts cross process recreation; all record writes share one transaction. */
class QuickEntryStore(context: Context, private val db: LifeOsDatabase) {
    private val prefs = context.getSharedPreferences("quick_entry", Context.MODE_PRIVATE)
    fun readDraft(): EntryDraft {
        val raw = prefs.getString("draft", null) ?: return EntryDraft()
        return runCatching {
            val json = JSONObject(raw)
            val values = json.getJSONObject("values")
            EntryDraft(json.getString("id"), EntryKind.valueOf(json.getString("kind")), values.keys().asSequence().associateWith { values.getString(it) })
        }.getOrElse { EntryDraft() }
    }
    private fun encode(value: EntryDraft) = JSONObject().put("id", value.id).put("kind", value.kind.name)
        .put("values", JSONObject(value.values)).toString()
    fun draft(value: EntryDraft) { prefs.edit().putString("draft", encode(value)).apply() }
    fun afterSave(value: EntryDraft): EntryDraft {
        val next = EntryDraft(kind = value.kind, values = value.values.filterKeys { !it.startsWith("${value.kind.name}.") })
        check(prefs.edit().putString("draft", encode(next)).commit())
        return next
    }
    fun clearDraft() { check(prefs.edit().remove("draft").commit()) }

    suspend fun save(draft: EntryDraft, now: Long): EntryResult {
        val problem = EntryRules.error(draft)
        require(problem == null) { problem.orEmpty() }
        val dao = db.workspaceDao()
        val title = draft.get("title").trim()
        fun value(key: String) = draft.get(key).trim()
        var reused = false
        val receipt = db.withTransaction {
            dao.entryReceipt(draft.id)?.let { reused = true; return@withTransaction it }
            val key = "entry:${draft.id}"
            val record = when (draft.kind) {
                EntryKind.TASK -> db.taskDao().insertTask(TaskItem(title = title,
                    quadrant = Quadrant.entries.firstOrNull { it.name == value("priority") } ?: Quadrant.Q2_SCHEDULE,
                    category = value("category").ifBlank { "General" }, dueTime = value("due"), isTopThree = value("top") == "true", createdAt = now)).toString()
                EntryKind.PROJECT -> {
                    val explicitLink = EntryRules.repository(value("link"))?.let { "https://github.com/$it" } ?: value("link")
                    val titleLink = EntryRules.repositoryFromTitle(title)?.let { "https://github.com/$it" }.orEmpty()
                    val goal = listOf(value("goal"), explicitLink, titleLink).filter { it.isNotBlank() }.distinct().joinToString("\n")
                    dao.insertSuggestedProject(WorkProject(key, EntryRules.projectTitle(title), goal))
                    EntryRules.actions(value("steps")).forEachIndexed { index, action -> dao.insertSuggestedStep(ProjectStep("$key:$index", key, action, index)) }
                    key
                }
                EntryKind.NOTE -> { dao.saveCapture(CaptureNote(key, title, now)); key }
                EntryKind.TOPIC -> { dao.insertAcademicTopic(ExamTopic(key, value("course"), title, value("link"))); key }
                EntryKind.CODING -> { dao.insertAcademicAttempt(CodingAttempt(key, title, value("link").ifBlank { title.takeIf { it.startsWith("https://") || it.startsWith("http://") }.orEmpty() }, value("level").ifBlank { "Unspecified" }, createdAt = now)); key }
                EntryKind.HABIT -> db.habitDao().insertHabit(HabitItem(title = title, targetDaysPerWeek = value("days").toIntOrNull() ?: 7, category = value("category").ifBlank { "General" })).toString()
                EntryKind.CLASS -> {
                    val start = requireNotNull(EntryRules.time(value("start"))); val end = requireNotNull(EntryRules.time(value("end")))
                    db.academicDao().insertTimetableEntry(TimetableEntry(courseCode = value("course").uppercase(), courseName = title,
                        dayOfWeek = value("day").toInt(), startHour = start / 60, startMinute = start % 60, endHour = end / 60, endMinute = end % 60,
                        room = value("room"), professor = value("professor"))).toString()
                }
                EntryKind.DEADLINE -> db.academicDao().insertAssignment(AssignmentItem(courseCode = value("course"), title = title,
                    description = value("note"), dueDateMillis = AcademicDates.millis(value("date")), weightPercentage = value("weight").toIntOrNull() ?: 0,
                    category = value("category").ifBlank { "Assignment" })).toString()
                EntryKind.WATER -> db.fitnessDao().insertWaterLog(WaterLog(amountMl = title.toInt(), timestamp = now)).toString()
                EntryKind.WEIGHT -> db.fitnessDao().insertWeightLog(BodyWeightLog(weightKg = title.toFloat(), note = value("note"), timestamp = now)).toString()
                EntryKind.TRAINING -> {
                    val minutes = value("minutes").toInt()
                    dao.saveSession(WorkSessionRecord(key, "$title training", "FITNESS", title, "", "", value("note"), now - minutes * 60_000L, now, minutes * 60, "Training · self reported")); key
                }
                EntryKind.SET -> {
                    val split = value("split").ifBlank { "Push" }
                    val number = value("number").toIntOrNull() ?: Math.addExact(db.fitnessDao().latestSetNumber(split, title) ?: 0, 1)
                    db.fitnessDao().insertSet(ExerciseSetLog(exerciseName = title, splitCategory = split,
                        setNumber = number, reps = value("reps").toInt(), weightKg = value("kg").toFloat(), timestamp = now)).toString()
                }
            }
            EntryReceipt(draft.id, draft.kind.name, record).also { dao.saveEntryReceipt(it) }
        }
        val detail = if (reused) "" else when (draft.kind) {
            EntryKind.HABIT -> "Find it in Today → Everyday tools → Habits."
            EntryKind.SET -> "Find it in the ${value("split").ifBlank { "Push" }} routine in Fitness."
            EntryKind.CLASS -> "Find it on the weekday you chose in College."
            EntryKind.TOPIC -> "Find it under your additional revision topics in Exam Prep."
            else -> ""
        }
        return EntryResult(receipt, EntryKind.valueOf(receipt.kind).destination,
            if (reused) "Previously saved ${EntryKind.valueOf(receipt.kind).label.lowercase()}" else if (draft.kind == EntryKind.PROJECT) EntryRules.projectTitle(title) else title, detail)
    }
}
