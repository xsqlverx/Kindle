package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class AcademicSource(val id: String, val asset: String, val pages: Int, val sha256: String)
data class SyllabusCourse(val id: String, val code: String, val title: String, val short: String,
    val hours: String, val credits: Int, val assessment: String, val note: String)
data class SyllabusItem(val id: String, val course: String, val module: String, val moduleKey: String,
    val hours: Int, val page: Int, val scope: String, val title: String, val detail: String,
    val source: String, val firstStep: String) {
    val material: String get() = "syllabus://$source/$page"
    val sourceType: String get() = if (scope == "CODE") "PRACTICE" else "TOPIC"
}
data class AcademicEvent(val id: String, val title: String, val semester: Int, val kind: String,
    val dates: List<String>, val page: Int)
data class AcademicPack(val version: String, val title: String, val preparationEnd: String,
    val sources: List<AcademicSource>, val courses: List<SyllabusCourse>, val items: List<SyllabusItem>,
    val events: List<AcademicEvent>, val notices: List<String>) {
    fun nextExam(today: String) = events.filter { it.semester == 1 && it.kind == "EXAM" && it.dates.any { date -> date >= today } }
        .minByOrNull { it.dates.first { date -> date >= today } }
}
data class AcademicTarget(val id: String, val date: String, val itemId: String, val minutes: Int, val mode: String)
data class AcademicRoadmap(val targets: List<AcademicTarget> = emptyList(), val dailyMinutes: Int = 50,
    val covered: Set<String> = emptySet(), val done: Set<String> = emptySet(), val modules: Set<String> = emptySet())

/** Institution date keys; no java.time dependency on API 24 and no invented paper time. */
object AcademicDates {
    private fun format() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata"); isLenient = false
    }
    fun key(at: Long) = format().format(java.util.Date(at))
    fun millis(key: String): Long {
        val formatter = format()
        val parsed = requireNotNull(formatter.parse(key))
        require(formatter.format(parsed) == key)
        return parsed.time
    }
    fun plus(key: String, days: Int): String = format().format(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
        timeInMillis = millis(key); add(Calendar.DAY_OF_MONTH, days)
    }.time)
}

/** Date-only preparation suggestions. Contact hours are never treated as personal study estimates. */
object SyllabusPlanner {
    fun build(pack: AcademicPack, from: String, dailyMinutes: Int, covered: Set<String> = emptySet(),
        modules: Set<String> = emptySet()): List<AcademicTarget> {
        require(dailyMinutes in 10..180)
        AcademicDates.millis(from)
        if (from > pack.preparationEnd) return emptyList()
        val eligible = pack.items.filter { it.scope == "THEORY" && (modules.isEmpty() || it.moduleKey in modules) }
        val pools = pack.courses.map { course -> eligible.filter { it.course == course.id && it.id !in covered } }.filter { it.isNotEmpty() }
        val cycle = mutableListOf<SyllabusItem>()
        repeat(pools.maxOfOrNull { it.size } ?: 0) { index -> pools.forEach { pool -> pool.getOrNull(index)?.let(cycle::add) } }
        // Once first-pass items are explicitly covered, use review rather than inventing mastery.
        val reviewOnly = cycle.isEmpty()
        if (reviewOnly) cycle.addAll(eligible)
        if (cycle.isEmpty()) return emptyList()
        val count = (dailyMinutes / 25).coerceIn(1, 4)
        val result = mutableListOf<AcademicTarget>()
        var date = from
        var cursor = 0
        var day = 0
        while (date <= pack.preparationEnd && day < 120) {
            repeat(count) { slot ->
                val portfolio = if (day % 7 == 0 && slot == count - 1) pack.items.firstOrNull { it.id == "aisat26:life_skills:portfolio" && it.id !in covered } else null
                val item = portfolio ?: cycle[cursor % cycle.size]
                val review = portfolio == null && (reviewOnly || cursor >= cycle.size)
                if (portfolio == null) cursor++
                val minutes = dailyMinutes / count + if (slot == count - 1) dailyMinutes % count else 0
                result.add(AcademicTarget("${pack.version}:$date:$slot:${item.id}", date, item.id, minutes,
                    if (portfolio != null) "PORTFOLIO" else if (review) "RECALL" else "FIRST PASS"))
            }
            day++; date = AcademicDates.plus(date, 1)
        }
        return result
    }

    fun next(pack: AcademicPack, roadmap: AcademicRoadmap, now: Long, topics: List<ExamTopic>,
        attempts: List<CodingAttempt>): List<DayMove> {
        val today = AcademicDates.key(now)
        if (today > pack.preparationEnd) return emptyList()
        val nextExam = pack.nextExam(today)
        return roadmap.targets.filter { it.date <= today && it.id !in roadmap.done }.mapNotNull { target ->
            val item = pack.items.firstOrNull { it.id == target.itemId } ?: return@mapNotNull null
            if (item.id in roadmap.covered && target.mode != "RECALL") return@mapNotNull null
            val topic = topics.firstOrNull { it.id == item.id }
            val attempt = attempts.firstOrNull { it.id == item.id }
            if (item.sourceType == "TOPIC" && topic == null || item.sourceType == "PRACTICE" && attempt == null) return@mapNotNull null
            val course = pack.courses.first { it.id == item.course }
            DayMove("${course.short} · ${topic?.title ?: attempt?.problem ?: item.title}", item.sourceType, item.id,
                topic?.material ?: attempt?.link ?: item.material,
                "${target.mode.lowercase().replaceFirstChar(Char::uppercase)} · ${item.firstStep}",
                "Your dated preparation target${nextExam?.let { " · ${it.title} from ${it.dates.first()}" }.orEmpty()}. Internal portions are unconfirmed.",
                minutes = target.minutes)
        }.distinctBy { it.key }.take(2)
    }
}
