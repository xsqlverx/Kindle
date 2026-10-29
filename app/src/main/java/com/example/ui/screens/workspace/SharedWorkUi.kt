package com.example.ui.screens.workspace

import com.example.ui.components.WorkspaceButton
import com.example.ui.components.WorkspaceOutlinedButton
import com.example.ui.components.WorkspaceTextButton

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.BackdropGlass
import com.example.ui.components.focusSource
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.AcademicReference
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.LifeOsViewModel
import com.example.viewmodel.TodayPrototypeViewModel
import com.example.viewmodel.WorkspaceViewModel
import java.text.SimpleDateFormat
import java.util.*

data class WorkChoice(val type: String, val id: String, val title: String, val material: String = "", val step: String = "")

@Composable
fun workChoices(life: LifeOsViewModel, model: WorkspaceViewModel): List<WorkChoice> {
    val tasks by life.allTasks.collectAsStateWithLifecycle()
    val assignments by life.assignments.collectAsStateWithLifecycle()
    val projects by model.projects.collectAsStateWithLifecycle()
    val steps by model.steps.collectAsStateWithLifecycle()
    val topics by model.topics.collectAsStateWithLifecycle()
    val attempts by model.attempts.collectAsStateWithLifecycle()
    return tasks.filter { it.status == TaskStatus.PENDING }.map { WorkChoice("TASK", it.id.toString(), it.title) } +
        assignments.filter { !it.isSubmitted }.map { WorkChoice("ASSIGNMENT", it.id.toString(), it.title, step = it.description) } +
        steps.filter { !it.done && projects.any { project -> project.id == it.projectId && !project.archived } }.map { WorkChoice("PROJECT_STEP", it.id, it.title) } +
        topics.map { WorkChoice("TOPIC", it.id, "${it.course} · ${it.title}", it.material) } +
        attempts.filter { it.outcome != "Solved" || it.retryAt > 0 }.map { WorkChoice("PRACTICE", it.id, it.problem, it.link, it.notes) }
}

fun clock(at: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(at))
fun dated(at: Long): String = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(at))
fun uid(): String = UUID.randomUUID().toString()

@Composable
fun MaterialReference(text: String) {
    if (text.startsWith("syllabus://")) { AcademicReference(text); return }
    val handler = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    if (text.startsWith("https://") || text.startsWith("http://")) {
        WorkspaceTextButton(onClick = { try { handler.openUri(text) } catch (_: Exception) { failed = true } }) { Text(text) }
        if (failed) Text("No app could open this link. You can edit or copy the reference.", fontSize = 12.sp)
    } else androidx.compose.foundation.text.selection.SelectionContainer { Text(text, fontSize = 13.sp) }
}

@Composable
fun SpaceHeading(kicker: String, title: String, detail: String) {
    val colors = LocalLifeOsColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(kicker.uppercase(Locale.getDefault()), color = colors.primaryAccent, fontSize = 11.sp, letterSpacing = 1.8.sp)
        Text(title, color = colors.textPrimary, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Medium)
        Text(detail, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
fun WorkCard(title: String, detail: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) {
    val colors = LocalLifeOsColors.current
    BackdropGlass(modifier.fillMaxWidth().focusSource()) {
        Column(Modifier.animateContentSize(if (LocalReducedMotion.current) snap() else spring(1f, 450f)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = colors.textPrimary, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
            if (detail.isNotBlank()) Text(detail, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            content()
        }
    }
}

/** Saveable drafts remain in the dialog until a valid explicit submission. */
@Composable
fun RecordDialog(title: String, labels: List<String>, initial: List<String> = labels.map { "" },
                 onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var values by rememberSaveable { mutableStateOf(ArrayList(initial)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            labels.forEachIndexed { index, label ->
                OutlinedTextField(values[index], { next -> values = ArrayList(values.toMutableList().apply { set(index, next) }) },
                    label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = if (index == labels.lastIndex && labels.size > 1) 2 else 1)
            }
        }
    }, confirmButton = { WorkspaceButton(enabled = values.firstOrNull()?.isNotBlank() == true, onClick = { onSave(values.map { it.trim() }); onDismiss() }) { Text("Save") } },
        dismissButton = { WorkspaceTextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun WorkComposer(choices: List<WorkChoice>, onDismiss: () -> Unit,
                 onStart: (WorkChoice, String, String, Int) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var material by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("25") }
    var selectedType by rememberSaveable { mutableStateOf("CUSTOM") }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var picker by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Give these minutes a purpose") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkspaceTextButton(onClick = { picker = !picker }) { Text("Choose a task, deadline, topic or coding step") }
            if (picker) Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                choices.forEach { choice -> WorkspaceTextButton(onClick = {
                    title = choice.title; material = choice.material; step = choice.step
                    selectedType = choice.type; selectedId = choice.id; picker = false
                }) { Text("${choice.type.lowercase().replace('_', ' ')} · ${choice.title}") } }
                if (choices.isEmpty()) Text("No saved work yet. Name a concrete action below.")
            }
            OutlinedTextField(title, { title = it }, label = { Text("Action") })
            if (selectedId.isNotBlank()) WorkspaceTextButton(onClick = { selectedType = "CUSTOM"; selectedId = "" }) { Text("Linked ${selectedType.lowercase()} · unlink") }
            OutlinedTextField(material, { material = it }, label = { Text("Material / URL / page") })
            OutlinedTextField(step, { step = it }, label = { Text("First concrete step") })
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes (5–180)") })
            Text("Uses a free window now. Planned commitments stay protected. A finished timer does not complete the source record.", fontSize = 12.sp)
        }
    }, confirmButton = { WorkspaceButton(enabled = title.isNotBlank() && (minutes.toIntOrNull() ?: 0) in 5..180,
        onClick = { onStart(WorkChoice(selectedType, selectedId, title), material, step, minutes.toInt()); onDismiss() }) { Text("Start focus") } },
        dismissButton = { WorkspaceTextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPanel(focus: TodayPrototypeViewModel, now: Long, onResume: () -> Unit, onDismiss: () -> Unit) {
    val session by focus.session.collectAsStateWithLifecycle()
    val saving by focus.saving.collectAsStateWithLifecycle()
    val error by focus.saveError.collectAsStateWithLifecycle()
    LaunchedEffect(session.phase) { if (session.phase == FocusPhase.IDLE) onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(session.sourceType.replace('_', ' '), fontSize = 11.sp)
            Text(session.title, fontSize = 25.sp, lineHeight = 31.sp)
            val remaining = session.remainingSeconds(now)
            Text("%02d:%02d".format(remaining / 60, remaining % 60), fontSize = 56.sp, fontWeight = FontWeight.Light)
            Text(when (session.phase) {
                FocusPhase.FINISHED -> "Time is up. Record where you stopped."
                FocusPhase.PAUSED -> "Paused · your place is held"
                else -> "One small step at a time"
            })
            if (session.material.isNotBlank()) MaterialReference(session.material)
            if (session.firstStep.isNotBlank()) Text("First step · ${session.firstStep}")
            OutlinedTextField(session.resumeNote, focus::updateResumeNote, label = { Text("Where should you resume?") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            Text("Timer expiry records elapsed time only. Complete a task, milestone or submission explicitly in its own space.", fontSize = 12.sp)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (session.phase == FocusPhase.RUNNING) WorkspaceOutlinedButton(onClick = focus::pause, enabled = !saving) { Text("Pause") }
                if (session.phase == FocusPhase.PAUSED) WorkspaceOutlinedButton(onClick = onResume, enabled = !saving) { Text("Continue") }
                WorkspaceButton(onClick = focus::dismiss, enabled = !saving) { Text(if (saving) "Saving…" else "Save & end session") }
            }
            WorkspaceTextButton(onClick = onDismiss) { Text("Keep in capsule") }
        }
    }
}
