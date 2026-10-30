package com.example.data.model

import java.util.Calendar

data class DayMove(
    val title: String,
    val sourceType: String = "CUSTOM",
    val sourceId: String = "",
    val material: String = "",
    val firstStep: String = "",
    val reason: String = "A small step you choose",
    val blockId: String = "",
    val minutes: Int = 25
) {
    val key: String get() = "$sourceType:$sourceId:${if (sourceId.isBlank()) title else ""}"
}

data class DayAnchor(val id: String, val title: String, val start: Long, val end: Long, val destination: Destination)
enum class DayStage { FIRST_STEP, ARRIVE, READY, FOCUS, FINISH, CLASS, COMMITMENT, RECOVER, CLOSE }
data class DayCue(val stage: DayStage, val title: String, val detail: String, val move: DayMove? = null, val anchor: DayAnchor? = null)
data class DayInputs(
    val now: Long,
    val homeAt: Long = 0,
    val session: FocusSession = FocusSession(),
    val blocks: List<DayBlock> = emptyList(),
    val timetable: List<TimetableEntry> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val assignments: List<AssignmentItem> = emptyList(),
    val topics: List<ExamTopic> = emptyList(),
    val attempts: List<CodingAttempt> = emptyList(),
    val projects: List<WorkProject> = emptyList(),
    val steps: List<ProjectStep> = emptyList(),
    val history: List<WorkSessionRecord> = emptyList(),
    val resetDone: Boolean = false,
    val academicMoves: List<DayMove> = emptyList()
)

/** A transparent local ordering of real records. No inferred activity or readiness score. */
object DayGuide {
    fun available(input: DayInputs, type: String, id: String): Boolean = when (type) {
        "TASK" -> input.tasks.any { it.id.toString() == id && it.status == TaskStatus.PENDING }
        "ASSIGNMENT" -> input.assignments.any { it.id.toString() == id && !it.isSubmitted }
        "PROJECT_STEP" -> input.steps.any { step -> step.id == id && !step.done && input.projects.any { it.id == step.projectId && !it.archived } }
        "TOPIC" -> input.topics.any { it.id == id }
        "PRACTICE" -> input.attempts.any { it.id == id }
        "CUSTOM", "REST" -> true
        else -> false
    }
    fun startOfDay(at: Long): Long = Calendar.getInstance().apply {
        timeInMillis = at; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    fun endOfDay(at: Long): Long = Calendar.getInstance().apply { timeInMillis = startOfDay(at); add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    fun anchors(input: DayInputs): List<DayAnchor> {
        val cal = Calendar.getInstance().apply { timeInMillis = input.now }
        val classes = input.timetable.filter { it.dayOfWeek == cal.get(Calendar.DAY_OF_WEEK) }.map { entry ->
            fun at(h: Int, m: Int) = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            DayAnchor("class:${entry.id}", entry.courseName, at(entry.startHour, entry.startMinute), at(entry.endHour, entry.endMinute), Destination.COLLEGE)
        }
        return (classes + input.blocks.filter { it.fixed && it.startMillis in startOfDay(input.now) until endOfDay(input.now) }
            .map { DayAnchor(it.id, it.title, it.startMillis, it.endMillis, Destination.PLAN) }).sortedBy { it.start }
    }

    fun occupied(input: DayInputs, omitBlock: String = ""): List<Pair<Long, Long>> {
        val end = when (input.session.phase) {
            FocusPhase.RUNNING -> input.session.deadlineMillis
            FocusPhase.PAUSED -> input.now + input.session.pausedSeconds * 1000L
            else -> input.now
        }
        val active = if (end > input.now && input.session.sourceType != "PREVIEW") listOf(input.now to end) else emptyList()
        return anchors(input).map { it.start to it.end } + active + input.blocks.filter {
            !it.fixed && it.state == "PLANNED" && it.id != omitBlock && available(input, it.sourceType, it.sourceId)
        }.map { it.startMillis to it.endMillis }
    }

    fun window(input: DayInputs, minutes: Int, omitBlock: String = ""): Long? =
        DayPlanner.nextWindow(input.now, endOfDay(input.now), minutes, occupied(input, omitBlock))

    fun choices(input: DayInputs): List<DayMove> {
        val ranked = mutableListOf<Pair<Int, DayMove>>()
        input.academicMoves.filter { available(input, it.sourceType, it.sourceId) }.forEach { ranked.add(38 to it) }
        input.assignments.filter { !it.isSubmitted }.sortedBy { it.dueDateMillis }.forEach { item ->
            val urgent = item.dueDateMillis <= input.now + 2 * 86_400_000L
            ranked.add((if (urgent) 0 else 60) to DayMove(item.title, "ASSIGNMENT", item.id.toString(), firstStep = item.description,
                reason = if (urgent) "A deadline needs room soon" else "Your next saved deadline"))
        }
        input.tasks.filter { it.status == TaskStatus.PENDING && it.quadrant != Quadrant.Q4_ELIMINATE }.sortedWith(compareByDescending<TaskItem> { it.isTopThree }.thenBy { it.createdAt }).forEach { task ->
            val rank = when { task.isTopThree -> 10; task.quadrant == Quadrant.Q1_DO -> 20; task.quadrant == Quadrant.Q2_SCHEDULE -> 45; else -> 55 }
            ranked.add(rank to DayMove(task.title, "TASK", task.id.toString(), reason = if (task.isTopThree) "One of your chosen priorities" else "From your pending tasks"))
        }
        input.topics.filter { it.revisionAt in 1L..input.now }.sortedBy { it.revisionAt }.forEach {
            ranked.add(30 to DayMove("${it.course} · ${it.title}", "TOPIC", it.id, it.material, reason = "A revision you scheduled is due"))
        }
        input.attempts.filter { it.retryAt in 1L..input.now }.sortedBy { it.retryAt }.forEach {
            ranked.add(35 to DayMove(it.problem, "PRACTICE", it.id, it.link, it.notes, "A retry you scheduled is due"))
        }
        input.attempts.filter { it.retryAt == 0L && it.outcome != "Solved" && !it.id.startsWith("aisat26:") }.forEach {
            ranked.add(50 to DayMove(it.problem, "PRACTICE", it.id, it.link, it.notes, "An open coding problem"))
        }
        input.topics.filter { it.revisionAt == 0L && !it.id.startsWith("aisat26:") }.forEach {
            ranked.add(65 to DayMove(listOf(it.course, it.title).filter(String::isNotBlank).joinToString(" · "), "TOPIC", it.id, it.material, reason = "From your study topics"))
        }
        input.projects.filter { !it.archived && it.blocker.isBlank() }.forEach { project ->
            input.steps.filter { it.projectId == project.id && !it.done }.minByOrNull { it.position }?.let {
                ranked.add(40 to DayMove(it.title, "PROJECT_STEP", it.id, material = project.goal.lines().firstOrNull { line -> line.startsWith("https://") || line.startsWith("http://") }.orEmpty(), firstStep = project.blocker,
                    reason = "${if (project.id.startsWith("github:xsqlverx:")) "Suggested project" else "Next milestone"} · ${project.title}"))
            }
        }
        return ranked.sortedBy { it.first }.map { withResume(input, it.second) }.distinctBy { it.key }
    }

    private fun withResume(input: DayInputs, move: DayMove): DayMove {
        val last = input.history.firstOrNull { it.sourceType == move.sourceType && it.sourceId == move.sourceId }
        return if (last?.resumeNote?.isNotBlank() == true) move.copy(material = last.material.ifBlank { move.material }, firstStep = last.resumeNote,
            reason = "Your saved place · ${move.reason.replaceFirstChar(Char::lowercase)}") else move
    }

    /** Manual choice remains broader than the short automatic suggestion list. */
    fun library(input: DayInputs): List<DayMove> {
        val all = input.tasks.filter { it.status == TaskStatus.PENDING }.map { DayMove(it.title, "TASK", it.id.toString(), reason = "From your saved tasks") } +
            input.assignments.filter { !it.isSubmitted }.map { DayMove(it.title, "ASSIGNMENT", it.id.toString(), firstStep = it.description, reason = "From your College deadlines") } +
            input.topics.map { DayMove(listOf(it.course, it.title).filter(String::isNotBlank).joinToString(" · "), "TOPIC", it.id, it.material, reason = "From your study topics") } +
            input.attempts.map { DayMove(it.problem, "PRACTICE", it.id, it.link, it.notes, "From your coding practice") } +
            input.steps.filter { !it.done && input.projects.any { project -> project.id == it.projectId && !project.archived } }.map {
                val project = input.projects.first { project -> project.id == it.projectId }
                DayMove(it.title, "PROJECT_STEP", it.id, material = project.goal.lines().firstOrNull { line -> line.startsWith("https://") || line.startsWith("http://") }.orEmpty(), reason = "From your project milestones")
            }
        return (choices(input) + all.map { withResume(input, it) }).distinctBy { it.key }
    }

    fun fromBlock(block: DayBlock) = DayMove(block.title, block.sourceType, block.sourceId, block.material, block.firstStep,
        "Already in your day", block.id, block.durationMinutes)

    fun cue(input: DayInputs): DayCue {
        val session = input.session
        if (session.phase == FocusPhase.FINISHED) return DayCue(DayStage.FINISH, "Keep the progress.", "Leave one line for the next time you come back.")
        if (session.isActive) return DayCue(DayStage.FOCUS, if (session.phase == FocusPhase.PAUSED) "Your place is held." else "Just this, for now.", session.title)
        val current = anchors(input).firstOrNull { input.now >= it.start && input.now < it.end }
        if (current != null) return DayCue(if (current.destination == Destination.COLLEGE) DayStage.CLASS else DayStage.COMMITMENT,
            current.title, if (current.destination == Destination.COLLEGE) "From your saved timetable." else "This time is already spoken for.", anchor = current)
        val planned = input.blocks.filter { !it.fixed && it.state == "PLANNED" && it.startMillis in startOfDay(input.now) until endOfDay(input.now) && available(input, it.sourceType, it.sourceId) }
        val currentWork = planned.firstOrNull { it.startMillis <= input.now && input.now < it.endMillis }
        val hour = Calendar.getInstance().apply { timeInMillis = input.now }.get(Calendar.HOUR_OF_DAY)
        if (hour >= 21 && currentWork == null) return if (input.resetDone)
            DayCue(DayStage.CLOSE, "Today is put away.", "Your reflection is saved. Let the rest of tonight be yours.")
            else DayCue(DayStage.CLOSE, "Let today land.", "Keep what worked. Leave tomorrow one clear starting point.")
        val moves = choices(input)
        if (moves.isEmpty() && planned.isEmpty()) return DayCue(DayStage.FIRST_STEP, "One thing is enough.", "Name a small action. We'll find its place in the day.")
        val checkedIn = input.homeAt in startOfDay(input.now)..input.now
        if (hour in 13..18 && !checkedIn) return DayCue(DayStage.ARRIVE, "Home? Start here.", "Put college down for a moment. Then give the afternoon one clear next step.", moves.firstOrNull())
        val missed = planned.firstOrNull { it.endMillis <= input.now }
        if (missed != null) return DayCue(DayStage.RECOVER, "A smaller start still counts.", "${missed.title} can have a fresh window. Your commitments keep their place.", fromBlock(missed).copy(minutes = 10))
        val next = currentWork ?: planned.firstOrNull { it.startMillis > input.now }
        val move = next?.let(::fromBlock) ?: moves.firstOrNull()
        return DayCue(DayStage.READY, if (checkedIn) "Make the afternoon yours." else "Your next small move.", move?.reason.orEmpty(), move)
    }
}
