package com.example

import com.example.data.assistant.*
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AssistantPresentationTest {
    @Test fun convertsTheReportedMarkdownTableIntoSuggestions() {
        val text = """Here's a possible outline.

            || Time | Activity | Quick Tips |
            ||------|----------|------------|
            || **7:30 am** | Wake & hydrate | Drink water. |
            || **8:00 am** | Breakfast | Eat breakfast. |

            |- **Pomodoro:** Work for 25 minutes.
        """.trimMargin()
        val blocks = AssistantAnswer.parse(text)
        val rows = blocks.filter { it.kind == AnswerBlockKind.TABLE_ROW }
        assertEquals(2, rows.size)
        assertEquals("7:30 am", rows[0].label)
        assertEquals("Wake & hydrate", rows[0].title)
        assertTrue(rows[0].body.contains("Drink water"))
        assertEquals("Pomodoro", blocks.first { it.kind == AnswerBlockKind.STEP }.title)
        assertTrue(blocks.none { it.title.contains("**") || it.label.contains("**") })
    }

    @Test fun keepsCodeAndMalformedContentReadable() {
        val blocks = AssistantAnswer.parse("Before\n\n```python\nprint('hello')\n```\n\nA | B without a table divider")
        assertEquals("print('hello')", blocks.first { it.kind == AnswerBlockKind.CODE }.body)
        assertTrue(blocks.any { it.body.contains("A | B without a table divider") })
        assertTrue(AssistantAnswer.parse("```\nunfinished code").any { it.body.contains("unfinished code") })
        assertEquals("5 * 6 * 7 and 2 ** 3 ** 4", AssistantAnswer.clean("5 * 6 * 7 and 2 ** 3 ** 4"))
        assertEquals("AURA and emphasis", AssistantAnswer.clean("*AURA* and **emphasis**"))
    }

    @Test fun nextQuestionUsesOnlyActualSavedDayData() {
        assertTrue(AssistantDay.isNextQuestion("what's next in my day"))
        assertFalse(AssistantDay.isNextQuestion("what's next in Python today"))
        assertFalse(AssistantDay.isNextQuestion("plan tomorrow for me"))
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 6, 12, 30, 0); set(Calendar.MILLISECOND, 0)
        }
        val current = TimetableEntry(id = 1, courseCode = "CS", courseName = "Actual class", dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
            startHour = 12, startMinute = 0, endHour = 13, endMinute = 0, room = "")
        val future = current.copy(id = 2, courseName = "Later class", startHour = 14, endHour = 15)
        val input = DayInputs(now = cal.timeInMillis, timetable = listOf(current, future), tasks = listOf(TaskItem(id = 7, title = "Real task")))
        val day = AssistantDay.snapshot(input)
        assertEquals("Actual class", day.current?.title)
        assertEquals("Later class", day.nextAnchor?.title)
        assertEquals("Real task", day.suggestion?.title)
        val summary = AssistantDay.summary(day)
        assertTrue(summary.contains("Actual class"))
        assertFalse(summary.contains("Breakfast"))
        assertNull(AssistantDay.snapshot(DayInputs(now = input.now)).nextAnchor)
    }
}
