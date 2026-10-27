package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DatabaseProvider
import com.example.data.model.WorkSessionRecord
import com.example.data.reminders.PlanReminders
import com.example.data.model.FocusKind
import com.example.data.model.FocusPhase
import com.example.data.model.FocusSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** The first-look timer is independent of workout and Daily Reset timers. */
class TodayPrototypeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(readSession())
    val session = _session.asStateFlow()
    private val _openSessionPanel = MutableStateFlow(false)
    val openSessionPanel = _openSessionPanel.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()

    fun requestSessionPanel() { _openSessionPanel.value = true }
    fun consumeSessionPanelRequest() { _openSessionPanel.value = false }

    init {
        if (_session.value.phase != FocusPhase.IDLE && _session.value.id.isBlank()) {
            save(_session.value.copy(id = UUID.randomUUID().toString()))
        }
        refresh()
    }

    fun start(title: String, kind: FocusKind = FocusKind.STUDY, sourceType: String = "CUSTOM", sourceId: String = "",
              material: String = "", firstStep: String = "", blockId: String = "", minutes: Int = 25) {
        if (_session.value.phase != FocusPhase.IDLE || _saving.value) return
        val seconds = if (kind == FocusKind.REST) 20 * 60 else minutes.coerceIn(5, 180) * 60
        save(
            FocusSession(
                title = title.trim().ifBlank { "Review today's notes" },
                kind = kind,
                phase = FocusPhase.RUNNING,
                durationSeconds = seconds,
                pausedSeconds = seconds,
                deadlineMillis = System.currentTimeMillis() + seconds * 1000L,
                id = UUID.randomUUID().toString(), startedAt = System.currentTimeMillis(),
                sourceType = if (kind == FocusKind.REST) "REST" else sourceType, sourceId = sourceId, material = material,
                firstStep = firstStep, blockId = blockId
            )
        )
    }

    fun pause() {
        if (_saving.value) return
        val current = _session.value
        if (current.phase != FocusPhase.RUNNING) return
        val remaining = current.remainingSeconds(System.currentTimeMillis())
        save(current.copy(
            phase = if (remaining == 0) FocusPhase.FINISHED else FocusPhase.PAUSED,
            pausedSeconds = remaining,
            deadlineMillis = 0L
        ))
    }

    fun resume() {
        if (_saving.value) return
        val current = _session.value
        if (current.phase != FocusPhase.PAUSED) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val dao = DatabaseProvider.get(getApplication()).workspaceDao()
                if (dao.sessionById(current.id) != null) {
                    dao.updateSessionNote(current.id, current.resumeNote)
                    save(FocusSession())
                } else save(current.copy(
                    phase = FocusPhase.RUNNING,
                    deadlineMillis = System.currentTimeMillis() + current.pausedSeconds * 1000L
                ))
            } catch (_: Exception) { _saveError.value = "Could not check session history. Your paused session is retained; try again." }
            finally { _saving.value = false }
        }
    }

    fun refresh() {
        val current = _session.value
        if (current.phase == FocusPhase.RUNNING && current.remainingSeconds(System.currentTimeMillis()) == 0) {
            save(current.copy(phase = FocusPhase.FINISHED, pausedSeconds = 0, deadlineMillis = 0L))
        }
    }

    fun updateResumeNote(note: String) { if (!_saving.value) save(_session.value.copy(resumeNote = note)) }
    fun clearForUserWipe() {
        check(!_saving.value)
        save(FocusSession())
    }

    /** History and linked block review commit together before clearing the persisted timer. */
    fun dismiss() {
        if (_saving.value || _session.value.phase == FocusPhase.IDLE) return
        if (_session.value.phase == FocusPhase.RUNNING) pause()
        val current = _session.value
        _saving.value = true
        _saveError.value = null
        viewModelScope.launch {
            try {
                DatabaseProvider.get(getApplication()).workspaceDao().finish(WorkSessionRecord(
                    id = current.id.ifBlank { UUID.randomUUID().toString() }, title = current.title,
                    sourceType = current.sourceType, sourceId = current.sourceId, blockId = current.blockId,
                    material = current.material, resumeNote = current.resumeNote, startedAt = current.startedAt,
                    endedAt = System.currentTimeMillis(), elapsedSeconds = current.durationSeconds - current.remainingSeconds(System.currentTimeMillis()),
                    outcome = if (current.phase == FocusPhase.FINISHED) "Timer elapsed" else "Stopped early"
                ))
                save(FocusSession())
            } catch (_: Exception) { _saveError.value = "History could not be saved. Your session and note are retained. Try again." }
            finally { _saving.value = false }
        }
    }

    private fun save(value: FocusSession) {
        val previous = _session.value
        _session.value = value
        preferences.edit()
            .putString("title", value.title)
            .putString("kind", value.kind.name)
            .putString("phase", value.phase.name)
            .putInt("duration", value.durationSeconds)
            .putInt("remaining", value.pausedSeconds)
            .putLong("deadline", value.deadlineMillis)
            .putString("id", value.id).putLong("started_at", value.startedAt)
            .putString("source_type", value.sourceType).putString("source_id", value.sourceId)
            .putString("block_id", value.blockId).putString("material", value.material)
            .putString("first_step", value.firstStep).putString("resume_note", value.resumeNote)
            .apply()
        // Editing a resume note does not poll in the background. One alarm tracks the deadline.
        if (previous.phase != value.phase || previous.deadlineMillis != value.deadlineMillis) {
            try { PlanReminders.syncSession(getApplication()) }
            catch (_: SecurityException) { _saveError.value = "Session saved. Android could not schedule its alert; return here to review the timer." }
        }
    }

    private fun readSession(): FocusSession {
        val phase = FocusPhase.entries.firstOrNull { it.name == preferences.getString("phase", "") }
            ?: FocusPhase.IDLE
        val kind = FocusKind.entries.firstOrNull { it.name == preferences.getString("kind", "") }
            ?: FocusKind.STUDY
        val duration = preferences.getInt("duration", 25 * 60).coerceIn(1, 180 * 60)
        return FocusSession(
            title = preferences.getString("title", "").orEmpty(),
            kind = kind,
            phase = phase,
            durationSeconds = duration,
            pausedSeconds = preferences.getInt("remaining", duration).coerceIn(0, duration),
            deadlineMillis = preferences.getLong("deadline", 0L),
            id = preferences.getString("id", "").orEmpty(), startedAt = preferences.getLong("started_at", 0L),
            sourceType = preferences.getString("source_type", "CUSTOM").orEmpty(), sourceId = preferences.getString("source_id", "").orEmpty(),
            blockId = preferences.getString("block_id", "").orEmpty(), material = preferences.getString("material", "").orEmpty(),
            firstStep = preferences.getString("first_step", "").orEmpty(), resumeNote = preferences.getString("resume_note", "").orEmpty()
        )
    }
}
