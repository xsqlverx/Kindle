package com.example

import com.example.data.model.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Cloud runner only. Exercises the decision rules behind the actual daily UI. */
class DayGuideTest {
    private lateinit var originalZone: TimeZone
    @Before fun zone() { originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata")) }
    @After fun restore() { TimeZone.setDefault(originalZone) }
    private fun at(hour: Int, minute: Int = 0) = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 27, hour, minute, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val task get() = TaskItem(id = 7, title = "Revise recursion", isTopThree = true)

    @Test fun `empty storage asks for one real action and never constructs sample work`() {
        val input = DayInputs(at(10))
        assertEquals(DayStage.FIRST_STEP, DayGuide.cue(input).stage)
        assertTrue(DayGuide.choices(input).isEmpty())
        assertTrue(input.tasks.isEmpty())
        assertTrue(input.blocks.isEmpty())
    }
    @Test fun `arrival is a prompt until an explicit dated check-in exists`() {
        val input = DayInputs(at(15), tasks = listOf(task))
        assertEquals(DayStage.ARRIVE, DayGuide.cue(input).stage)
        assertEquals(DayStage.READY, DayGuide.cue(input.copy(homeAt = at(14, 50))).stage)
        assertEquals(DayStage.ARRIVE, DayGuide.cue(input.copy(homeAt = at(14, 50) - 86_400_000)).stage)
    }
    @Test fun `a running or paused session stays ahead of another proposed task`() {
        val input = DayInputs(at(15), tasks = listOf(task), session = FocusSession(title = "Actual work", phase = FocusPhase.PAUSED))
        assertEquals(DayStage.FOCUS, DayGuide.cue(input).stage)
        assertEquals("Actual work", DayGuide.cue(input).detail)
        assertEquals(DayStage.FOCUS, DayGuide.cue(input.copy(session = input.session.copy(phase = FocusPhase.RUNNING))).stage)
    }
    @Test fun `expiry requests a resume point without completing or replacing the task`() {
        val input = DayInputs(at(15), tasks = listOf(task), session = FocusSession(phase = FocusPhase.FINISHED, sourceType = "TASK", sourceId = "7"))
        assertEquals(DayStage.FINISH, DayGuide.cue(input).stage)
        assertEquals(TaskStatus.PENDING, input.tasks.single().status)
    }
    @Test fun `a saved class protects its time and provides a concrete route`() {
        val entry = TimetableEntry(1, "CS", "Data structures", Calendar.TUESDAY, 10, 0, 11, 0, "Lab")
        val input = DayInputs(at(10, 15), tasks = listOf(task), timetable = listOf(entry))
        val cue = DayGuide.cue(input)
        assertEquals(DayStage.CLASS, cue.stage)
        assertEquals(Destination.COLLEGE, cue.anchor!!.destination)
        assertEquals(at(11), DayGuide.window(input, 25))
    }
    @Test fun `the window cannot stretch through a fixed evening commitment`() {
        val input = DayInputs(at(19, 50), blocks = listOf(DayBlock("gym", "Gym", at(20), 60, fixed = true)))
        assertEquals(at(21), DayGuide.window(input, 25))
        assertEquals(at(19, 50), DayGuide.window(input, 10))
    }
    @Test fun `arbitrary entered minutes fit exactly or move past a commitment without preset rounding`() {
        val input = DayInputs(at(19), blocks = listOf(DayBlock("fixed", "Fixed commitment", at(19, 20), 40, fixed = true)))
        val seventeen = requireNotNull(MoveDuration.parse("17"))
        val thirtySeven = requireNotNull(MoveDuration.parse("37"))
        assertEquals(at(19), DayGuide.window(input, seventeen))
        assertEquals(at(19, 17), requireNotNull(DayGuide.window(input, seventeen)) + seventeen * 60_000L)
        assertEquals(at(20), DayGuide.window(input, thirtySeven))
        assertEquals(at(20, 37), requireNotNull(DayGuide.window(input, thirtySeven)) + thirtySeven * 60_000L)
    }
    @Test fun `a smaller restart keeps the original source and material`() {
        val block = DayBlock("study", task.title, at(14), 25, sourceType = "TASK", sourceId = "7", material = "Notes p12", firstStep = "Example 3")
        val input = DayInputs(at(15), homeAt = at(14), tasks = listOf(task), blocks = listOf(block))
        val cue = DayGuide.cue(input)
        assertEquals(DayStage.RECOVER, cue.stage)
        val move = requireNotNull(cue.move)
        assertEquals(10, move.minutes)
        assertEquals("study", move.blockId)
        assertEquals("7", move.sourceId)
        assertEquals("Notes p12", move.material)
        assertEquals("Example 3", move.firstStep)
    }
    @Test fun `completed deleted and archived work cannot become the next move`() {
        val project = WorkProject("project", "Build", "A useful build", archived = true)
        val step = ProjectStep("step", project.id, "Ship it", 0)
        val orphan = DayBlock("old", "Deleted task", at(9), 25, sourceType = "TASK", sourceId = "999")
        val input = DayInputs(at(10), tasks = listOf(task.copy(status = TaskStatus.COMPLETED)), projects = listOf(project), steps = listOf(step), blocks = listOf(orphan))
        assertTrue(DayGuide.choices(input).isEmpty())
        assertEquals(DayStage.FIRST_STEP, DayGuide.cue(input).stage)
        assertEquals(at(10), DayGuide.window(input, 25))
    }
    @Test fun `a real due deadline outranks an undated priority with an explanation`() {
        val assignment = AssignmentItem(id = 4, courseCode = "CS", title = "Lab submission", dueDateMillis = at(17))
        val choices = DayGuide.choices(DayInputs(at(10), tasks = listOf(task), assignments = listOf(assignment)))
        assertEquals("ASSIGNMENT", choices.first().sourceType)
        assertTrue(choices.first().reason.contains("deadline"))
    }
    @Test fun `saved continuation material stays attached to the original task`() {
        val record = WorkSessionRecord("saved", task.title, "TASK", "7", "", "Chapter 2", "Continue example 3", at(9), at(9, 25), 1500, "Timer elapsed")
        val move = DayGuide.choices(DayInputs(at(10), tasks = listOf(task), history = listOf(record))).single()
        assertEquals("7", move.sourceId)
        assertEquals("Chapter 2", move.material)
        assertEquals("Continue example 3", move.firstStep)
    }
    @Test fun `active unplanned work also occupies time in the proposed window`() {
        val input = DayInputs(at(15), session = FocusSession(phase = FocusPhase.RUNNING, deadlineMillis = at(15, 20)))
        assertEquals(at(15, 20), DayGuide.window(input, 10))
    }
    @Test fun `the explicitly separate preview does not reserve real planning time`() {
        val input = DayInputs(at(15), session = FocusSession(phase = FocusPhase.RUNNING, deadlineMillis = at(15, 20), sourceType = "PREVIEW"))
        assertEquals(at(15), DayGuide.window(input, 10))
        assertTrue(input.tasks.isEmpty())
        assertTrue(input.blocks.isEmpty())
        assertTrue(input.history.isEmpty())
    }
    @Test fun `future revision stays out of suggestions while remaining manually reachable`() {
        val topic = ExamTopic("topic", "CS", "Stacks", "Notes", revisionAt = at(12) + 86_400_000L)
        val input = DayInputs(at(10), topics = listOf(topic))
        assertTrue(DayGuide.choices(input).isEmpty())
        assertEquals("topic", DayGuide.library(input).single().sourceId)
    }
    @Test fun `blocked projects and drop quadrant do not create automatic busywork`() {
        val project = WorkProject("project", "Build", "Ship", blocker = "Waiting for lab hardware")
        val step = ProjectStep("step", project.id, "Test device", 0)
        val input = DayInputs(at(10), projects = listOf(project), steps = listOf(step), tasks = listOf(task.copy(quadrant = Quadrant.Q4_ELIMINATE)))
        assertTrue(DayGuide.choices(input).isEmpty())
        assertEquals(2, DayGuide.library(input).size)
    }
    @Test fun `a saved night reset is acknowledged rather than presented as undone`() {
        val input = DayInputs(at(23), resetDone = true)
        assertEquals(DayStage.CLOSE, DayGuide.cue(input).stage)
        assertTrue(DayGuide.cue(input).detail.contains("saved"))
    }
}
