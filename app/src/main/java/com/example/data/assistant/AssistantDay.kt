package com.example.data.assistant

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantDaySnapshot(val at: Long, val session: FocusSession?, val current: DayAnchor?,
    val nextAnchor: DayAnchor?, val nextBlock: DayBlock?, val suggestion: DayMove?)

/** Local, explainable answers use only records already in the day model. */
object AssistantDay {
    fun isNextQuestion(raw: String): Boolean {
        val text = raw.lowercase(Locale.ROOT).replace('’', '\'').replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        if (listOf("tomorrow", "next week", "next month", "python", "code", "explain", "exam").any { it in text }) return false
        val next = listOf("what's next", "whats next", "what is next", "what next", "what should i do now", "what do i do now", "what should i do next", "what do i do next", "what should i do today").any { it in text }
        val day = listOf("day", "today", "schedule", "now", "next up").any { it in text }
        return next && (day || text in listOf("what's next", "whats next", "what is next", "what next", "what should i do next", "what do i do next"))
    }

    fun snapshot(input: DayInputs): AssistantDaySnapshot {
        val anchors = DayGuide.anchors(input)
        val session = input.session.takeIf { it.phase != FocusPhase.IDLE }
        val current = anchors.firstOrNull { input.now >= it.start && input.now < it.end }
        val nextAnchor = anchors.firstOrNull { it.start > input.now }
        val nextBlock = input.blocks.asSequence().filter {
            !it.fixed && it.state == "PLANNED" && it.startMillis in DayGuide.startOfDay(input.now) until DayGuide.endOfDay(input.now) &&
                it.endMillis > input.now && DayGuide.available(input, it.sourceType, it.sourceId)
        }.minByOrNull { it.startMillis }
        return AssistantDaySnapshot(input.now, session, current, nextAnchor, nextBlock, DayGuide.choices(input).firstOrNull())
    }

    fun summary(day: AssistantDaySnapshot): String {
        fun time(value: Long) = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(value))
        return buildString {
            append("Snapshot from your saved day at ${time(day.at)}.")
            day.session?.let { append("\nSession: ${it.title} (${it.phase.name.lowercase(Locale.ROOT)}${if (it.sourceType == "PREVIEW") ", preview" else ""}).") }
            day.current?.let { append("\nCurrent saved commitment: ${it.title}, until ${time(it.end)}.") }
            day.nextAnchor?.let { append("\nNext saved commitment: ${it.title}, ${time(it.start)}.") }
            day.nextBlock?.let { append("\nSaved work: ${it.title}, ${time(it.startMillis)}.") }
            day.suggestion?.let { append("\nSuggested next step: ${it.title}. ${it.reason}.") }
            if (day.session == null && day.current == null && day.nextAnchor == null && day.nextBlock == null && day.suggestion == null)
                append("\nThere is no upcoming saved work or commitment in this snapshot.")
        }
    }
}
