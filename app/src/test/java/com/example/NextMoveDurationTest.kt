package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.viewmodel.TodayPrototypeViewModel
import com.example.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** External cloud runner only; no local Android tests executed. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NextMoveDurationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var sequence = 0
    private fun provider() = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(app))
    private fun workspace() = provider().get("workspace${sequence++}", WorkspaceViewModel::class.java)
    private fun focus() = provider().get("focus${sequence++}", TodayPrototypeViewModel::class.java)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        app.getSharedPreferences("workspace_v2", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("today_focus_session", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun `exact custom minutes survive stale metadata callbacks source changes and reopening`() {
        val model = workspace()
        model.offerMove(DayMove("Read the chapter"))
        val old = model.moveDraft.value!!
        model.updateMoveMinutes("37")
        model.updateMove(old.copy(title = "Read chapter three", sourceType = "TASK", sourceId = "7"))
        assertEquals("37", model.moveMinutesText.value)
        assertEquals(37, model.moveDraft.value!!.minutes)
        assertEquals("7", model.moveDraft.value!!.sourceId)
        model.stashMove(); model.offerMove()
        assertEquals("37", model.moveMinutesText.value)
        val recreated = workspace()
        assertEquals("37", recreated.moveMinutesText.value)
        assertEquals(37, recreated.moveDraft.value!!.minutes)
        assertEquals("Read chapter three", recreated.moveDraft.value!!.title)
        val stale = recreated.moveDraft.value!!
        recreated.closeMove()
        recreated.updateMove(stale.copy(material = "Late keyboard callback"))
        assertNull(recreated.moveDraft.value)
        assertEquals("", recreated.moveMinutesText.value)
    }

    @Test fun `blank decimal zero overflow and nonnumeric durations cannot start the previous valid preview`() = runTest {
        val model = workspace(); val timer = focus()
        model.offerMove(DayMove("Try this", "PREVIEW", minutes = 25))
        for (text in listOf("", "0", "181", "17.5", "1e2", "-3", "999999999999")) {
            model.updateMoveMinutes(text)
            model.placeMove(timer, start = true)
            runCurrent()
            assertEquals(FocusPhase.IDLE, timer.session.value.phase)
            assertEquals(text, model.moveMinutesText.value)
            assertTrue(model.message.value.orEmpty().contains("whole minutes"))
            assertNotNull(model.moveDraft.value)
        }
        model.updateMoveMinutes("17")
        model.placeMove(timer, start = true)
        runCurrent()
        assertEquals(FocusPhase.RUNNING, timer.session.value.phase)
        assertEquals(17 * 60, timer.session.value.durationSeconds)
        assertEquals("PREVIEW", timer.session.value.sourceType)
    }

    @Test fun `invalid raw draft restores without silently accepting its last valid number`() {
        val model = workspace()
        model.offerMove(DayMove("Revise pointers"))
        model.updateMoveMinutes("17")
        model.updateMoveMinutes("")
        model.updateMove(model.moveDraft.value!!.copy(material = "Notes p4"))
        val restored = workspace()
        assertEquals("", restored.moveMinutesText.value)
        assertNull(MoveDuration.parse(restored.moveMinutesText.value))
        assertEquals("Notes p4", restored.moveDraft.value!!.material)
        assertEquals(17, restored.moveDraft.value!!.minutes) // retained draft only, never valid input
    }

    @Test fun `preview keeps the real custom duration source and material in its backup`() {
        val model = workspace()
        model.offerMove(DayMove("Fix the bug", "TASK", "42", material = "Issue 7"))
        model.updateMoveMinutes("43")
        model.offerMove(DayMove("Try the loop", "PREVIEW", minutes = 1))
        model.updateMoveMinutes("7")
        model.stashMove(); model.offerMove()
        assertEquals("43", model.moveMinutesText.value)
        assertEquals(43, model.moveDraft.value!!.minutes)
        assertEquals("42", model.moveDraft.value!!.sourceId)
        assertEquals("Issue 7", model.moveDraft.value!!.material)
    }

    @Test fun `legacy draft without raw text restores its exact nonpreset duration`() {
        app.getSharedPreferences("workspace_v2", Context.MODE_PRIVATE).edit()
            .putString("move_draft", """{"title":"Legacy action","minutes":53,"type":"CUSTOM"}""").commit()
        val model = workspace()
        assertEquals("53", model.moveMinutesText.value)
        assertEquals(53, model.moveDraft.value!!.minutes)
        model.updateMoveMinutes("180")
        assertEquals(180, model.moveDraft.value!!.minutes)
        model.updateMoveMinutes("1")
        assertEquals(1, model.moveDraft.value!!.minutes)
    }
}
