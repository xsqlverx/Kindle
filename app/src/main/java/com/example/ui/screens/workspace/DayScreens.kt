package com.example.ui.screens.workspace

import com.example.ui.components.WorkspaceButton
import com.example.ui.components.WorkspaceTextButton
import com.example.ui.components.WorkspaceActionRow
import com.example.ui.components.WorkspaceDisclosure

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.MoveButton
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.*
import java.util.Calendar

@Composable
private fun LiveRibbon(blocks: List<DayBlock>, now: Long) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val start = WorkspaceViewModel.dayStart(now)
    val span = (WorkspaceViewModel.dayEnd(now) - start).toFloat()
    val position by animateFloatAsState(((now - start) / span).coerceIn(0f, 1f), tween(if (reduced) 0 else 650), label = "live_day")
    Column {
        Canvas(Modifier.fillMaxWidth().height(104.dp)) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(size.width * 0.32f, size.height * 0.18f, size.width * 0.64f, size.height * 0.96f, size.width, size.height * 0.35f)
            }
            drawPath(path, colors.primaryAccent.copy(alpha = 0.08f), style = Stroke(32.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, colors.primaryAccent.copy(alpha = 0.25f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            fun y(t: Float): Float {
                val u = 1f - t
                return size.height * (u*u*u*0.72f + 3*u*u*t*0.18f + 3*u*t*t*0.96f + t*t*t*0.35f)
            }
            blocks.forEach { block ->
                val p = ((block.startMillis - start) / span).coerceIn(0f, 1f)
                drawCircle(if (block.fixed) colors.textSecondary else colors.primaryAccent, if (block.fixed) 4.dp.toPx() else 6.dp.toPx(), Offset(size.width * p, y(p)))
            }
            val center = Offset(size.width * position, y(position))
            drawCircle(Brush.radialGradient(listOf(colors.primaryAccent.copy(alpha = 0.4f), Color.Transparent), center, 25.dp.toPx()), 25.dp.toPx(), center)
            drawCircle(colors.textPrimary, 4.dp.toPx(), center)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:00", color = colors.textTertiary, fontSize = 10.sp)
            Text("${clock(now)} · YOUR SAVED DAY", color = colors.primaryAccent, fontSize = 10.sp)
            Text("24:00", color = colors.textTertiary, fontSize = 10.sp)
        }
    }
}

@Composable
fun LiveTodayScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long,
                    onTools: () -> Unit) {
    GuidedTodayScreen(life, model, focus, now, onTools) { model.navigate(Destination.SETTINGS) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long) {
    val blocks by model.blocks.collectAsStateWithLifecycle()
    val undo by model.canUndo.collectAsStateWithLifecycle()
    val session by focus.session.collectAsStateWithLifecycle()
    val timetable by life.allTimetable.collectAsStateWithLifecycle()
    val choices = workChoices(life, model)
    val reduced = LocalReducedMotion.current
    val input = dayInputs(life, model, focus, now)
    var add by rememberSaveable { mutableStateOf(false) }
    var resizeId by rememberSaveable { mutableStateOf<String?>(null) }
    var eveningDraft by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val classes = timetable.filter { it.dayOfWeek == cal.get(Calendar.DAY_OF_WEEK) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SpaceHeading("${dated(now)}", "The day, in order.", "One small move, with room around the things you've committed to.") }
        item {
            MoveButton("Make room for one thing", Modifier.fillMaxWidth()) { model.offerMove(DayGuide.choices(input).firstOrNull() ?: DayMove("")) }
            Spacer(Modifier.height(12.dp))
            WorkspaceActionRow("Add to your day", "Work blocks or fixed commitments", Icons.Outlined.AddCircleOutline) { add = true }
        }
        item { DayAgenda(input, model, onAddCommitment = { add = true }) }
        item { AcademicCalendarBrief(model, now) }
        item {
            WorkspaceActionRow("Evening commitments", "Review the approximate draft before saving", Icons.Outlined.Bedtime) { eveningDraft = true }
            WorkspaceTextButton(onClick = { model.recover(now, WorkspaceViewModel.dayEnd(now), session.blockId) }) { Text("Recover missed starts") }
            if (undo) WorkspaceTextButton(onClick = { model.undoRecovery() }) { Text("Undo last recovery") }
            WorkspaceDisclosure(if (editing) "Hide block controls" else "Block controls & later days", editing,
                { editing = !editing }, Modifier.fillMaxWidth())
        }
        if (editing && classes.isNotEmpty()) item {
            WorkCard("Saved timetable · today", classes.joinToString("\n") {
                "%02d:%02d–%02d:%02d · %s".format(it.startHour, it.startMinute, it.endHour, it.endMinute, it.courseName)
            }) { WorkspaceTextButton(onClick = { model.navigate(Destination.COLLEGE) }) { Text("Edit classes in College") } }
        }
        if (editing) item { LiveRibbon(blocks.filter { it.startMillis in WorkspaceViewModel.dayStart(now) until WorkspaceViewModel.dayEnd(now) }, now) }
        if (editing) items(blocks.filter { it.endMillis >= WorkspaceViewModel.dayStart(now) }, key = { it.id }) { block ->
            WorkCard("${if (block.fixed) "●" else "○"} ${block.title}", "${dated(block.startMillis)} · ${block.durationMinutes} min · ${if (block.fixed) "Fixed" else block.state.lowercase()}",
                Modifier.animateItemPlacement(tween(if (reduced) 0 else 460))) {
                if (block.firstStep.isNotBlank()) Text(block.firstStep)
                if (!block.fixed && block.state == "PLANNED") {
                    WorkspaceTextButton(onClick = { model.begin(focus, block.title, block.sourceType, block.sourceId, block.material, block.firstStep, block.id, block.durationMinutes) }) { Text("Start in a free window now") }
                }
                Row {
                    WorkspaceTextButton(onClick = { resizeId = block.id }) { Text("Duration") }
                    WorkspaceTextButton(onClick = { model.moveBlockTomorrow(block) }) { Text("Tomorrow") }
                    WorkspaceTextButton(onClick = { model.deleteBlock(block) }) { Text("Remove from plan") }
                }
            }
        }
    }
    if (add) PlanDialog(choices, onDismiss = { add = false }) { title, minutes, start, fixed, type, id, material, step ->
        model.schedule(title, minutes, start, WorkspaceViewModel.dayEnd(start), fixed, type, id, material, step)
    }
    blocks.firstOrNull { it.id == resizeId }?.let { block ->
        RecordDialog("Change duration", listOf("Minutes (5–180)"), listOf(block.durationMinutes.toString()), { resizeId = null }) {
            model.resizeBlock(block, it[0].toIntOrNull() ?: 0)
        }
    }
    if (eveningDraft) EveningDraftDialog(now, { eveningDraft = false }) { model.confirmCommitments(it) }
}

@Composable
private fun EveningDraftDialog(now: Long, onDismiss: () -> Unit, onConfirm: (List<DayBlock>) -> Unit) {
    var gym by rememberSaveable { mutableStateOf("20:00") }
    var prayer by rememberSaveable { mutableStateOf("21:30") }
    var call by rememberSaveable { mutableStateOf("22:30") }
    var gymMinutes by rememberSaveable { mutableStateOf("60") }
    var prayerMinutes by rememberSaveable { mutableStateOf("60") }
    var callMinutes by rememberSaveable { mutableStateOf("75") }
    fun time(text: String): Long? {
        val parts = text.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return Calendar.getInstance().apply {
            timeInMillis = now; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val times = listOf(time(gym), time(prayer), time(call))
    val durations = listOf(gymMinutes.toIntOrNull(), prayerMinutes.toIntOrNull(), callMinutes.toIntOrNull())
    val valid = times.all { it != null } && durations.all { it != null && it in 5..180 }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("A draft, until you confirm it") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("These are approximate times from your reported routine. Edit them to match today before saving. This does not record attendance or detect what you are doing.", fontSize = 13.sp)
            OutlinedTextField(gym, { gym = it }, label = { Text("Gym start · 24-hour HH:mm") })
            OutlinedTextField(gymMinutes, { gymMinutes = it }, label = { Text("Gym minutes") })
            OutlinedTextField(prayer, { prayer = it }, label = { Text("Family prayer start · HH:mm") })
            OutlinedTextField(prayerMinutes, { prayerMinutes = it }, label = { Text("Prayer minutes") })
            OutlinedTextField(call, { call = it }, label = { Text("Call start · HH:mm") })
            OutlinedTextField(callMinutes, { callMinutes = it }, label = { Text("Call minutes") })
        }
    }, confirmButton = { WorkspaceButton(enabled = valid, onClick = {
        onConfirm(listOf("Gym", "Family prayer", "Call").mapIndexed { index, title ->
            DayBlock(uid(), title, times[index]!!, durations[index]!!, fixed = true)
        }); onDismiss()
    }) { Text("Confirm these times for today") } }, dismissButton = { WorkspaceTextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PlanDialog(choices: List<WorkChoice>, onDismiss: () -> Unit,
                       onSave: (String, Int, Long, Boolean, String, String, String, String) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("25") }
    var start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var fixed by rememberSaveable { mutableStateOf(false) }
    var material by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("CUSTOM") }
    var id by rememberSaveable { mutableStateOf("") }
    var picker by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Reserve a window") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkspaceTextButton(onClick = { picker = !picker }) { Text("Link saved work") }
            if (picker) Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                choices.forEach { choice -> WorkspaceTextButton(onClick = { title = choice.title; type = choice.type; id = choice.id; material = choice.material; step = choice.step; picker = false }) { Text(choice.title) } }
            }
            OutlinedTextField(title, { title = it }, label = { Text("Action / commitment") })
            if (id.isNotBlank()) WorkspaceTextButton(onClick = { type = "CUSTOM"; id = "" }) { Text("Linked ${type.lowercase()} · unlink") }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(fixed, { fixed = it }); Text("Fixed commitment") }
            Text(if (fixed) "Only save a time you confirm." else "Find the first free window at or after this time.", fontSize = 12.sp)
            WorkspaceTextButton(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = start }
                DatePickerDialog(context, { _, y, m, d ->
                    start = (cal.clone() as Calendar).apply { set(y, m, d) }.timeInMillis
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }) { Text(dated(start)) }
            WorkspaceTextButton(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = start }
                TimePickerDialog(context, { _, h, m -> start = (cal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
            }) { Text("Change start time") }
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Duration · 5–180 minutes") })
            if (!fixed) {
                OutlinedTextField(material, { material = it }, label = { Text("Material / page / link") })
                OutlinedTextField(step, { step = it }, label = { Text("First concrete step") })
            }
        }
    }, confirmButton = { WorkspaceButton(enabled = title.isNotBlank() && (minutes.toIntOrNull() ?: 0) in 5..180, onClick = {
        onSave(title, minutes.toInt(), start, fixed, type, id, material, step); onDismiss()
    }) { Text("Save to Plan") } }, dismissButton = { WorkspaceTextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun StudyScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel) {
    val history by model.history.collectAsStateWithLifecycle()
    val blocks by model.blocks.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SpaceHeading("Study", "Return to the thread.", "Choose actual work, keep the material beside it, and leave a useful place to resume.") }
        item { MoveButton("Choose one move", Modifier.fillMaxWidth()) { model.offerMove() } }
        history.filter { it.sourceType != "FITNESS" && it.sourceType != "REST" }.distinctBy { it.sourceType to it.sourceId.ifBlank { it.title } }.take(6).forEach { record -> item(key = record.id) {
            WorkCard(record.title, "${record.elapsedSeconds / 60} min recorded · ${dated(record.endedAt)}\n${record.resumeNote.ifBlank { "No resume note was saved." }}") {
                if (record.material.isNotBlank()) Text(record.material, fontSize = 13.sp)
                MoveButton("Continue this thread", Modifier.fillMaxWidth()) { model.offerMove(DayMove(record.title, record.sourceType, record.sourceId, record.material, record.resumeNote, "Your saved place")) }
            }
        } }
        items(blocks.filter { !it.fixed && it.state == "PLANNED" }, key = { "plan_${it.id}" }) { block ->
            WorkCard("Planned · ${block.title}", "${dated(block.startMillis)} · ${block.durationMinutes} minutes\n${block.firstStep}") {
                WorkspaceTextButton(onClick = { model.offerMove(DayGuide.fromBlock(block)) }) { Text("Find this work a window") }
            }
        }
        if (history.isEmpty() && blocks.none { !it.fixed }) item { WorkCard("Start with something small", "A task, a deadline, a page of notes or a coding step. Sessions appear here only after you save real work.") }
    }
}
