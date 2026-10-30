package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

/** External cloud runner only. These protect user input and prevent invented values. */
class QuickEntryRulesTest {
    private fun draft(kind: EntryKind, title: String) = EntryDraft(id = "draft", kind = kind).edit("title", title)

    @Test fun `a single title is enough for a task project note topic and coding problem`() {
        for (kind in listOf(EntryKind.TASK, EntryKind.PROJECT, EntryKind.NOTE, EntryKind.TOPIC, EntryKind.CODING)) {
            assertNull(EntryRules.error(draft(kind, "A real thing")))
            assertNotNull(EntryRules.error(draft(kind, "  ")))
        }
    }
    @Test fun `github project links produce a readable name while retaining a valid source`() {
        val link = "https://github.com/xsqlverx/Kindle.git"
        assertEquals("xsqlverx/Kindle", EntryRules.repository(link))
        assertEquals("Kindle", EntryRules.projectTitle(link))
        assertEquals("Make a zine", EntryRules.projectTitle(" Make a zine "))
        assertEquals("Physics/Math", EntryRules.projectTitle("Physics/Math"))
        assertNotNull(EntryRules.error(draft(EntryKind.PROJECT, "Project").edit("link", "not a link")))
        assertNull(EntryRules.error(draft(EntryKind.PROJECT, "Project").edit("link", "https://example.org/project")))
        assertEquals(listOf("First", "Second", "First"), EntryRules.actions(" First \n- Second\nFirst\n"))
    }
    @Test fun `class has an explicit weekday and valid ordered start and end`() {
        val classDraft = draft(EntryKind.CLASS, "Python").edit("course", "A105EST05").edit("day", "2")
        assertNotNull(EntryRules.error(classDraft))
        assertNotNull(EntryRules.error(classDraft.edit("start", "08:00").edit("end", "07:59")))
        assertNotNull(EntryRules.error(classDraft.edit("start", "24:00").edit("end", "25:00")))
        assertNull(EntryRules.error(classDraft.edit("start", "08:00").edit("end", "08:45")))
        assertEquals(525, EntryRules.time("08:45"))
    }
    @Test fun `deadline requires actual date and never substitutes a grade weight`() {
        val deadline = draft(EntryKind.DEADLINE, "Graphics sheet")
        assertNotNull(EntryRules.error(deadline))
        assertNotNull(EntryRules.error(deadline.edit("date", "2026-02-30")))
        assertNull(EntryRules.error(deadline.edit("date", "2026-11-15")))
        assertNotNull(EntryRules.error(deadline.edit("date", "2026-11-15").edit("weight", "101")))
    }
    @Test fun `water weight and sets reject invalid values without rounding or fabricated numbers`() {
        for (bad in listOf("", "0", "250.5", "-20", "10001", "999999999999")) assertNotNull(EntryRules.error(draft(EntryKind.WATER, bad)))
        assertNull(EntryRules.error(draft(EntryKind.WATER, "250")))
        for (bad in listOf("NaN", "Infinity", "-1", "0", "hello")) assertNotNull(EntryRules.error(draft(EntryKind.WEIGHT, bad)))
        assertNull(EntryRules.error(draft(EntryKind.WEIGHT, "64.5")))
        val set = draft(EntryKind.SET, "Pull ups").edit("reps", "8").edit("kg", "0")
        assertNull(EntryRules.error(set))
        assertNotNull(EntryRules.error(set.edit("kg", "NaN")))
        assertNotNull(EntryRules.error(set.edit("reps", "8.5")))
        assertNotNull(EntryRules.error(set.edit("number", "100")))
    }
    @Test fun `switching entry kinds retains independent user supplied fields`() {
        val task = draft(EntryKind.TASK, "My task").edit("due", "After class")
        val project = task.copy(kind = EntryKind.PROJECT).edit("title", "My app").edit("steps", "First screen")
        assertEquals("My task", project.copy(kind = EntryKind.TASK).get("title"))
        assertEquals("After class", project.copy(kind = EntryKind.TASK).get("due"))
        assertEquals("My app", project.get("title"))
        assertEquals(task.id, project.id)
    }
    @Test fun `meal values must all be explicit and cannot overflow the saved total`() {
        val totals = listOf(100, 20, 30, 10)
        assertNull(EntryRules.meal(listOf("500", "", "60", "10"), totals))
        assertNull(EntryRules.meal(listOf("500", "10.5", "60", "10"), totals))
        assertNull(EntryRules.meal(listOf("500", "-1", "60", "10"), totals))
        assertNull(EntryRules.meal(listOf("500", "10", "60", "10"), listOf(Int.MAX_VALUE, 0, 0, 0)))
        assertEquals(listOf(500, 0, 60, 10), EntryRules.meal(listOf("500", "0", "60", "10"), totals))
    }
}
