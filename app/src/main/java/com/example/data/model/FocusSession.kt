package com.example.data.model

enum class FocusPhase { IDLE, RUNNING, PAUSED, FINISHED }

enum class FocusKind { STUDY, REST }

/** A deadline-based timer. No background polling or alarm delivery is implied. */
data class FocusSession(
    val title: String = "",
    val kind: FocusKind = FocusKind.STUDY,
    val phase: FocusPhase = FocusPhase.IDLE,
    val durationSeconds: Int = 25 * 60,
    val pausedSeconds: Int = 25 * 60,
    val deadlineMillis: Long = 0L
) {
    val isActive: Boolean
        get() = phase == FocusPhase.RUNNING || phase == FocusPhase.PAUSED

    fun remainingSeconds(nowMillis: Long): Int = when (phase) {
        FocusPhase.RUNNING -> {
            val remaining = (deadlineMillis - nowMillis).coerceAtLeast(0L)
            ((remaining + 999L) / 1000L).coerceAtMost(durationSeconds.toLong()).toInt()
        }
        FocusPhase.PAUSED -> pausedSeconds.coerceIn(0, durationSeconds)
        FocusPhase.FINISHED -> 0
        FocusPhase.IDLE -> durationSeconds
    }
}
