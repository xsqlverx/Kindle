package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.data.local.DatabaseProvider
import com.example.data.model.*
import com.example.data.reminders.PlanReminders
import com.example.data.repository.AcademicStore
import com.example.data.repository.QuickEntryStore
import com.example.data.repository.ProjectQueueStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.UUID
import org.json.JSONObject

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)
    private val dao = db.workspaceDao()
    private val prefs = application.getSharedPreferences("workspace_v2", Context.MODE_PRIVATE)
    private val academicStore = AcademicStore(application, db)
    private val entryStore = QuickEntryStore(application, db)
    private val projectQueueStore = ProjectQueueStore(application, db)
    private val _projectSuggestions = MutableStateFlow<List<ProjectSuggestion>>(emptyList())
    val projectSuggestions = _projectSuggestions.asStateFlow()
    private val _entryDraft = MutableStateFlow(entryStore.readDraft())
    val entryDraft = _entryDraft.asStateFlow()
    private val _entryOpen = MutableStateFlow(false)
    val entryOpen = _entryOpen.asStateFlow()
    private val _entrySaving = MutableStateFlow(false)
    val entrySaving = _entrySaving.asStateFlow()
    private val _entryResult = MutableStateFlow<EntryResult?>(null)
    val entryResult = _entryResult.asStateFlow()
    private val _entryError = MutableStateFlow<String?>(null)
    val entryError = _entryError.asStateFlow()
    private val _academicPack = MutableStateFlow<AcademicPack?>(null)
    val academicPack = _academicPack.asStateFlow()
    private val _academicError = MutableStateFlow<String?>(null)
    val academicError = _academicError.asStateFlow()
    private val _academicRoadmap = MutableStateFlow(AcademicRoadmap())
    val academicRoadmap = _academicRoadmap.asStateFlow()
    private val academicInitialization = Mutex()
    private val writes = Mutex()
    private val _destination = MutableStateFlow(if (!prefs.getBoolean("guided_day_introduced", false)) Destination.TODAY
        else Destination.fromId(prefs.getString("destination", null)) ?: Destination.TODAY)
    val destination = _destination.asStateFlow()
    private val _slots = MutableStateFlow(Destination.validateSlots(prefs.getString("slots", "").orEmpty().split(',')))
    val slots = _slots.asStateFlow()
    private val _reduceMotion = MutableStateFlow(prefs.getBoolean("reduce_motion", false))
    val reduceMotion = _reduceMotion.asStateFlow()
    private val _homeAt = MutableStateFlow(prefs.getLong("home_at", 0L))
    val homeAt = _homeAt.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _moveDraft = MutableStateFlow(if (application.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE).getString("phase", "IDLE") == "IDLE") readMoveDraft() else null)
    val moveDraft = _moveDraft.asStateFlow()
    private val _moveMinutesText = MutableStateFlow(readMoveMinutesText(_moveDraft.value))
    val moveMinutesText = _moveMinutesText.asStateFlow()
    private val _journeyBusy = MutableStateFlow(false)
    val journeyBusy = _journeyBusy.asStateFlow()
    private val _showFirstLap = MutableStateFlow(!prefs.getBoolean("hide_first_lap", false))
    val showFirstLap = _showFirstLap.asStateFlow()
    fun hideFirstLap() { _showFirstLap.value = false; prefs.edit().putBoolean("hide_first_lap", true).apply() }
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
            try { academicInitialization.withLock {
                val suggestions = projectQueueStore.load()
                projectQueueStore.install(suggestions)
                _projectSuggestions.value = suggestions
            } } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { _message.value = "Could not open the suggested GitHub queue. You can still add your own projects." }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                academicInitialization.withLock {
                    val pack = academicStore.load()
                    academicStore.install(pack)
                    val roadmap = academicStore.readRoadmap() ?: AcademicRoadmap(
                        targets = SyllabusPlanner.build(pack, AcademicDates.key(System.currentTimeMillis()), 50)).also(academicStore::saveRoadmap)
                    _academicRoadmap.value = roadmap
                    _academicPack.value = pack
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { _academicError.value = "Could not open your syllabus and calendar. Your existing records and tools are still available." }
        }
        prefs.edit().putBoolean("guided_day_introduced", true).apply()
        viewModelScope.launch(Dispatchers.IO) {
            dao.blocks().collect {
                try { PlanReminders.sync(application, it) }
                catch (_: SecurityException) { _message.value = "Android could not schedule reminders. Your plan is saved and remains available here." }
            }
        }
    }
    fun openEntry(kind: EntryKind? = null) {
        if (_entrySaving.value) return
        _entryResult.value = null; _entryError.value = null
        if (kind != null) { _entryDraft.value = _entryDraft.value.copy(kind = kind); entryStore.draft(_entryDraft.value) }
        _entryOpen.value = true
    }
    fun editEntry(key: String, text: String) {
        if (_entrySaving.value || _entryResult.value != null) return
        _entryDraft.value = _entryDraft.value.edit(key, text)
        entryStore.draft(_entryDraft.value); _entryError.value = null
    }
    fun prefillEntry(key: String, text: String) { if (_entryDraft.value.get(key).isBlank()) editEntry(key, text) }
    fun discardEntryKind() {
        if (_entrySaving.value || _entryResult.value != null) return
        val old = _entryDraft.value
        _entryDraft.value = EntryDraft(kind = old.kind, values = old.values.filterKeys { !it.startsWith("${old.kind.name}.") })
        entryStore.draft(_entryDraft.value); _entryError.value = null
    }
    fun closeEntry() { if (!_entrySaving.value) _entryOpen.value = false }
    fun saveEntry() {
        if (_entrySaving.value || _entryResult.value != null) return
        val draft = _entryDraft.value
        EntryRules.error(draft)?.let { _entryError.value = it; return }
        _entrySaving.value = true
        viewModelScope.launch {
            try { writes.withLock {
                if (_entryDraft.value.id != draft.id) return@withLock
                val result = entryStore.save(draft, System.currentTimeMillis())
                _entryDraft.value = withContext(Dispatchers.IO) { entryStore.afterSave(draft) }
                _entryResult.value = result; _entryError.value = null
            } } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { _entryError.value = "Could not finish saving. Your draft is kept; retry here to avoid adding it twice."
            } finally { _entrySaving.value = false }
        }
    }
    fun openEntryResult() { _entryResult.value?.let { navigate(it.destination); _entryOpen.value = false } }
    fun addEntryAgain() { _entryResult.value = null; _entryError.value = null }

    private suspend fun saveAcademic(value: AcademicRoadmap) {
        withContext(Dispatchers.IO) { academicStore.saveRoadmap(value) }
        _academicRoadmap.value = value
    }
    fun replanAcademic(minutes: Int, modules: Set<String>) = write {
        val pack = _academicPack.value ?: return@write
        if (minutes !in 10..180) { _message.value = "Choose 10–180 suggested study minutes per day."; return@write }
        val old = _academicRoadmap.value
        val targets = SyllabusPlanner.build(pack, AcademicDates.key(System.currentTimeMillis()), minutes, old.covered, modules)
        saveAcademic(old.copy(targets = (old.targets.filter { it.id in old.done } + targets).distinctBy { it.id }, dailyMinutes = minutes, modules = modules))
        _message.value = "Preparation targets refreshed. Your actual scheduled blocks and history stay intact."
    }
    fun setAcademicCovered(id: String, covered: Boolean) = write {
        if (_academicPack.value?.items?.none { it.id == id } != false) return@write
        val old = _academicRoadmap.value
        saveAcademic(old.copy(covered = if (covered) old.covered + id else old.covered - id))
    }
    fun setAcademicTargetDone(id: String, done: Boolean) = write {
        val old = _academicRoadmap.value
        if (old.targets.none { it.id == id }) return@write
        saveAcademic(old.copy(done = if (done) old.done + id else old.done - id))
    }
    fun offerAcademic(id: String) = write {
        val pack = _academicPack.value ?: return@write
        val item = pack.items.firstOrNull { it.id == id } ?: return@write
        withContext(Dispatchers.IO) { academicStore.addItem(pack, item) }
        val date = AcademicDates.key(System.currentTimeMillis())
        val target = _academicRoadmap.value.targets.firstOrNull { it.itemId == id && it.date <= date && it.id !in _academicRoadmap.value.done }
        val existing = dao.blocksSnapshot().firstOrNull { it.sourceType == item.sourceType && it.sourceId == id && it.state == "PLANNED" && AcademicDates.key(it.startMillis) == date }
        val topic = dao.topicById(id)
        val attempt = dao.attemptById(id)
        val last = dao.sessions().first().firstOrNull { it.sourceType == item.sourceType && it.sourceId == id }
            ?.takeIf { it.resumeNote.isNotBlank() }
        offerMove(DayMove("${pack.courses.first { it.id == item.course }.short} · ${topic?.title ?: attempt?.problem ?: item.title}",
            item.sourceType, id, last?.material?.takeIf { it.isNotBlank() } ?: topic?.material ?: attempt?.link ?: item.material,
            last?.resumeNote ?: attempt?.notes?.takeIf { it.isNotBlank() } ?: item.firstStep,
            "From your supplied syllabus · ${item.module}. Scope and portion are shown in Exam Prep.", existing?.id.orEmpty(), target?.minutes ?: 25))
    }
    fun setReminders(value: Boolean) {
        _reminders.value = value
        PlanReminders.setEnabled(getApplication(), value)
        write { PlanReminders.sync(getApplication(), dao.blocksSnapshot()) }
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
        val block = dao.blocksSnapshot().firstOrNull { it.id == id && it.state == "PLANNED" && !it.fixed }
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

    private fun readMoveDraft(): DayMove? = try {
        prefs.getString("move_draft", null)?.let { raw ->
            val value = JSONObject(raw)
            DayMove(value.optString("title"), value.optString("type", "CUSTOM"), value.optString("source"),
                value.optString("material"), value.optString("step"), value.optString("reason"), value.optString("block"),
                value.optInt("minutes", 25).coerceIn(1, 180))
        }
    } catch (_: Exception) { null }

    private fun readMoveMinutesText(move: DayMove?): String = try {
        val fallback = move?.minutes?.toString().orEmpty()
        prefs.getString("move_draft", null)?.let { JSONObject(it).optString("durationText", fallback).take(12) } ?: fallback
    } catch (_: Exception) { move?.minutes?.toString().orEmpty() }

    fun offerMove(move: DayMove? = null) {
        if (_journeyBusy.value) return
        clearMessage()
        if (move?.sourceType == "PREVIEW" && readMoveDraft()?.sourceType != "PREVIEW") {
            prefs.getString("move_draft", null)?.let { prefs.edit().putString("draft_before_preview", it).apply() }
        }
        val selected = move ?: readMoveDraft() ?: DayMove("")
        _moveMinutesText.value = if (move == null) readMoveMinutesText(selected) else selected.minutes.toString()
        persistMove(selected)
    }
    private fun finishPreviewDraft() {
        val backup = prefs.getString("draft_before_preview", null)
        prefs.edit().apply { if (backup != null) putString("move_draft", backup) else remove("move_draft"); remove("draft_before_preview") }.apply()
        _moveDraft.value = null
        _moveMinutesText.value = ""
    }
    fun stashMove() {
        if (_journeyBusy.value) return
        if (_moveDraft.value?.sourceType == "PREVIEW") finishPreviewDraft() else _moveDraft.value = null
    }
    fun updateMove(move: DayMove) {
        if (_journeyBusy.value) return
        val current = _moveDraft.value ?: return
        // Metadata callbacks may carry an older snapshot. The time editor owns the duration.
        val minutes = MoveDuration.parse(_moveMinutesText.value) ?: current.minutes
        persistMove(move.copy(minutes = minutes))
    }
    fun updateMoveMinutes(text: String) {
        if (_journeyBusy.value) return
        val draft = _moveDraft.value ?: return
        _message.value = null
        _moveMinutesText.value = text.take(12)
        val minutes = MoveDuration.parse(_moveMinutesText.value)
        persistMove(if (minutes == null) draft else draft.copy(minutes = minutes))
    }
    private fun persistMove(move: DayMove) {
        _moveDraft.value = move
        val value = JSONObject().put("title", move.title).put("type", move.sourceType).put("source", move.sourceId)
            .put("material", move.material).put("step", move.firstStep).put("reason", move.reason)
            .put("block", move.blockId).put("minutes", move.minutes).put("durationText", _moveMinutesText.value)
        prefs.edit().putString("move_draft", value.toString()).apply()
    }
    fun closeMove() {
        if (_journeyBusy.value) return
        _moveDraft.value = null
        _moveMinutesText.value = ""
        prefs.edit().remove("move_draft").apply()
    }

    private suspend fun sourceAvailable(move: DayMove): Boolean = when (move.sourceType) {
        "TASK" -> db.taskDao().taskById(move.sourceId.toLongOrNull() ?: -1)?.status == TaskStatus.PENDING
        "ASSIGNMENT" -> db.academicDao().assignmentById(move.sourceId.toLongOrNull() ?: -1)?.isSubmitted == false
        "PROJECT_STEP" -> dao.stepById(move.sourceId)?.let { step -> !step.done && dao.projectsSnapshot().any { it.id == step.projectId && !it.archived } } == true
        "TOPIC" -> dao.topicById(move.sourceId) != null
        "PRACTICE" -> dao.attemptById(move.sourceId) != null
        "CUSTOM", "REST" -> true
        else -> false
    }

    /** One explicit action links or creates work, places the same record, then starts if free now. */
    fun placeMove(focus: TodayPrototypeViewModel, start: Boolean) = write {
        if (_journeyBusy.value) return@write
        val storedDraft = _moveDraft.value ?: return@write
        if (focus.session.value.phase != FocusPhase.IDLE) { stashMove(); focus.requestSessionPanel(); return@write }
        val minutes = MoveDuration.parse(_moveMinutesText.value)
        if (minutes == null) { _message.value = "Enter 1–180 whole minutes before placing this move."; return@write }
        val draft = storedDraft.copy(minutes = minutes)
        if (draft.title.isBlank()) return@write
        _message.value = null
        if (draft.sourceType == "PREVIEW") {
            finishPreviewDraft()
            focus.start(draft.title, sourceType = "PREVIEW", material = draft.material, firstStep = draft.firstStep, minutes = draft.minutes)
            focus.requestSessionPanel()
            return@write
        }
        _journeyBusy.value = true
        try {
            var placed: DayBlock? = null
            db.withTransaction {
                if (!sourceAvailable(draft)) { _message.value = "This work changed. Choose another next move."; return@withTransaction }
                val original = if (draft.blockId.isNotBlank()) dao.blockById(draft.blockId) else null
                if (draft.blockId.isNotBlank() && (original == null || original.fixed || original.state != "PLANNED")) {
                    _message.value = "This block changed. Your current day is still here."; return@withTransaction
                }
                val now = System.currentTimeMillis()
                val slot = DayPlanner.nextWindow(now, dayEnd(now), draft.minutes, occupied(now, setOf(draft.blockId)))
                if (slot == null) { _message.value = "That doesn't fit today. Try fewer minutes or use Plan for tomorrow."; return@withTransaction }
                if (start && slot > now + 1000L) {
                    _message.value = "That window just changed. Review the new time before saving."; return@withTransaction
                }
                if (focus.session.value.phase != FocusPhase.IDLE) {
                    _message.value = "Another session started. Your draft is kept; return to the capsule."; return@withTransaction
                }
                var type = draft.sourceType
                var source = draft.sourceId
                if (type == "CUSTOM") {
                    source = db.taskDao().insertTask(TaskItem(title = draft.title.trim(), quadrant = Quadrant.Q2_SCHEDULE, category = "General")).toString()
                    type = "TASK"
                }
                val block = DayBlock(original?.id ?: UUID.randomUUID().toString(), draft.title.trim(), slot, draft.minutes,
                    sourceType = type, sourceId = source, material = draft.material.trim(), firstStep = draft.firstStep.trim())
                dao.saveBlock(block)
                placed = block
                _undo.value = emptyList()
            }
            placed?.let { block ->
                _moveDraft.value = null; prefs.edit().remove("move_draft").apply()
                _moveMinutesText.value = ""
                val now = System.currentTimeMillis()
                if (start && block.startMillis <= now + 1000L) {
                    focus.start(block.title, if (block.sourceType == "REST") FocusKind.REST else FocusKind.STUDY,
                        block.sourceType, block.sourceId, block.material, block.firstStep, block.id, block.durationMinutes)
                    focus.requestSessionPanel()
                } else _message.value = "Saved in your day. The next move is ready when its window arrives."
            }
        } finally { _journeyBusy.value = false }
    }

    fun completeSource(record: WorkSessionRecord) = write {
        db.withTransaction {
            when (record.sourceType) {
                "TASK" -> {
                    val item = db.taskDao().taskById(record.sourceId.toLongOrNull() ?: -1)
                    if (item == null) { _message.value = "That task is no longer in storage."; return@withTransaction }
                    db.taskDao().updateStatus(item.id, TaskStatus.COMPLETED)
                }
                "ASSIGNMENT" -> {
                    val item = db.academicDao().assignmentById(record.sourceId.toLongOrNull() ?: -1)
                    if (item == null) { _message.value = "That deadline is no longer in storage."; return@withTransaction }
                    db.academicDao().updateAssignment(item.copy(isSubmitted = true))
                }
                "PROJECT_STEP" -> {
                    val item = dao.stepById(record.sourceId)
                    if (item == null) { _message.value = "That milestone is no longer in storage."; return@withTransaction }
                    dao.saveStep(item.copy(done = true))
                }
                else -> return@withTransaction
            }
            dao.sourcePlanState(record.sourceType, record.sourceId, "DONE")
            _message.value = "Marked complete. Your session remains in Review."
        }
    }

    private fun write(action: suspend () -> Unit) = viewModelScope.launch {
        try {
            writes.withLock { action() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _message.value = "Could not save. Your current records are still here; try again."
        }
    }
    fun saveProject(project: WorkProject) = write { dao.saveProject(project) }
    fun saveStep(step: ProjectStep) = write {
        db.withTransaction {
            dao.saveStep(step)
            dao.sourcePlanState("PROJECT_STEP", step.id, if (step.done) "DONE" else "PLANNED")
        }
    }
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
        academicInitialization.withLock {
            withContext(Dispatchers.IO) { academicStore.clearRoadmap() }
            withContext(Dispatchers.IO) { projectQueueStore.retainWipeIntent(); entryStore.clearDraft() }
            _academicRoadmap.value = AcademicRoadmap()
            _entryDraft.value = EntryDraft(); _entryResult.value = null; _entryOpen.value = false
        }
        focus.clearForUserWipe()
        dao.clearWorkspace()
        _homeAt.value = 0L; _undo.value = emptyList()
        _moveDraft.value = null
        _moveMinutesText.value = ""
        prefs.edit().remove("home_at").remove("move_draft").remove("draft_before_preview").apply()
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
        val classes = db.academicDao().timetableSnapshot().filter { it.dayOfWeek == weekday }.map { entry ->
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
        val active = if (includeSession && activeEnd > now && focusPrefs.getString("source_type", "") != "PREVIEW") listOf(now to activeEnd) else emptyList()
        val records = DayInputs(day, tasks = db.taskDao().tasksSnapshot(), assignments = db.academicDao().assignmentsSnapshot(),
            topics = dao.topicsSnapshot(), attempts = dao.attemptsSnapshot(), projects = dao.projectsSnapshot(), steps = dao.stepsSnapshot())
        return classes + active + dao.blocksSnapshot().filter { it.id !in omit && (it.fixed || it.state == "PLANNED" && DayGuide.available(records, it.sourceType, it.sourceId)) }
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
            val overdue = dao.blocksSnapshot().filter {
                !it.fixed && it.state == "PLANNED" && it.startMillis in dayStart(from) until from && it.id != activeBlockId && sourceAvailable(DayGuide.fromBlock(it))
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
            val current = dao.blocksSnapshot()
            if (recovered.all { saved -> current.firstOrNull { it.id == saved.id } == saved }) {
                dao.saveBlocks(_undo.value)
                _message.value = "Previous block times restored."
            } else _message.value = "The plan changed since recovery. Edit the affected blocks instead."
            _undo.value = emptyList()
        }
    }

    fun resumeSession(focus: TodayPrototypeViewModel) = write {
        _message.value = null
        val session = focus.session.value
        if (session.phase != FocusPhase.PAUSED) return@write
        if (session.sourceType == "PREVIEW") { focus.resume(); return@write }
        val now = System.currentTimeMillis()
        val end = now + session.pausedSeconds * 1000L
        if (end > dayEnd(now) || occupied(now, setOf(session.blockId), includeSession = false).any { now < it.second && end > it.first }) {
            _message.value = "Continuing now would overlap your plan or a saved class. Keep this paused, or save it and plan a smaller continuation."
        } else focus.resume()
    }

    fun begin(focus: TodayPrototypeViewModel, title: String, sourceType: String = "CUSTOM", sourceId: String = "",
              material: String = "", firstStep: String = "", blockId: String = "", minutes: Int = 25, kind: FocusKind = FocusKind.STUDY) = write {
        _message.value = null
        if (focus.session.value.phase != FocusPhase.IDLE) { focus.requestSessionPanel(); return@write }
        if (minutes !in 1..180 || !sourceAvailable(DayMove(title, sourceType, sourceId))) {
            _message.value = "That work changed. Choose a current action and 1–180 minutes."; return@write
        }
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
