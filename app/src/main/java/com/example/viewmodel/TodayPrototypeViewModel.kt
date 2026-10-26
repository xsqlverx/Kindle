package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.data.model.FocusKind
import com.example.data.model.FocusPhase
import com.example.data.model.FocusSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The first-look timer is independent of workout and Daily Reset timers. */
class TodayPrototypeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(readSession())
    val session = _session.asStateFlow()

    init {
        refresh()
    }

    fun start(title: String, kind: FocusKind = FocusKind.STUDY) {
        if (_session.value.isActive) return
        val seconds = if (kind == FocusKind.REST) 20 * 60 else 25 * 60
        save(
            FocusSession(
                title = title.trim().ifBlank { "Review today's notes" },
                kind = kind,
                phase = FocusPhase.RUNNING,
                durationSeconds = seconds,
                pausedSeconds = seconds,
                deadlineMillis = System.currentTimeMillis() + seconds * 1000L
            )
        )
    }

    fun pause() {
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
        val current = _session.value
        if (current.phase != FocusPhase.PAUSED) return
        save(current.copy(
            phase = FocusPhase.RUNNING,
            deadlineMillis = System.currentTimeMillis() + current.pausedSeconds * 1000L
        ))
    }

    fun refresh() {
        val current = _session.value
        if (current.phase == FocusPhase.RUNNING && current.remainingSeconds(System.currentTimeMillis()) == 0) {
            save(current.copy(phase = FocusPhase.FINISHED, pausedSeconds = 0, deadlineMillis = 0L))
        }
    }

    fun dismiss() = save(FocusSession())

    private fun save(value: FocusSession) {
        _session.value = value
        preferences.edit()
            .putString("title", value.title)
            .putString("kind", value.kind.name)
            .putString("phase", value.phase.name)
            .putInt("duration", value.durationSeconds)
            .putInt("remaining", value.pausedSeconds)
            .putLong("deadline", value.deadlineMillis)
            .apply()
    }

    private fun readSession(): FocusSession {
        val phase = FocusPhase.entries.firstOrNull { it.name == preferences.getString("phase", "") }
            ?: FocusPhase.IDLE
        val kind = FocusKind.entries.firstOrNull { it.name == preferences.getString("kind", "") }
            ?: FocusKind.STUDY
        val duration = preferences.getInt("duration", 25 * 60).coerceIn(1, 60 * 60)
        return FocusSession(
            title = preferences.getString("title", "").orEmpty(),
            kind = kind,
            phase = phase,
            durationSeconds = duration,
            pausedSeconds = preferences.getInt("remaining", duration).coerceIn(0, duration),
            deadlineMillis = preferences.getLong("deadline", 0L)
        )
    }
}
