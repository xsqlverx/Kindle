package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.data.local.DatabaseProvider
import com.example.data.model.*
import com.example.data.reminders.PlanReminders
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import java.util.Calendar
import java.util.UUID

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)
    private val dao = db.workspaceDao()
    private val prefs = application.getSharedPreferences("workspace_v2", Context.MODE_PRIVATE)
    private val _destination = MutableStateFlow(Destination.fromId(prefs.getString("destination", null)) ?: Destination.TODAY)
    val destination = _destination.asStateFlow()
    private val _slots = MutableStateFlow(Destination.validateSlots(prefs.getString("slots", "").orEmpty().split(',')))
    val slots = _slots.asStateFlow()
    private val _reduceMotion = MutableStateFlow(prefs.getBoolean("reduce_motion", false))
    val reduceMotion = _reduceMotion.asStateFlow()
    private val _homeAt = MutableStateFlow(prefs.getLong("home_at", 0L))
    val homeAt = _homeAt.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _undo = MutableStateFlow<List<DayBlock>>(emptyList())
    val canUndo = _undo.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private var recovered = emptyList<DayBlock>()

    val blocks = dao.blocks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = dao.sessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val projects = dao.projects().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val steps = dao.steps().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val captures = dao.captures().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val topics = dao.topics().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val attempts = dao.attempts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _reminders = MutableStateFlow(PlanReminders.enabled(application))
    val reminders = _reminders.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.blocks().collect {
                try { PlanReminders.sync(application, it) }
                catch (_: SecurityException) { _message.value = "Android could not schedule reminders. Your plan is saved and remains available here." }
            }
        }
    }
    fun setReminders(value: Boolean) {
        _reminders.value = value
        PlanReminders.setEnabled(getApplication(), value)
        write { PlanReminders.sync(getApplication(), dao.blocks().first()) }
    }

    fun handleReminder(action: String, id: String, focus: TodayPrototypeViewModel) = write {
        if (action == "SESSION") {
            if (focus.session.value.id == id && focus.session.value.phase != FocusPhase.IDLE) {
                focus.refresh(); navigate(Destination.STUDY); focus.requestSessionPanel()
            } else _message.value = "That session was already saved. Its history is in Review."
            getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(id, 1)
            return@write
        }
        navigate(Destination.PLAN)
        val block = dao.blocks().first().firstOrNull { it.id == id && it.state == "PLANNED" && !it.fixed }
        if (block == null) { _message.value = "This block was removed or reviewed. Your current plan is shown."; return@write }
        when (action) {
            "START" -> begin(focus, block.title, block.sourceType, block.sourceId, block.material, block.firstStep, block.id, block.durationMinutes)
            "DELAY" -> db.withTransaction {
                val from = System.currentTimeMillis() + 10 * 60_000L
                val start = DayPlanner.nextWindow(from, dayEnd(from), block.durationMinutes, occupied(from, setOf(block.id)))
                if (start == null) _message.value = "No free window remains today. Shorten it or move it to tomorrow."
                else { dao.saveBlock(block.copy(startMillis = start)); _message.value = "Moved to the next free window at least 10 minutes later." }
            }
            "REPLAN" -> _message.value = "Choose a duration, move to tomorrow, or recover missed starts. Fixed commitments stay protected."
        }
        android.app.NotificationManager::class.java.let { type -> getApplication<Application>().getSystemService(type).cancel(id, 1) }
    }

    fun navigate(to: Destination) {
        _destination.value = to
        prefs.edit().putString("destination", to.id).apply()
    }
    fun assign(index: Int, to: Destination) = saveSlots(Destination.assign(_slots.value, index, to))
    fun move(index: Int, offset: Int) {
        val other = index + offset
        if (index !in 0..3 || other !in 0..3) return
        saveSlots(Destination.assign(_slots.value, index, _slots.value[other]))
    }
    private fun saveSlots(value: List<Destination>) {
        _slots.value = value
        prefs.edit().putString("slots", value.joinToString(",") { it.id }).apply()
    }
    fun setReduceMotion(value: Boolean) {
        _reduceMotion.value = value
        prefs.edit().putBoolean("reduce_motion", value).apply()
    }
    fun checkIn() {
        _homeAt.value = System.currentTimeMillis()
        prefs.edit().putLong("home_at", _homeAt.value).apply()
    }
    fun clearMessage() { _message.value = null }

    private fun write(action: suspend () -> Unit) = viewModelScope.launch {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _message.value = "Could not save. Your current records are still here; try again."
        }
    }
    fun saveProject(project: WorkProject) = write { dao.saveProject(project) }
    fun saveStep(step: ProjectStep) = write { dao.saveStep(step) }
    fun saveCapture(note: CaptureNote) = write { dao.saveCapture(note) }
    fun captureToTask(note: CaptureNote) = write {
        val id = dao.captureToTask(note, db.taskDao())
        _message.value = if (id > 0L) "Saved in Tasks. The original note stays in your processed inbox."
            else "This note is no longer in the inbox. Your current records are shown."
    }
    fun saveTopic(topic: ExamTopic) = write { dao.saveTopic(topic) }
    fun saveAttempt(attempt: CodingAttempt) = write { dao.saveAttempt(attempt) }
    fun clearForUserWipe(focus: TodayPrototypeViewModel) = write {
        if (focus.saving.value) { _message.value = "Wait for the current session save to finish before clearing records."; return@write }
        focus.clearForUserWipe()
        dao.clearWorkspace()
        _homeAt.value = 0L; _undo.value = emptyList()
        prefs.edit().remove("home_at").apply()
    }
    fun recordTraining(split: String, minutes: Int, note: String) = write {
        if (minutes !in 1..360) { _message.value = "Enter 1–360 actual training minutes."; return@write }
        val now = System.currentTimeMillis()
        dao.saveSession(WorkSessionRecord(UUID.randomUUID().toString(), "$split training", "FITNESS", split, "", "", note,
            now - minutes * 60_000L, now, minutes * 60, "Training · self reported"))
    }
    fun deleteBlock(block: DayBlock) = write { dao.deleteBlock(block.id); _undo.value = emptyList() }

    fun moveBlockTomorrow(block: DayBlock) = write {
        db.withTransaction {
            val from = Calendar.getInstance().apply { timeInMillis = block.startMillis; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            val busy = occupied(from, setOf(block.id))
            val start = if (block.fixed) from.takeIf { at -> busy.none { at < it.second && at + block.durationMinutes * 60_000L > it.first } }
                else DayPlanner.nextWindow(from, dayEnd(from), block.durationMinutes, busy)
            if (start == null) _message.value = "No room tomorrow at that duration. The original block is retained."
            else { dao.saveBlock(block.copy(startMillis = start)); _undo.value = emptyList() }
        }
    }

    fun resizeBlock(block: DayBlock, minutes: Int) = write {
        if (minutes !in 5..180) { _message.value = "Choose 5–180 minutes."; return@write }
        db.withTransaction {
            val end = block.startMillis + minutes * 60_000L
            if (end > dayEnd(block.startMillis) || occupied(block.startMillis, setOf(block.id)).any { block.startMillis < it.second && end > it.first })
                _message.value = "That duration overlaps a saved commitment or block."
            else { dao.saveBlock(block.copy(durationMinutes = minutes)); _undo.value = emptyList() }
        }
    }

    fun confirmCommitments(drafts: List<DayBlock>) = write {
        db.withTransaction {
            val busy = occupied(drafts.firstOrNull()?.startMillis ?: return@withTransaction).toMutableList()
            val valid = drafts.all { block ->
                val free = block.fixed && block.durationMinutes in 5..180 && block.endMillis <= dayEnd(block.startMillis) &&
                    busy.none { block.startMillis < it.second && block.endMillis > it.first }
                if (free) busy.add(block.startMillis to block.endMillis)
                free
            }
            if (!valid) _message.value = "These times overlap saved work or commitments. Nothing was added; adjust them in Plan."
            else { dao.saveBlocks(drafts); _undo.value = emptyList(); _message.value = "Your confirmed commitments are in today's plan." }
        }
    }

    /** Dates use the device's calendar zone, matching the rest of the native app. */
    private suspend fun occupied(day: Long, omit: Set<String> = emptySet(), includeSession: Boolean = true): List<Pair<Long, Long>> {
        val cal = Calendar.getInstance().apply { timeInMillis = day }
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        val classes = db.academicDao().getAllTimetable().first().filter { it.dayOfWeek == weekday }.map { entry ->
            fun at(hour: Int, minute: Int) = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            at(entry.startHour, entry.startMinute) to at(entry.endHour, entry.endMinute)
        }
        val focusPrefs = getApplication<Application>().getSharedPreferences("today_focus_session", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val phase = focusPrefs.getString("phase", "")
        val activeEnd = if (phase == FocusPhase.RUNNING.name) focusPrefs.getLong("deadline", now)
            else if (phase == FocusPhase.PAUSED.name) now + focusPrefs.getInt("remaining", 0) * 1000L else now
        val active = if (includeSession && activeEnd > now) listOf(now to activeEnd) else emptyList()
        return classes + active + dao.blocks().first().filter { it.id !in omit && (it.fixed || it.state == "PLANNED") }
            .map { it.startMillis to it.endMillis }
    }

    fun schedule(title: String, minutes: Int, from: Long, until: Long, fixed: Boolean,
                 sourceType: String = "CUSTOM", sourceId: String = "", material: String = "", firstStep: String = "") = write {
        if (title.isBlank() || minutes !in 5..180) return@write
        db.withTransaction {
            val busy = occupied(from)
            val start = if (fixed) from.takeIf { start -> busy.none { start < it.second && start + minutes * 60_000L > it.first } }
                else DayPlanner.nextWindow(maxOf(from, System.currentTimeMillis()), until, minutes, busy)
            if (start == null || start + minutes * 60_000L > until) {
                _message.value = "No room in this window. Choose fewer minutes or a different start; commitments stay protected."
            } else {
                dao.saveBlock(DayBlock(UUID.randomUUID().toString(), title.trim(), start, minutes, fixed, sourceType, sourceId, material.trim(), firstStep.trim()))
                _undo.value = emptyList()
                _message.value = "Added to Plan."
            }
        }
    }

    fun recover(from: Long, until: Long, activeBlockId: String) = write {
        db.withTransaction {
            val overdue = dao.blocks().first().filter {
                !it.fixed && it.state == "PLANNED" && it.startMillis in dayStart(from) until from && it.id != activeBlockId
            }.sortedBy { it.startMillis }
            val busy = occupied(from, overdue.map { it.id }.toSet()).toMutableList()
            val replacements = overdue.mapNotNull { block ->
                DayPlanner.nextWindow(from, until, block.durationMinutes, busy)?.let { start ->
                    block.copy(startMillis = start).also { busy.add(it.startMillis to it.endMillis) }
                }
            }
            dao.saveBlocks(replacements)
            recovered = replacements
            _undo.value = overdue.filter { old -> replacements.any { it.id == old.id } }
            _message.value = if (replacements.isEmpty()) "No missed block fits before the day ends. Edit its duration or move it to tomorrow."
                else "Moved ${replacements.size} missed block(s). ${overdue.size - replacements.size} still need a smaller window."
        }
    }

    fun undoRecovery() = write {
        db.withTransaction {
            val current = dao.blocks().first()
            if (recovered.all { saved -> current.firstOrNull { it.id == saved.id } == saved }) {
                dao.saveBlocks(_undo.value)
                _message.value = "Previous block times restored."
            } else _message.value = "The plan changed since recovery. Edit the affected blocks instead."
            _undo.value = emptyList()
        }
    }

    fun resumeSession(focus: TodayPrototypeViewModel) = write {
        val session = focus.session.value
        if (session.phase != FocusPhase.PAUSED) return@write
        val now = System.currentTimeMillis()
        val end = now + session.pausedSeconds * 1000L
        if (end > dayEnd(now) || occupied(now, setOf(session.blockId), includeSession = false).any { now < it.second && end > it.first }) {
            _message.value = "Continuing now would overlap your plan or a saved class. Keep this paused, or save it and plan a smaller continuation."
        } else focus.resume()
    }

    fun begin(focus: TodayPrototypeViewModel, title: String, sourceType: String = "CUSTOM", sourceId: String = "",
              material: String = "", firstStep: String = "", blockId: String = "", minutes: Int = 25, kind: FocusKind = FocusKind.STUDY) = write {
        if (focus.session.value.phase != FocusPhase.IDLE) { focus.requestSessionPanel(); return@write }
        val now = System.currentTimeMillis()
        val busy = occupied(now, setOf(blockId))
        val until = now + minutes * 60_000L
        if (until > dayEnd(now) || busy.any { now < it.second && until > it.first }) {
            _message.value = "These minutes overlap your plan or a saved class. Schedule a free window in Plan."
            return@write
        }
        focus.start(title, kind = kind, sourceType = sourceType, sourceId = sourceId, material = material,
            firstStep = firstStep, blockId = blockId, minutes = minutes)
        focus.requestSessionPanel()
    }

    companion object {
        fun dayStart(at: Long): Long = Calendar.getInstance().apply {
            timeInMillis = at; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        fun dayEnd(at: Long): Long = Calendar.getInstance().apply { timeInMillis = dayStart(at); add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    }
}
