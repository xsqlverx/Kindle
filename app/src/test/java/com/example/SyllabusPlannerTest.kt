package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

/** External cloud runner only. Planning suggestions are separate from actual work records. */
class SyllabusPlannerTest {
    private fun item(id: String, course: String = "math", scope: String = "THEORY", module: String = "$course:m1") =
        SyllabusItem(id, course, "Module 1", module, 9, 1, scope, id, "Source content", course, "Suggested practice")
    private val items = listOf(item("aisat26:math:a"), item("aisat26:math:b"), item("aisat26:python:a", "python"),
        item("aisat26:python:lab", "python", "CODE"), item("aisat26:math:optional", scope = "OPTIONAL"),
        item("aisat26:life_skills:portfolio", "life_skills", "ACTIVITY"))
    private val pack = AcademicPack("pack", "S1", "2026-12-06", emptyList(),
        listOf("math", "python", "life_skills").map { SyllabusCourse(it, it, it, it, "3:0:0:0", 3, "", "") }, items,
        listOf(AcademicEvent("internal", "Internal", 1, "EXAM", listOf("2026-11-13", "2026-11-16"), 1),
            AcademicEvent("final", "Final", 1, "EXAM", listOf("2026-12-07", "2026-12-09"), 1)), emptyList())
    private val topics = items.filter { it.scope != "CODE" }.map { ExamTopic(it.id, it.course, it.title, it.material) }

    @Test fun `budget is exact without inventing personal times or excluded portions`() {
        for (budget in listOf(10, 37, 50, 79, 180)) {
            val result = SyllabusPlanner.build(pack, "2026-10-30", budget)
            assertEquals("2026-10-30", result.first().date)
            assertEquals("2026-12-06", result.last().date)
            result.groupBy { it.date }.values.forEach { day -> assertEquals(budget, day.sumOf { it.minutes }) }
            assertTrue(result.all { target -> items.first { it.id == target.itemId }.scope in setOf("THEORY", "ACTIVITY") })
            assertFalse(result.any { it.itemId.endsWith(":optional") || it.itemId.endsWith(":lab") })
            assertEquals(result.size, result.map { it.id }.distinct().size)
        }
    }
    @Test fun `courses rotate and covered theory becomes explicit recall`() {
        val result = SyllabusPlanner.build(pack, "2026-12-04", 50)
        assertEquals(listOf("math", "python", "math"), result.filter { it.mode == "FIRST PASS" }.take(3).map { target -> items.first { it.id == target.itemId }.course })
        val reviewed = SyllabusPlanner.build(pack, "2026-12-04", 50, items.map { it.id }.toSet())
        assertTrue(reviewed.all { it.mode == "RECALL" })
    }
    @Test fun `confirmed module filter narrows theory while retaining the separate portfolio audit`() {
        val result = SyllabusPlanner.build(pack, "2026-12-04", 50, modules = setOf("python:m1"))
        assertTrue(result.filter { it.mode != "PORTFOLIO" }.all { it.itemId == "aisat26:python:a" })
        assertTrue(result.any { it.mode == "PORTFOLIO" })
    }
    @Test fun `outdated S1 preparation does not restart after the preparation end`() {
        assertTrue(SyllabusPlanner.build(pack, "2027-01-04", 50).isEmpty())
        val old = AcademicRoadmap(SyllabusPlanner.build(pack, "2026-10-30", 50))
        assertTrue(SyllabusPlanner.next(pack, old, AcademicDates.millis("2026-12-07"), topics, emptyList()).isEmpty())
        assertNull(pack.nextExam("2026-12-10"))
        assertEquals("internal", pack.nextExam("2026-11-14")!!.id) // a window already in progress
        assertEquals("final", pack.nextExam("2026-11-17")!!.id)
    }
    @Test fun `overdue completion coverage and deleted source affect the next suggestion`() {
        val targets = listOf(AcademicTarget("one", "2026-10-29", items[0].id, 37, "FIRST PASS"),
            AcademicTarget("two", "2026-10-30", items[1].id, 13, "FIRST PASS"))
        val now = AcademicDates.millis("2026-10-30")
        val plan = AcademicRoadmap(targets)
        assertEquals(37, SyllabusPlanner.next(pack, plan, now, topics, emptyList()).first().minutes)
        val edited = topics.map { if (it.id == items[1].id) it.copy(title = "My label", material = "My notes p12") else it }
        val moves = SyllabusPlanner.next(pack, plan.copy(done = setOf("one")), now, edited, emptyList())
        assertEquals(1, moves.size)
        assertTrue(moves.single().title.contains("My label"))
        assertEquals("My notes p12", moves.single().material)
        assertTrue(SyllabusPlanner.next(pack, plan.copy(covered = targets.map { it.itemId }.toSet()), now, topics, emptyList()).isEmpty())
        assertTrue(SyllabusPlanner.next(pack, plan, now, emptyList(), emptyList()).isEmpty())
    }
    @Test fun `urgent work outranks automatic syllabus targets without flooding suggestions`() {
        val now = AcademicDates.millis("2026-10-30")
        val move = DayMove("Suggested math", "TOPIC", items[0].id, items[0].material)
        val input = DayInputs(now, topics = topics, academicMoves = listOf(move), tasks = listOf(TaskItem(id = 9, title = "Urgent", quadrant = Quadrant.Q1_DO)))
        val choices = DayGuide.choices(input)
        assertEquals("TASK", choices.first().sourceType)
        assertEquals(2, choices.size)
        assertTrue(DayGuide.choices(input.copy(tasks = emptyList(), academicMoves = emptyList())).isEmpty())
        assertEquals(topics.size, DayGuide.library(input.copy(tasks = emptyList(), academicMoves = emptyList())).size)
    }
    @Test fun `institution dates reject impossible keys and stay independent of device timezone`() {
        val previous = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals("2026-11-01", AcademicDates.plus("2026-10-31", 1))
            assertEquals("2026-10-30", AcademicDates.key(AcademicDates.millis("2026-10-30")))
            assertTrue(runCatching { AcademicDates.millis("2026-02-30") }.isFailure)
            assertTrue(runCatching { SyllabusPlanner.build(pack, "2026-10-30", 0) }.isFailure)
        } finally { java.util.TimeZone.setDefault(previous) }
    }
}
