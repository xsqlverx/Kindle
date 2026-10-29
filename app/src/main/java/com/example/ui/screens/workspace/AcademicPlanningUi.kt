package com.example.ui.screens.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.WorkspaceViewModel

@Composable
fun AcademicCalendarBrief(model: WorkspaceViewModel, now: Long) {
    val pack by model.academicPack.collectAsStateWithLifecycle()
    val calendar = pack ?: return
    val today = AcademicDates.key(now)
    val next = calendar.events.firstOrNull { today in it.dates } ?: calendar.nextExam(today)
    WorkspaceActionRow(next?.title ?: "Your academic calendar", next?.let { "${it.dates.filter { date -> date >= today }.joinToString(" · ")} · college calendar, date only" } ?: "S1 / S2 milestones from your supplied calendar",
        Icons.Outlined.EventNote) { model.navigate(Destination.EXAMS) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicHub(model: WorkspaceViewModel, now: Long) {
    val pack by model.academicPack.collectAsStateWithLifecycle()
    val error by model.academicError.collectAsStateWithLifecycle()
    val roadmap by model.academicRoadmap.collectAsStateWithLifecycle()
    val colors = LocalLifeOsColors.current
    var library by rememberSaveable { mutableStateOf(false) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    var targets by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val academic = pack
    if (academic == null) { WorkCard("Your supplied syllabus", error ?: "Opening the academic reference…"); return }
    val today = AcademicDates.key(now)
    val next = academic.nextExam(today)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WorkCard("Your S1 study map", "${academic.courses.size} courses · original syllabus pages and dated preparation targets") {
            Text(next?.title ?: "S1 calendar reference", color = colors.primaryAccent, fontSize = 18.sp)
            Text(next?.dates?.joinToString(" · ") ?: "No future S1 examination date in the supplied calendar.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
            Text("College dates only. Subject-wise papers, times and internal portions are not supplied.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            AcademicReference("syllabus://calendar/1", "Check calendar")
            WorkspaceActionRow("Syllabus & sources", "Theory, lab, portfolio and excluded content", Icons.Outlined.MenuBook) { library = true }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WorkspaceOutlinedButton(onClick = { targets = true }) { Text("Roadmap") }
                WorkspaceTextButton(onClick = { calendar = true }) { Text("Calendar") }
                WorkspaceTextButton(onClick = { settings = true }) { Text("Adjust") }
            }
        }
        val due = roadmap.targets.filter { it.date <= today && it.id !in roadmap.done && (it.itemId !in roadmap.covered || it.mode == "RECALL") }.take(2)
        if (due.isEmpty()) Text("No pending date-only target for today. You can choose any syllabus item, or adjust the roadmap.", color = colors.textSecondary, fontSize = 13.sp)
        due.forEach { target -> AcademicTargetCard(academic, roadmap, target, model) }
        val theory = academic.items.filter { it.scope == "THEORY" && (roadmap.modules.isEmpty() || it.moduleKey in roadmap.modules) }
        val theoryIds = theory.map { it.id }.toSet()
        val beforeExam = roadmap.targets.filter { it.date >= today && it.id !in roadmap.done && next != null && it.date < next.dates.first() &&
            it.itemId in theoryIds && (it.itemId !in roadmap.covered || it.mode == "RECALL") }.map { it.itemId }.distinct().size
        Text("Suggested budget: ${roadmap.dailyMinutes} min/day. ${theory.count { it.id in roadmap.covered }}/${theory.size} theory groups marked covered by you. " +
            "$beforeExam distinct theory groups planned before the next window starts. Targets estimate study effort, not mastery or guaranteed coverage.",
            color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
        Text("Default scope is all theory while teacher portions are unknown. Dates are preparation targets; actual time is chosen around your saved commitments.",
            color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
    }
    if (library) AcademicLibrary(academic, roadmap, model) { library = false }
    if (calendar) AcademicCalendar(academic, today) { calendar = false }
    if (targets) AcademicRoadmapSheet(academic, roadmap, model, today) { targets = false }
    if (settings) AcademicPlanSettings(academic, roadmap, model) { settings = false }
}

@Composable
private fun AcademicTargetCard(pack: AcademicPack, roadmap: AcademicRoadmap, target: AcademicTarget, model: WorkspaceViewModel, onLeave: () -> Unit = {}) {
    val item = pack.items.firstOrNull { it.id == target.itemId } ?: return
    val course = pack.courses.first { it.id == item.course }
    val done = target.id in roadmap.done
    WorkCard("${course.short} · ${item.title}", "${target.date} · ${target.minutes} min · ${target.mode.lowercase()} · date-only suggestion") {
        Text(item.firstStep, fontSize = 12.sp, lineHeight = 19.sp)
        if (!done && target.date < AcademicDates.key(System.currentTimeMillis())) Text("Still open · work on it now, or refresh the roadmap from today in Adjust.", fontSize = 12.sp, lineHeight = 18.sp)
        WorkspaceButton(onClick = { onLeave(); model.offerAcademic(item.id) }) { Text("Choose its study window") }
        WorkspaceTextButton(onClick = { model.setAcademicTargetDone(target.id, !done) }) { Text(if (done) "Undo target completion" else "Mark this target done") }
        if (done) Text("Target completed by you. Course mastery and submission remain separate.", fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademicLibrary(pack: AcademicPack, roadmap: AcademicRoadmap, model: WorkspaceViewModel, onDismiss: () -> Unit) {
    val colors = LocalLifeOsColors.current
    val topics by model.topics.collectAsStateWithLifecycle()
    val attempts by model.attempts.collectAsStateWithLifecycle()
    var courseId by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = pack.courses.firstOrNull { it.id == courseId }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.background) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.92f), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(selected?.short ?: "Your actual syllabus", color = colors.textPrimary, fontSize = 28.sp)
                Text("Grouped from your supplied documents. Coverage is unreported until you mark it; study time alone never marks it covered.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                WorkspaceTextButton(onClick = { if (selected == null) onDismiss() else courseId = "" }) { Text(if (selected == null) "Close" else "All courses") }
            }
            if (selected == null) items(pack.courses, key = { it.id }) { course ->
                WorkspaceActionRow(course.title, "${course.code} · ${course.credits} credits · ${course.hours}", Icons.Outlined.School) { search = ""; courseId = course.id }
            } else {
                item {
                    Text(selected.assessment, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                    if (selected.note.isNotBlank()) Text(selected.note, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
                    AcademicReference("syllabus://${selected.id}/1", "Original syllabus")
                    MoveField(search, { search = it }, "FIND IN THIS COURSE", "A topic, module or activity", true, maxLines = 1)
                }
                items(pack.items.filter { it.course == courseId && (it.title + it.module + it.detail).contains(search, true) }, key = { it.id }) { item ->
                    WorkCard(item.title, "${item.module} · ${item.scope.lowercase().replace('_',' ')}${if (item.hours > 0) " · module contact hours ${item.hours}" else ""}") {
                        Text(item.detail, fontSize = 13.sp, lineHeight = 20.sp)
                        AcademicReference(item.material)
                        val record = topics.firstOrNull { it.id == item.id }
                        if (record != null) {
                            if (record.title != item.title) Text("Your study label · ${record.title}", fontSize = 13.sp)
                            if (record.material != item.material && record.material.isNotBlank()) MaterialReference(record.material)
                            Text("Your confidence · ${listOf("Unrated", "Needs work", "Developing", "Comfortable")[record.confidence.coerceIn(0, 3)]}", fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (0..3).forEach { level -> FilterChip(record.confidence == level,
                                    { model.saveTopic(record.copy(confidence = level)) }, label = { Text(if (level == 0) "?" else "$level") }) }
                            }
                            RevisionActions(record.revisionAt) { model.saveTopic(record.copy(revisionAt = it)) }
                            WorkspaceTextButton(onClick = { editId = record.id }) { Text("Study label & material") }
                        }
                        if (item.scope == "CODE") {
                            val outcome = attempts.firstOrNull { it.id == item.id }?.outcome ?: "Unattempted"
                            Text(if (outcome == "Unattempted") "No outcome recorded in the app" else "Your recorded outcome · $outcome", fontSize = 12.sp)
                            WorkspaceTextButton(onClick = { onDismiss(); model.navigate(Destination.PRACTICE) }) { Text("Open coding practice & record outcome") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(item.id in roadmap.covered, { model.setAcademicCovered(item.id, it) })
                            Text(if (item.scope == "ACTIVITY") "Artifact prepared by me" else "Covered/prepared by me", fontSize = 12.sp)
                        }
                        WorkspaceButton(onClick = { onDismiss(); model.offerAcademic(item.id) }) {
                            Text(if (item.scope == "CODE") "Practice this exercise" else "Work on this")
                        }
                        Text("Suggested approach · ${item.firstStep.removePrefix("Suggested practice: ").removePrefix("Suggested next step: ")}", fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
    topics.firstOrNull { it.id == editId }?.let { record ->
        RecordDialog("Your study label & material", listOf("Study label", "Material / page / link"), listOf(record.title, record.material), { editId = null }) {
            model.saveTopic(record.copy(title = it[0], material = it[1]))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademicRoadmapSheet(pack: AcademicPack, roadmap: AcademicRoadmap, model: WorkspaceViewModel, today: String, onDismiss: () -> Unit) {
    val colors = LocalLifeOsColors.current
    var all by rememberSaveable { mutableStateOf(false) }
    val visible = roadmap.targets.sortedWith(compareBy<AcademicTarget> { it.id in roadmap.done }.thenBy { it.date }).let { if (all) it else it.take(14) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.background) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.92f), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Preparation roadmap", color = colors.textPrimary, fontSize = 27.sp)
                Text("New suggestions budget: ${roadmap.dailyMinutes} min/day through ${pack.preparationEnd}. Completed targets retain their original minutes as history. No exact personal time is assumed.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                Text("Open targets from before $today stay visible. Refresh from today in Adjust if the plan has drifted; completed targets and actual sessions are kept.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
                WorkspaceDisclosure(if (all) "Show first targets" else "Show whole roadmap", all, { all = !all })
            }
            items(visible, key = { it.id }) { target -> AcademicTargetCard(pack, roadmap, target, model, onDismiss) }
            item { WorkspaceTextButton(onClick = onDismiss) { Text("Back") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademicCalendar(pack: AcademicPack, today: String, onDismiss: () -> Unit) {
    val colors = LocalLifeOsColors.current
    var semester by rememberSaveable { mutableIntStateOf(1) }
    var past by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.background) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.92f), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("College calendar", color = colors.textPrimary, fontSize = 27.sp)
                Text("Relevant student milestones, not all faculty/admin meetings. Full original table is available below. Exam dates do not specify subjects or times.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(semester == 1, { semester = 1 }, label = { Text("S1 · 2026") })
                    FilterChip(semester == 2, { semester = 2 }, label = { Text("S2 · 2027") })
                }
                WorkspaceTextButton(onClick = { past = !past }) { Text(if (past) "Upcoming only" else "Include past dates") }
                AcademicReference("syllabus://calendar/$semester", "Original calendar")
                if (semester == 2) Text("S2 dates only. S2 syllabus has not been supplied.", color = colors.textSecondary, fontSize = 12.sp)
            }
            items(pack.events.filter { it.semester == semester && (past || it.dates.any { date -> date >= today }) }.sortedBy { it.dates.first() }, key = { it.id }) { event ->
                WorkCard(event.title, event.dates.joinToString(" · ")) {
                    Text("${event.kind.lowercase()} · calendar page ${event.page} · date only", fontSize = 12.sp)
                }
            }
            item { WorkspaceTextButton(onClick = onDismiss) { Text("Back") } }
        }
    }
}

@Composable
private fun AcademicPlanSettings(pack: AcademicPack, roadmap: AcademicRoadmap, model: WorkspaceViewModel, onDismiss: () -> Unit) {
    var minutes by rememberSaveable { mutableStateOf(roadmap.dailyMinutes.toString()) }
    var modules by remember { mutableStateOf(roadmap.modules) }
    val selected = minutes.toIntOrNull()
    val groups = pack.items.filter { it.scope == "THEORY" }.distinctBy { it.moduleKey }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Make the study map fit") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This is a planning budget, not detected availability. Actual placement checks your saved commitments. Refreshing keeps scheduled blocks, history and explicit completions.", fontSize = 12.sp, lineHeight = 19.sp)
            OutlinedTextField(minutes, { minutes = it }, label = { Text("Suggested study minutes/day · 10–180") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, isError = selected == null || selected !in 10..180,
                supportingText = { Text("A whole number from 10 to 180. This changes suggestions, not your saved commitments.") })
            Text("Internal portions are not known. Select only teacher-confirmed modules, or keep the default broad theory scope.", fontSize = 12.sp, lineHeight = 19.sp)
            WorkspaceTextButton(onClick = { modules = emptySet() }) { Text("Use all theory modules") }
            groups.forEach { group -> Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(modules.isEmpty() || group.moduleKey in modules, { checked ->
                    val current = if (modules.isEmpty()) groups.map { it.moduleKey }.toSet() else modules
                    val next = if (checked) current + group.moduleKey else current - group.moduleKey
                    if (next.isNotEmpty()) modules = next
                })
                Text("${pack.courses.first { it.id == group.course }.short} · ${group.module}", fontSize = 12.sp)
            } }
            Text("Chemistry ESE-excluded batteries, CAD, lab lists and Life Skills activities stay in the catalog; they are not fabricated written-exam portions. A weekly portfolio audit is included separately.", fontSize = 12.sp, lineHeight = 19.sp)
        }
    }, confirmButton = { WorkspaceButton(enabled = selected != null && selected in 10..180,
        onClick = { selected?.let { model.replanAcademic(it, modules); onDismiss() } }) { Text("Refresh from today") } },
        dismissButton = { WorkspaceTextButton(onClick = onDismiss) { Text("Keep existing map") } })
}
