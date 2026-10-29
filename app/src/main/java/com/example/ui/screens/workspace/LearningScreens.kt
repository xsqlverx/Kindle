package com.example.ui.screens.workspace

import com.example.ui.components.WorkspaceButton
import com.example.ui.components.WorkspaceTextButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.*
import java.util.Calendar

@Composable
fun ProjectsScreen(model: WorkspaceViewModel, focus: TodayPrototypeViewModel) {
    val projects by model.projects.collectAsStateWithLifecycle()
    val steps by model.steps.collectAsStateWithLifecycle()
    var addProject by rememberSaveable { mutableStateOf(false) }
    var editProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var addStepTo by rememberSaveable { mutableStateOf<String?>(null) }
    var showArchive by rememberSaveable { mutableStateOf(false) }
    val colors = LocalLifeOsColors.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { SpaceHeading("Projects", "Ship the next piece.", "Keep the goal, milestones and blocker together. Focus on one unfinished step.") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkspaceButton(onClick = { addProject = true }) { Text("New project") }
            WorkspaceTextButton(onClick = { showArchive = !showArchive }) { Text(if (showArchive) "Active" else "Archive") }
        } }
        if (projects.none { it.archived == showArchive }) item { WorkCard("A goal needs a next step", "Add a coding project, college deliverable or personal build. Milestone checkboxes are your explicit completion record.") }
        items(projects.filter { it.archived == showArchive }, key = { it.id }) { project ->
            val milestones = steps.filter { it.projectId == project.id }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(project.title, color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Text(project.goal, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                if (project.blocker.isNotBlank()) Text("BLOCKER · ${project.blocker}", color = colors.primaryAccent, fontSize = 13.sp)
                if (milestones.isNotEmpty()) LinearProgressIndicator(progress = { milestones.count { it.done }.toFloat() / milestones.size }, modifier = Modifier.fillMaxWidth())
                milestones.forEach { step ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(step.done, { model.saveStep(step.copy(done = it)) })
                        Text(step.title, Modifier.weight(1f), color = colors.textPrimary)
                    }
                }
                val next = milestones.firstOrNull { !it.done }
                if (next != null && !project.archived) WorkspaceButton(onClick = { model.offerMove(DayMove(next.title, "PROJECT_STEP", next.id, firstStep = project.blocker, reason = "Next milestone · ${project.title}")) }) { Text("Focus on next milestone") }
                Row {
                    WorkspaceTextButton(onClick = { addStepTo = project.id }) { Text("Add milestone") }
                    WorkspaceTextButton(onClick = { editProjectId = project.id }) { Text("Edit") }
                    WorkspaceTextButton(onClick = { model.saveProject(project.copy(archived = !project.archived)) }) { Text(if (project.archived) "Restore" else "Archive") }
                }
                HorizontalDivider(color = colors.borderGlass)
            }
        }
    }
    if (addProject) RecordDialog("New project", listOf("Project name", "What does done look like?"), onDismiss = { addProject = false }) {
        model.saveProject(WorkProject(uid(), it[0], it[1]))
    }
    projects.firstOrNull { it.id == editProjectId }?.let { project ->
        RecordDialog("Edit project", listOf("Name", "Goal", "Current blocker"), listOf(project.title, project.goal, project.blocker), { editProjectId = null }) {
            model.saveProject(project.copy(title = it[0], goal = it[1], blocker = it[2]))
        }
    }
    addStepTo?.let { projectId -> RecordDialog("A concrete milestone", listOf("Action / deliverable"), onDismiss = { addStepTo = null }) {
        model.saveStep(ProjectStep(uid(), projectId, it[0], (steps.filter { step -> step.projectId == projectId }.maxOfOrNull { step -> step.position } ?: -1) + 1))
    } }
}

@Composable
fun CaptureScreen(model: WorkspaceViewModel) {
    val notes by model.captures.collectAsStateWithLifecycle()
    var add by rememberSaveable { mutableStateOf(false) }
    var processed by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SpaceHeading("Capture", "Get it out of your head.", "A thought, a useful link, a question for class. Decide what it becomes when you have space.") }
        item { Row {
            WorkspaceButton(onClick = { add = true }) { Text("Capture a thought") }
            WorkspaceTextButton(onClick = { processed = !processed }) { Text(if (processed) "Inbox" else "Processed") }
        } }
        if (notes.none { it.archived == processed }) item { WorkCard(if (processed) "Nothing processed yet" else "Your inbox is clear", "Notes stay local. Turning one into a task keeps its original record and avoids duplicate tasks on repeated taps.") }
        items(notes.filter { it.archived == processed }, key = { it.id }) { note ->
            WorkCard(note.text, dated(note.createdAt)) {
                if (note.taskId > 0) Text("Linked task #${note.taskId} · see Tasks", fontSize = 12.sp)
                Row {
                    if (note.taskId == 0L) WorkspaceTextButton(onClick = { model.captureToTask(note) }) { Text("Make a task") }
                    WorkspaceTextButton(onClick = { editId = note.id }) { Text("Edit note") }
                    WorkspaceTextButton(onClick = { model.saveCapture(note.copy(archived = !note.archived)) }) { Text(if (note.archived) "Restore" else "File away") }
                }
            }
        }
    }
    if (add) RecordDialog("Capture", listOf("Note / URL / question"), onDismiss = { add = false }) { model.saveCapture(CaptureNote(uid(), it[0], System.currentTimeMillis())) }
    notes.firstOrNull { it.id == editId }?.let { note ->
        RecordDialog("Edit original note", listOf("Note"), listOf(note.text), { editId = null }) { model.saveCapture(note.copy(text = it[0])) }
    }
}

@Composable
fun ExamPrepScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long) {
    val topics by model.topics.collectAsStateWithLifecycle()
    val ownTopics = topics.filterNot { it.id.startsWith("aisat26:") }
    val target by life.targetExam.collectAsStateWithLifecycle()
    var add by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = LocalLifeOsColors.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SpaceHeading("Exam preparation", "Your syllabus, in motion.", if (target.dateMillis > 0) "Your manual target · ${target.title} · ${target.getFormattedDate()}" else "Your supplied syllabus and college calendar, connected to the next useful session.") }
        item { AcademicHub(model, now) }
        item { Text("Your additional revision topics", color = colors.textPrimary, fontSize = 21.sp) }
        item { WorkspaceButton(onClick = { add = true }) { Text("Add a topic") } }
        if (ownTopics.isEmpty()) item { WorkCard("Keep extra work here", "Your provided syllabus is in the study map above. Add teacher extras, custom topics or personal references here.") }
        ownTopics.groupBy { it.course }.forEach { (course, group) ->
            item(key = "course_$course") { Text(course.ifBlank { "Unsorted" }, color = colors.primaryAccent, fontSize = 21.sp, fontWeight = FontWeight.Medium) }
            items(group.sortedBy { it.revisionAt }, key = { it.id }) { topic ->
                WorkCard(topic.title, when {
                    topic.revisionAt == 0L -> "Not scheduled for revision"
                    topic.revisionAt <= now -> "Revision due · ${dated(topic.revisionAt)}"
                    else -> "Next revision · ${dated(topic.revisionAt)}"
                }) {
                    if (topic.material.isNotBlank()) MaterialReference(topic.material)
                    Text("Your confidence · ${listOf("Unrated", "Needs work", "Developing", "Comfortable")[topic.confidence.coerceIn(0, 3)]}", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (0..3).forEach { level -> FilterChip(selected = topic.confidence == level,
                            onClick = { model.saveTopic(topic.copy(confidence = level)) }, label = { Text(if (level == 0) "?" else "$level") }) }
                    }
                    WorkspaceButton(onClick = { model.offerMove(DayMove("${topic.course} · ${topic.title}", "TOPIC", topic.id, topic.material, reason = "From your revision topics")) }) { Text("Study this topic") }
                    RevisionActions(topic.revisionAt, onChange = { model.saveTopic(topic.copy(revisionAt = it)) })
                    WorkspaceTextButton(onClick = { editId = topic.id }) { Text("Edit topic & material") }
                }
            }
        }
    }
    if (add) RecordDialog("Add an exam topic", listOf("Topic", "Course", "Material / page / link"), onDismiss = { add = false }) {
        model.saveTopic(ExamTopic(uid(), it[1], it[0], it[2]))
    }
    topics.firstOrNull { it.id == editId }?.let { topic -> RecordDialog("Edit topic", listOf("Topic", "Course", "Material"), listOf(topic.title, topic.course, topic.material), { editId = null }) {
        model.saveTopic(topic.copy(title = it[0], course = it[1], material = it[2]))
    } }
}

@Composable
internal fun RevisionActions(at: Long, onChange: (Long) -> Unit) {
    Text("Schedule next review", fontSize = 12.sp)
    Row {
        listOf(1, 3, 7).forEach { days -> WorkspaceTextButton(onClick = {
            onChange(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, days) }.timeInMillis)
        }) { Text("${days}d") } }
        if (at > 0) WorkspaceTextButton(onClick = { onChange(0L) }) { Text("Clear") }
    }
}

@Composable
fun PracticeScreen(model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long) {
    val attempts by model.attempts.collectAsStateWithLifecycle()
    var add by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var dueOnly by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SpaceHeading("Coding practice", "Keep the lesson, not just the count.", "Log a problem and your actual outcome. Save the idea you missed and bring it back into the retry queue.") }
        item { Row {
            WorkspaceButton(onClick = { add = true }) { Text("Add a problem") }
            WorkspaceTextButton(onClick = { dueOnly = !dueOnly }) { Text(if (dueOnly) "All problems" else "Due retries") }
        } }
        val visible = attempts.filter { !dueOnly || it.retryAt in 1L..now }
        if (visible.isEmpty()) item { WorkCard(if (dueOnly) "No retries due" else "Start with a real problem", "Add a DSA exercise, debugging question or contest problem. Unattempted stays unattempted until you report the result.") }
        items(visible, key = { it.id }) { attempt ->
            WorkCard(attempt.problem, "${attempt.difficulty} · ${if (attempt.id.startsWith("aisat26:") && attempt.outcome == "Unattempted") "No app outcome recorded" else attempt.outcome}" + if (attempt.retryAt > 0) "\nRetry · ${dated(attempt.retryAt)}" else "") {
                if (attempt.link.isNotBlank()) MaterialReference(attempt.link)
                if (attempt.notes.isNotBlank()) Text("Lesson · ${attempt.notes}", fontSize = 14.sp)
                WorkspaceButton(onClick = {
                    if (attempt.id.startsWith("aisat26:")) model.offerAcademic(attempt.id)
                    else model.offerMove(DayMove(attempt.problem, "PRACTICE", attempt.id, attempt.link, attempt.notes, "From your coding practice"))
                }) { Text("Work on this problem") }
                Column {
                    listOf("Unattempted", "Stuck", "With help", "Solved").chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { pair.forEach { outcome ->
                            FilterChip(selected = attempt.outcome == outcome, onClick = { model.saveAttempt(attempt.copy(outcome = outcome)) }, label = { Text(outcome) })
                        } }
                    }
                }
                RevisionActions(attempt.retryAt) { model.saveAttempt(attempt.copy(retryAt = it)) }
                WorkspaceTextButton(onClick = { editId = attempt.id }) { Text("Edit problem / save lesson") }
            }
        }
    }
    if (add) RecordDialog("Coding problem", listOf("Problem", "Link", "Difficulty / source"), onDismiss = { add = false }) {
        model.saveAttempt(CodingAttempt(uid(), it[0], it[1], it[2].ifBlank { "Unspecified" }, createdAt = System.currentTimeMillis()))
    }
    attempts.firstOrNull { it.id == editId }?.let { attempt ->
        RecordDialog("Problem & lesson", listOf("Problem", "Link", "Difficulty", "What did you learn / where are you stuck?"),
            listOf(attempt.problem, attempt.link, attempt.difficulty, attempt.notes), { editId = null }) {
            model.saveAttempt(attempt.copy(problem = it[0], link = it[1], difficulty = it[2], notes = it[3]))
        }
    }
}

@Composable
fun ReviewScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long) {
    val history by model.history.collectAsStateWithLifecycle()
    val resets by life.allDailyResets.collectAsStateWithLifecycle()
    val tasks by life.allTasks.collectAsStateWithLifecycle()
    val sets by life.currentSplitSets.collectAsStateWithLifecycle()
    val attempts by model.attempts.collectAsStateWithLifecycle()
    val colors = LocalLifeOsColors.current
    val today = history.filter { it.endedAt in WorkspaceViewModel.dayStart(now)..now }
    val weekStart = Calendar.getInstance().apply { timeInMillis = WorkspaceViewModel.dayStart(now); add(Calendar.DAY_OF_MONTH, -6) }.timeInMillis
    val week = history.filter { it.endedAt in weekStart..now }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SpaceHeading("Review", "Evidence, in your own words.", "Only saved sessions and explicit records appear here. Time spent is different from a task completed.") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) { Text("${today.sumOf { it.elapsedSeconds } / 60}", color = colors.primaryAccent, fontSize = 48.sp); Text("minutes today", color = colors.textSecondary) }
                Column(Modifier.weight(1f)) { Text("${week.sumOf { it.elapsedSeconds } / 60}", color = colors.primaryAccent, fontSize = 48.sp); Text("minutes · 7 days", color = colors.textSecondary) }
            }
        }
        item { WorkCard("Other saved evidence", "${tasks.count { it.status == TaskStatus.COMPLETED }} completed tasks in storage\n${sets.count { it.isCompleted }}/${sets.size} sets checked in the selected split\n${attempts.count { it.outcome == "Solved" }} problems marked solved\n${resets.size} Daily Reset records") {
            Text("Task totals are all-time stored counts; set checks reflect the selected routine, not a dated workout history.", fontSize = 11.sp)
            WorkspaceTextButton(onClick = { model.navigate(Destination.RESET) }) { Text("Reflect in Daily Reset") }
        } }
        if (history.isEmpty()) item { WorkCard("Your trail starts with real work", "Save a session with a resume note. A timer that elapsed still needs your review before appearing here.") }
        items(history, key = { it.id }) { record ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dated(record.endedAt), color = colors.primaryAccent, fontSize = 11.sp)
                Text(record.title, color = colors.textPrimary, fontSize = 21.sp)
                Text("${record.elapsedSeconds / 60} min ${record.elapsedSeconds % 60}s · ${record.outcome} · ${record.sourceType.lowercase().replace('_', ' ')}", color = colors.textSecondary, fontSize = 12.sp)
                if (record.resumeNote.isNotBlank()) Text(record.resumeNote, color = colors.textPrimary, fontSize = 14.sp)
                WorkspaceTextButton(onClick = {
                    when (record.sourceType) {
                        "FITNESS" -> model.navigate(Destination.FITNESS)
                        "REST" -> model.navigate(Destination.PLAN)
                        else -> model.offerMove(DayMove(record.title, record.sourceType, record.sourceId, record.material, record.resumeNote, "Your saved place"))
                    }
                }) { Text(when (record.sourceType) { "FITNESS" -> "Open Fitness"; "REST" -> "Plan the next window"; else -> "Continue this thread" }) }
                HorizontalDivider(color = colors.borderGlass)
            }
        }
    }
}
