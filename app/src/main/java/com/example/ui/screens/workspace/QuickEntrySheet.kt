package com.example.ui.screens.workspace

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.WorkspaceViewModel
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickEntrySheet(model: WorkspaceViewModel) {
    val draft by model.entryDraft.collectAsStateWithLifecycle()
    val busy by model.entrySaving.collectAsStateWithLifecycle()
    val result by model.entryResult.collectAsStateWithLifecycle()
    val error by model.entryError.collectAsStateWithLifecycle()
    val pack by model.academicPack.collectAsStateWithLifecycle()
    val projects by model.projects.collectAsStateWithLifecycle()
    val colors = LocalLifeOsColors.current
    val context = LocalContext.current
    var details by rememberSaveable(draft.kind) { mutableStateOf(false) }
    var allKinds by rememberSaveable { mutableStateOf(false) }
    fun field(key: String, text: String) = model.editEntry(key, text)
    val kind = draft.kind
    ModalBottomSheet(onDismissRequest = model::closeEntry, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background, dragHandle = null) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding().testTag("quick_entry")) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (result == null) "Add something to your day" else "Kept, in the right place.", color = colors.textPrimary, fontSize = 23.sp)
                    Text(if (result == null) "Start small. The extra detail can wait." else "Saved in ${result?.destination?.title}", color = colors.textSecondary, fontSize = 12.sp)
                }
                WorkspaceIconButton(onClick = model::closeEntry, enabled = !busy) { Icon(Icons.Outlined.Close, "Close; keep unfinished draft") }
            }
            val saved = result
            if (saved != null) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Icon(Icons.Outlined.CheckCircle, "Storage confirmed", tint = colors.primaryAccent, modifier = Modifier.size(52.dp))
                    ContinuityOrb(Modifier.size(100.dp))
                    Text(saved.title, color = colors.textPrimary, fontSize = 25.sp)
                    Text(when (EntryKind.valueOf(saved.receipt.kind)) {
                        EntryKind.PROJECT -> "Your project and next actions are together. You can add or change steps whenever you need."
                        EntryKind.CLASS -> "Only the class you entered was added. Attendance and historical totals stay separate."
                        EntryKind.SET -> "A planned set was added to your routine. It has not been marked performed."
                        EntryKind.WATER, EntryKind.WEIGHT, EntryKind.TRAINING -> "Your self-reported entry is saved."
                        else -> "You can return to it from ${saved.destination.title}."
                    }, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                    if (saved.detail.isNotBlank()) Text(saved.detail, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                    WorkspaceButton(onClick = model::openEntryResult, modifier = Modifier.fillMaxWidth()) { Text("Open ${saved.destination.title}") }
                    WorkspaceOutlinedButton(onClick = model::addEntryAgain, modifier = Modifier.fillMaxWidth()) { Text("Add another") }
                    WorkspaceTextButton(onClick = model::closeEntry) { Text("Back to what I was doing") }
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        (if (allKinds) EntryKind.entries.toList() else listOf(EntryKind.TASK, EntryKind.PROJECT, EntryKind.NOTE, EntryKind.DEADLINE, EntryKind.WATER)).forEach { choice ->
                            FilterChip(kind == choice, { if (!busy) { model.openEntry(choice); allKinds = false } }, label = { Text(choice.label) }, enabled = !busy)
                        }
                    }
                    WorkspaceDisclosure(if (allKinds) "Fewer entry types" else "College, study, habits & training", allKinds, { allKinds = !allKinds })
                    if (!allKinds && kind !in listOf(EntryKind.TASK, EntryKind.PROJECT, EntryKind.NOTE, EntryKind.DEADLINE, EntryKind.WATER)) Text("Adding · ${kind.label}", color = colors.primaryAccent, fontSize = 13.sp)
                    EntryField(draft.get("title"), { field("title", it) }, kind.prompt, kind.example, busy,
                        numeric = kind in listOf(EntryKind.WATER, EntryKind.WEIGHT), decimal = kind == EntryKind.WEIGHT, multiline = kind == EntryKind.NOTE, prominent = true)
                    if (kind == EntryKind.WATER) {
                        Text("Millilitres actually drunk. Nothing is logged until you confirm below.", color = colors.textSecondary, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(250, 500, 750).forEach { ml ->
                            WorkspaceOutlinedButton(enabled = !busy, onClick = { field("title", ml.toString()) }) { Text("$ml ml") }
                        } }
                    }
                    if (kind == EntryKind.WEIGHT) Text("kg · use your actual measurement. A goal is not required for a weigh-in.", color = colors.textSecondary, fontSize = 12.sp)
                    if (kind == EntryKind.HABIT) Text("${draft.get("days").ifBlank { "7" }} days per week · adjust in extra details.", color = colors.textSecondary, fontSize = 12.sp)
                    if (kind == EntryKind.TASK) Text("Priority · ${Quadrant.entries.firstOrNull { it.name == draft.get("priority") }?.label ?: "Schedule"} · adjust in extra details.", color = colors.textSecondary, fontSize = 12.sp)
                    if (kind == EntryKind.PROJECT) {
                        EntryField(draft.get("steps"), { field("steps", it) }, "Next actions · optional", "One small action per line\nSketch the first screen\nBuild the first working path", busy, multiline = true)
                        Text("A name or GitHub URL is enough to save. Each line becomes an editable project step.", color = colors.textSecondary, fontSize = 12.sp)
                        val repo = EntryRules.repository(draft.get("link")) ?: EntryRules.repositoryFromTitle(draft.get("title"))
                        val existing = repo?.let { target -> projects.firstOrNull { project -> project.goal.lines().any { line -> EntryRules.repository(line)?.equals(target, ignoreCase = true) == true } } }
                        if (existing != null) {
                            Text("Already in Projects as ${existing.title}. Saving here creates a separate project.", color = colors.primaryAccent, fontSize = 12.sp, lineHeight = 18.sp)
                            WorkspaceTextButton(onClick = { model.closeEntry(); model.navigate(Destination.PROJECTS) }, enabled = !busy) { Text("Open existing projects") }
                        }
                    }
                    if (kind in listOf(EntryKind.CLASS, EntryKind.DEADLINE, EntryKind.TOPIC)) {
                        Text("Swipe through your courses, or type your own below.", color = colors.textSecondary, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(pack?.courses.orEmpty(), key = { it.id }) { course ->
                            FilterChip(draft.get("course") == course.code, { if (!busy) {
                                field("course", course.code)
                                if (kind == EntryKind.CLASS && (draft.get("title").isBlank() || pack?.courses?.any { it.short == draft.get("title") } == true)) field("title", course.short)
                            } }, label = { Text(course.short) }, enabled = !busy)
                        } }
                        EntryField(draft.get("course"), { field("course", it) }, if (kind == EntryKind.CLASS) "Course code" else "Course · optional", "Pick above or enter the code you use", busy)
                    }
                    if (kind == EntryKind.CLASS) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat", 1 to "Sun").forEach { (day, label) ->
                            FilterChip(draft.get("day") == day.toString(), { if (!busy) field("day", day.toString()) }, label = { Text(label) }, enabled = !busy)
                        } }
                        listOf("start" to "Class starts", "end" to "Class ends").forEach { (key, title) ->
                            WorkspaceActionRow(title, draft.get(key).ifBlank { "Tap to pick the actual time" }, Icons.Outlined.Schedule) {
                                if (!busy) { val current = EntryRules.time(draft.get(key)) ?: 8 * 60
                                    TimePickerDialog(context, { _, hour, minute -> field(key, String.format(Locale.ROOT, "%02d:%02d", hour, minute)) }, current / 60, current % 60, android.text.format.DateFormat.is24HourFormat(context)).show()
                                }
                            }
                        }
                    }
                    if (kind == EntryKind.DEADLINE) WorkspaceActionRow("Due date", draft.get("date").ifBlank { "Choose the date your teacher gave" }, Icons.Outlined.Event) {
                        if (!busy) {
                            val selected = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply { runCatching { AcademicDates.millis(draft.get("date")) }.getOrNull()?.let { timeInMillis = it } }
                            DatePickerDialog(context, { _, year, month, day -> field("date", String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)) }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show()
                        }
                    }
                    if (kind == EntryKind.TRAINING) EntryField(draft.get("minutes"), { field("minutes", it) }, "Actual duration · minutes", "How long you trained, 1–360", busy, numeric = true)
                    if (kind == EntryKind.SET) {
                        Text("Add a planned set to your routine; this does not mark it performed.", color = colors.textSecondary, fontSize = 12.sp)
                        EntryField(draft.get("reps"), { field("reps", it) }, "Repetitions", "Enter your planned reps", busy, numeric = true)
                        EntryField(draft.get("kg"), { field("kg", it) }, "Load · kg", "Use 0 for bodyweight", busy, numeric = true, decimal = true)
                        Text("Routine · ${draft.get("split").ifBlank { "Push" }}", color = colors.textSecondary, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Push", "Pull", "Legs", "Upper", "Lower", "Cardio").forEach { split ->
                            FilterChip(draft.get("split").ifBlank { "Push" } == split, { if (!busy) field("split", split) }, label = { Text(split) }, enabled = !busy)
                        } }
                    }
                    if (kind !in listOf(EntryKind.NOTE, EntryKind.WATER)) {
                        WorkspaceDisclosure("Extra details · optional", details, { details = !details })
                        if (details) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (kind) {
                                EntryKind.TASK -> {
                                    Text("Priority · defaults to Schedule", color = colors.textSecondary, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Quadrant.entries.forEach { q -> FilterChip(draft.get("priority").ifBlank { Quadrant.Q2_SCHEDULE.name } == q.name,
                                        { if (!busy) field("priority", q.name) }, label = { Text(q.label) }, enabled = !busy) } }
                                    EntryField(draft.get("category"), { field("category", it) }, "Category", "Academics, Personal, Projects…", busy)
                                    EntryField(draft.get("due"), { field("due", it) }, "Time note", "After college, or a time you choose", busy)
                                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(draft.get("top") == "true", { field("top", it.toString()) }, enabled = !busy); Text("One of my chosen priorities", color = colors.textPrimary, fontSize = 13.sp) }
                                }
                                EntryKind.PROJECT -> { EntryField(draft.get("goal"), { field("goal", it) }, "What would done look like?", "A small working version I can demonstrate", busy, multiline = true); EntryField(draft.get("link"), { field("link", it) }, "Project link", "GitHub or any https:// link", busy) }
                                EntryKind.CLASS -> { EntryField(draft.get("room"), { field("room", it) }, "Room", "Leave blank if unknown", busy); EntryField(draft.get("professor"), { field("professor", it) }, "Teacher", "Optional", busy) }
                                EntryKind.DEADLINE -> { EntryField(draft.get("note"), { field("note", it) }, "Brief / instructions", "Sheet numbers, submission link…", busy, multiline = true); EntryField(draft.get("weight"), { field("weight", it) }, "Assessment weight · %", "Leave blank if unknown", busy, numeric = true); EntryField(draft.get("category"), { field("category", it) }, "Type", "Assignment, record, project…", busy) }
                                EntryKind.TOPIC -> EntryField(draft.get("link"), { field("link", it) }, "Material / page / link", "Notebook p12, or a link", busy)
                                EntryKind.CODING -> { EntryField(draft.get("link"), { field("link", it) }, "Problem link", "Paste the exercise URL", busy); EntryField(draft.get("level"), { field("level", it) }, "Difficulty / source", "Optional — no score is assumed", busy) }
                                EntryKind.HABIT -> { EntryField(draft.get("days"), { field("days", it) }, "Days each week", "7 for daily, or choose 1–7", busy, numeric = true); EntryField(draft.get("category"), { field("category", it) }, "Category", "Optional", busy) }
                                EntryKind.WEIGHT, EntryKind.TRAINING -> EntryField(draft.get("note"), { field("note", it) }, "A note for next time", "Optional", busy, multiline = true)
                                EntryKind.SET -> EntryField(draft.get("number"), { field("number", it) }, "Set number", "Otherwise follows your saved sets", busy, numeric = true)
                                else -> Unit
                            }
                        }
                    }
                    if (draft.values.any { it.key.startsWith("${kind.name}.") && it.value.isNotBlank() }) WorkspaceTextButton(onClick = model::discardEntryKind, enabled = !busy) { Text("Clear this draft") }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.testTag("entry_error")) }
                    Spacer(Modifier.height(12.dp))
                }
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Saves to ${kind.destination.title}. Closing keeps your draft.", color = colors.textSecondary, fontSize = 12.sp)
                    WorkspaceButton(onClick = model::saveEntry, enabled = !busy && EntryRules.error(draft) == null, modifier = Modifier.fillMaxWidth().testTag("save_entry")) {
                        Text(if (busy) "Keeping it…" else if (error != null) "Try saving again" else if (kind in listOf(EntryKind.WATER, EntryKind.WEIGHT, EntryKind.TRAINING)) "Log ${kind.label.lowercase()}" else "Add ${kind.label.lowercase()}")
                    }
                    if (draft.get("title").isNotBlank() && EntryRules.error(draft) != null) Text(EntryRules.error(draft).orEmpty(), color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EntryField(value: String, onChange: (String) -> Unit, label: String, example: String, busy: Boolean, numeric: Boolean = false, decimal: Boolean = false, multiline: Boolean = false, prominent: Boolean = false) {
    MoveField(value, onChange, label, example, enabled = !busy, prominent = prominent, maxLines = if (multiline) 6 else 1,
        minLines = if (multiline) 3 else 1, singleLine = !multiline,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric && decimal) KeyboardType.Decimal else if (numeric) KeyboardType.Number else KeyboardType.Text))
}
