package com.example.ui.screens.workspace

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.*

@Composable
fun dayInputs(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long): DayInputs {
    val tasks by life.allTasks.collectAsStateWithLifecycle()
    val assignments by life.assignments.collectAsStateWithLifecycle()
    val timetable by life.allTimetable.collectAsStateWithLifecycle()
    val blocks by model.blocks.collectAsStateWithLifecycle()
    val history by model.history.collectAsStateWithLifecycle()
    val topics by model.topics.collectAsStateWithLifecycle()
    val attempts by model.attempts.collectAsStateWithLifecycle()
    val projects by model.projects.collectAsStateWithLifecycle()
    val steps by model.steps.collectAsStateWithLifecycle()
    val session by focus.session.collectAsStateWithLifecycle()
    val home by model.homeAt.collectAsStateWithLifecycle()
    val resets by life.allDailyResets.collectAsStateWithLifecycle()
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))
    return DayInputs(now, home, session, blocks, timetable, tasks, assignments, topics, attempts, projects, steps, history, resets.any { it.dateString == date })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NextMoveSheet(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long) {
    val draft by model.moveDraft.collectAsStateWithLifecycle()
    val busy by model.journeyBusy.collectAsStateWithLifecycle()
    val input = dayInputs(life, model, focus, now)
    val move = draft ?: return
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val message by model.message.collectAsStateWithLifecycle()
    val choices = DayGuide.library(input)
    var more by rememberSaveable { mutableStateOf(false) }
    var pick by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var customDuration by rememberSaveable { mutableStateOf("") }
    val window = DayGuide.window(input, move.minutes, move.blockId)
    val preview = move.sourceType == "PREVIEW"
    val readyNow = window != null && window <= now + 1000L
    ModalBottomSheet(onDismissRequest = model::stashMove, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(start = 24.dp, end = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (preview) "A QUICK TRYOUT" else "ONE NEXT MOVE", color = colors.primaryAccent, fontSize = 11.sp, letterSpacing = 1.6.sp)
            Text(if (preview) "Try the whole loop." else "Give it a little room.", color = colors.textPrimary, fontSize = 29.sp, fontWeight = FontWeight.Medium)
            Text(if (preview) "Choose an action to rehearse. Focus, minimize, then save your place. This preview leaves your real day and study totals unchanged." else "Pick one action and a duration. Its place in the day comes with it.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            OutlinedTextField(move.title, { model.updateMove(move.copy(title = it.take(300))) }, label = { Text("What will you move forward?") },
                placeholder = { Text("A page to revise, a bug to fix…") }, modifier = Modifier.fillMaxWidth().testTag("next_move_title"), enabled = !busy)
            if (choices.isNotEmpty()) TextButton(onClick = { pick = !pick }, enabled = !busy) { Text(if (pick) "Hide saved work" else "Choose from my saved work") }
            AnimatedVisibility(pick, enter = fadeIn(tween(if (reduced) 0 else 160)), exit = fadeOut(tween(if (reduced) 0 else 100))) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(search, { search = it }, label = { Text("Find work") }, modifier = Modifier.fillMaxWidth())
                    LazyColumn(Modifier.heightIn(max = 220.dp)) {
                        items(choices.filter { it.title.contains(search, ignoreCase = true) }, key = { it.key }) { choice ->
                            TextButton(onClick = { model.updateMove(if (preview) choice.copy(sourceType = "PREVIEW", sourceId = "", blockId = "", minutes = move.minutes) else choice.copy(minutes = move.minutes)); pick = false }) { Text(choice.title) }
                        }
                    }
                }
            }
            if (!preview && (move.sourceId.isNotBlank() || move.sourceType != "CUSTOM")) {
                Text(move.reason, color = colors.textSecondary, fontSize = 12.sp)
                TextButton(onClick = { model.updateMove(move.copy(sourceType = "CUSTOM", sourceId = "", blockId = "", reason = "A small step you choose")) }, enabled = !busy) { Text("Make this a separate action") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (move.minutes == 1) listOf(1, 5, 10, 25) else listOf(5, 10, 25, 45)).forEach { minutes ->
                    FilterChip(move.minutes == minutes, { customDuration = ""; model.updateMove(move.copy(minutes = minutes)) }, enabled = !busy, label = { Text("${minutes}m") })
                }
            }
            BackdropGlass(Modifier.fillMaxWidth(), radius = 20) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(when { preview -> "Preview · ${move.minutes * 60} seconds"; readyNow -> "Fits now · ${clock(now)}–${clock(now + move.minutes * 60_000L)}"; window != null -> "A free window · ${clock(window)}–${clock(window + move.minutes * 60_000L)}"; else -> "No room at this duration today" }, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(if (preview) "No class, plan block or task is changed." else if (window != null) "Around your saved classes and commitments." else "Try fewer minutes, or choose tomorrow in Plan.", color = colors.textSecondary, fontSize = 12.sp)
                    if (move.blockId.isNotBlank()) Text("Updates this existing block; its source and material stay linked.", color = colors.textSecondary, fontSize = 11.sp)
                }
            }
            TextButton(onClick = { more = !more; if (!more) customDuration = "" }, enabled = !busy) { Text(if (more) "Keep it simple" else if (move.material.isNotBlank() || move.firstStep.isNotBlank()) "Material & resume point · kept" else "Add a page, link or first step") }
            AnimatedVisibility(more, enter = fadeIn(tween(if (reduced) 0 else 160)), exit = fadeOut(tween(if (reduced) 0 else 100))) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(customDuration, { value ->
                        customDuration = value.filter(Char::isDigit).take(3)
                        customDuration.toIntOrNull()?.takeIf { it in 1..180 }?.let { model.updateMove(move.copy(minutes = it)) }
                    }, label = { Text("Other duration · 1–180 minutes") }, placeholder = { Text(move.minutes.toString()) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                        isError = customDuration.isNotBlank() && (customDuration.toIntOrNull() ?: 0) !in 1..180)
                    OutlinedTextField(move.material, { model.updateMove(move.copy(material = it)) }, label = { Text("Material / link / page") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                    OutlinedTextField(move.firstStep, { model.updateMove(move.copy(firstStep = it)) }, label = { Text("First tiny step") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                }
            }
            message?.let { Text(it, color = colors.primaryAccent, fontSize = 12.sp, lineHeight = 18.sp) }
            MoveButton(if (busy) "Finding its place…" else if (input.session.phase != FocusPhase.IDLE) "Return to my current session" else if (preview) "Try ${move.minutes * 60} seconds" else if (readyNow) "Start ${move.minutes} ${if (move.minutes == 1) "minute" else "minutes"}" else if (window != null) "Save for ${clock(window)}" else "Try a smaller step",
                Modifier.fillMaxWidth().testTag("next_move_commit"), enabled = !busy && (input.session.phase != FocusPhase.IDLE || (preview || window != null) && move.title.isNotBlank() && (customDuration.isBlank() || (customDuration.toIntOrNull() ?: 0) in 1..180))) {
                model.placeMove(focus, start = readyNow)
            }
            if (!preview && readyNow && input.session.phase == FocusPhase.IDLE) TextButton(onClick = { model.placeMove(focus, start = false) }, enabled = !busy && move.title.isNotBlank() && (customDuration.isBlank() || (customDuration.toIntOrNull() ?: 0) in 1..180)) { Text("Just put it in my day") }
            TextButton(onClick = model::stashMove, enabled = !busy) { Text(if (preview) "Back to my day" else "Keep this draft for later") }
            Text(if (preview) "The tryout is separate from your real records." else "New actions also live in Tasks. Saving time never marks work complete.", color = colors.textTertiary, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
