package com.example.ui.screens.workspace

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
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
    val academicPack by model.academicPack.collectAsStateWithLifecycle()
    val academicRoadmap by model.academicRoadmap.collectAsStateWithLifecycle()
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))
    val academicMoves = academicPack?.let { SyllabusPlanner.next(it, academicRoadmap, now, topics, attempts) }.orEmpty()
    return DayInputs(now, home, session, blocks, timetable, tasks, assignments, topics, attempts, projects, steps, history, resets.any { it.dateString == date }, academicMoves)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextMoveSheet(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long) {
    val draft by model.moveDraft.collectAsStateWithLifecycle()
    val busy by model.journeyBusy.collectAsStateWithLifecycle()
    val minutesText by model.moveMinutesText.collectAsStateWithLifecycle()
    val input = dayInputs(life, model, focus, now)
    val move = draft ?: return
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val focusManager = LocalFocusManager.current
    val message by model.message.collectAsStateWithLifecycle()
    val choices = DayGuide.library(input)
    var more by rememberSaveable { mutableStateOf(false) }
    var pick by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val minutes = MoveDuration.parse(minutesText)
    val window = minutes?.let { DayGuide.window(input, it, move.blockId) }
    val preview = move.sourceType == "PREVIEW"
    val readyNow = window != null && window <= now + 1000L
    val active = input.session.phase != FocusPhase.IDLE
    val canPlace = !busy && (active || minutes != null && move.title.isNotBlank() && (preview || window != null))
    val allowDismiss by rememberUpdatedState(!busy)
    ModalBottomSheet(onDismissRequest = model::stashMove,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || allowDismiss }),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp), containerColor = colors.background, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f).imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(if (preview) "A QUICK TRYOUT" else "YOUR NEXT MOVE", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.7.sp)
                        Text(if (preview) "Try the whole loop." else "Give it a little room.", color = colors.textPrimary, fontSize = 29.sp,
                            lineHeight = 35.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(10.dp))
                    WorkspaceIconButton(enabled = !busy, onClick = model::stashMove) {
                        Icon(Icons.Outlined.Close, if (preview) "Close tryout" else "Close and keep draft")
                    }
                }
                if (preview) Text("Preview only · your real day stays unchanged.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                MoveField(move.title, { model.updateMove(move.copy(title = it.take(300))) }, "THE ACTION", "What will you move forward?",
                    !busy, Modifier.testTag("next_move_title"), prominent = true)
                if (choices.isNotEmpty()) WorkspaceDisclosure(if (pick) "Hide saved work" else "Use something I've saved", pick,
                    { focusManager.clearFocus(); pick = !pick }, Modifier.fillMaxWidth(), enabled = !busy)
                AnimatedVisibility(pick, enter = fadeIn(tween(if (reduced) 0 else 160)) + expandVertically(tween(if (reduced) 0 else 240)),
                    exit = fadeOut(tween(if (reduced) 0 else 100)) + shrinkVertically(tween(if (reduced) 0 else 180))) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoveField(search, { search = it }, "FIND YOUR WORK", "Search tasks, topics or projects", !busy, maxLines = 1)
                        val found = choices.filter { it.title.contains(search, ignoreCase = true) }
                        LazyColumn(Modifier.heightIn(max = 250.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(found, key = { it.key }) { choice ->
                                WorkspaceActionRow(choice.title, choice.reason, when (choice.sourceType) {
                                    "PRACTICE" -> Icons.Outlined.Code; "PROJECT_STEP" -> Icons.Outlined.FolderOpen;
                                    "TOPIC" -> Icons.Outlined.MenuBook; "ASSIGNMENT" -> Icons.Outlined.Assignment; else -> Icons.Outlined.CheckCircleOutline
                                }) {
                                    if (!busy) {
                                        model.updateMove(if (preview) choice.copy(sourceType = "PREVIEW", sourceId = "", blockId = "", minutes = move.minutes)
                                            else choice.copy(minutes = move.minutes))
                                        pick = false; focusManager.clearFocus()
                                    }
                                }
                            }
                        }
                        if (found.isEmpty()) Text("No matching saved work. You can name an action above.", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                MoveDurationControl(minutesText, !busy, model::updateMoveMinutes)
                if (!preview && (move.sourceId.isNotBlank() || move.sourceType != "CUSTOM")) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(colors.primaryAccent.copy(alpha = 0.07f)).padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Link, null, Modifier.size(17.dp), tint = colors.primaryAccent)
                        Text(move.reason, Modifier.weight(1f), color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                        WorkspaceIconButton(enabled = !busy, onClick = {
                            model.updateMove(move.copy(sourceType = "CUSTOM", sourceId = "", blockId = "", reason = "A small step you choose"))
                        }) { Icon(Icons.Outlined.LinkOff, "Make this a separate action", Modifier.size(18.dp)) }
                    }
                }
                MovePlacementReceipt(minutes, preview, window, now, move.blockId.isNotBlank())
                WorkspaceDisclosure(if (more) "Less detail" else if (move.material.isNotBlank() || move.firstStep.isNotBlank()) "Material & first step · kept" else "Material & first step", more,
                    { more = !more }, Modifier.fillMaxWidth(), enabled = !busy)
                AnimatedVisibility(more, enter = fadeIn(tween(if (reduced) 0 else 160)) + expandVertically(tween(if (reduced) 0 else 240)),
                    exit = fadeOut(tween(if (reduced) 0 else 100)) + shrinkVertically(tween(if (reduced) 0 else 180))) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoveField(move.material, { model.updateMove(move.copy(material = it)) }, "MATERIAL", "A link, book or page", !busy)
                        MoveField(move.firstStep, { model.updateMove(move.copy(firstStep = it)) }, "FIRST STEP", "Where will you begin?", !busy)
                    }
                }
                message?.let { Text(it, color = colors.primaryAccent, fontSize = 12.sp, lineHeight = 18.sp) }
                Text(if (preview) "This tryout stays separate from your real records." else "New actions also live in Tasks. Finishing time never marks the work complete.",
                    color = colors.textTertiary, fontSize = 11.sp, lineHeight = 17.sp)
            }
            HorizontalDivider(color = colors.borderGlass)
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MoveButton(when {
                    busy -> "Finding its place…"; active -> "Return to my current session"; minutes == null -> "Enter your minutes";
                    preview -> "Try ${minutes * 60} seconds"; readyNow -> "Start $minutes ${if (minutes == 1) "minute" else "minutes"}";
                    window != null -> "Save for ${clock(window)}"; else -> "No free window at this duration"
                }, Modifier.fillMaxWidth().testTag("next_move_commit"), enabled = canPlace) {
                    focusManager.clearFocus(); model.placeMove(focus, start = readyNow)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (!preview && readyNow && !active) WorkspaceTextButton(enabled = !busy && minutes != null && move.title.isNotBlank(),
                        onClick = { focusManager.clearFocus(); model.placeMove(focus, start = false) }) { Text("Save to today") }
                    WorkspaceTextButton(onClick = model::stashMove, enabled = !busy) { Text(if (preview) "Back to my day" else "Keep draft") }
                }
            }
        }
    }
}
