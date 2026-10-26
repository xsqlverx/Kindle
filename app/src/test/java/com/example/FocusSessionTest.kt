package com.example

import com.example.data.model.FocusPhase
import com.example.data.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusSessionTest {
    @Test
    fun `returning after an expired deadline shows zero rather than a fresh timer`() {
        val session = FocusSession(phase = FocusPhase.RUNNING, deadlineMillis = 100_000L)
        assertEquals(0, session.remainingSeconds(150_000L))
    }

    @Test
    fun `time spent outside the screen counts toward a running session`() {
        val session = FocusSession(phase = FocusPhase.RUNNING, deadlineMillis = 1_500_000L)
        assertEquals(600, session.remainingSeconds(900_000L))
    }

    @Test
    fun `a partial second remains visible until the deadline`() {
        val session = FocusSession(phase = FocusPhase.RUNNING, deadlineMillis = 2_000L)
        assertEquals(1, session.remainingSeconds(1_999L))
        assertEquals(0, session.remainingSeconds(2_000L))
    }

    @Test
    fun `paused sessions do not count down while away`() {
        val session = FocusSession(phase = FocusPhase.PAUSED, pausedSeconds = 480)
        assertEquals(480, session.remainingSeconds(0L))
        assertEquals(480, session.remainingSeconds(86_400_000L))
    }

    @Test
    fun `moving the device clock backward cannot inflate the original duration`() {
        val session = FocusSession(phase = FocusPhase.RUNNING, deadlineMillis = 3_000_000L)
        assertEquals(1_500, session.remainingSeconds(0L))
    }
}
