package com.example.ui.screens.workspace

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GuidedTodayScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, now: Long,
                      onTools: () -> Unit, onSettings: () -> Unit) {
    val input = dayInputs(life, model, focus, now)
    val cue = DayGuide.cue(input)
    val saved by focus.lastSaved.collectAsStateWithLifecycle()
    val trial by focus.lastTrial.collectAsStateWithLifecycle()
    val firstLap by model.showFirstLap.collectAsStateWithLifecycle()
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    var capture by rememberSaveable { mutableStateOf(false) }
    var showWholeDay by rememberSaveable { mutableStateOf(false) }
    val todayFocus = input.history.filter { it.endedAt in DayGuide.startOfDay(now)..now && it.sourceType != "FITNESS" && it.sourceType != "REST" }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().testTag("guided_today"),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(now)), color = colors.textSecondary, fontSize = 12.sp)
                    Text("Today", color = colors.textPrimary, fontSize = 27.sp, fontWeight = FontWeight.Medium)
                }
                WorkspaceIconButton(onClick = { model.offerMove() }) { Icon(Icons.Outlined.Add, "Add a next move", tint = colors.primaryAccent) }
                Spacer(Modifier.width(8.dp))
                WorkspaceIconButton(onClick = onSettings) { Icon(Icons.Outlined.Tune, "Settings", tint = colors.textSecondary) }
            }
        }
        item { WeekTrace(input) }
        item {
            AnimatedContent(Triple(saved, cue, trial), transitionSpec = {
                (fadeIn(tween(if (reduced) 0 else 220)) + slideInVertically(tween(if (reduced) 0 else 340)) { it / 12 }) togetherWith
                    fadeOut(tween(if (reduced) 0 else 130))
            }, label = "next_move_change") { target ->
                val record = target.first
                if (target.third != null) TrialEndCard(requireNotNull(target.third), model, focus)
                else if (record != null) SavedProgressCard(record, input, model, focus)
                else NextMoveHero(target.second, input, model, focus)
            }
        }
        item { AcademicCalendarBrief(model, now) }
        if (input.session.phase == FocusPhase.IDLE && firstLap) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.primaryAccent.copy(alpha = 0.06f))
                .clickable { model.offerMove(DayMove("", "PREVIEW", reason = "A safe daily-loop preview", minutes = 1)) }.padding(16.dp).testTag("first_lap"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PlayCircleOutline, null, tint = colors.primaryAccent, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Try the daily loop · 60 sec", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Choose a task → focus → keep your place", color = colors.textSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = model::hideFirstLap) { Icon(Icons.Outlined.Close, "Hide first lap; replay it from Settings", tint = colors.textSecondary, modifier = Modifier.size(16.dp)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("The shape of today", color = colors.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                    Text("${clock(now)} · classes and your saved plan", color = colors.textSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.width(10.dp))
                WorkspaceDisclosure(if (showWholeDay) "Less" else "Whole day", showWholeDay,
                    { showWholeDay = !showWholeDay }, Modifier.testTag("today_whole_day"))
            }
            Spacer(Modifier.height(12.dp))
            DayAgenda(input, model, showWholeDay)
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(colors.surfaceGlass).border(1.dp, colors.borderGlass, RoundedCornerShape(24.dp))) {
                Row(Modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    WorkspaceShortcut("College", Icons.Outlined.School, Modifier.weight(1f)) { model.navigate(Destination.COLLEGE) }
                    VerticalDivider(Modifier.height(28.dp), color = colors.borderGlass)
                    WorkspaceShortcut("Fitness", Icons.Outlined.FitnessCenter, Modifier.weight(1f)) { model.navigate(Destination.FITNESS) }
                    VerticalDivider(Modifier.height(28.dp), color = colors.borderGlass)
                    WorkspaceShortcut("Capture", Icons.Outlined.EditNote, Modifier.weight(1f)) { capture = true }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.borderGlass)
                WorkspaceActionRow("Everyday tools", "Habits, water & daily essentials", Icons.Outlined.Widgets,
                    Modifier.testTag("today_everyday_tools"), embedded = true, onClick = onTools)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (todayFocus.isEmpty()) "Your next return can be easier." else "${todayFocus.sumOf { it.elapsedSeconds } / 60} minutes kept today.", color = colors.textPrimary, fontSize = 16.sp)
                    Text(if (todayFocus.isEmpty()) "One saved line is enough to pick up the thread." else "From ${todayFocus.size} saved session(s). The little starts stay visible.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                WorkspaceIconButton(onClick = { model.navigate(Destination.REVIEW) }) { Icon(Icons.Outlined.History, "Review saved progress", tint = colors.primaryAccent) }
            }
            Spacer(Modifier.height(14.dp))
            WorkspaceActionRow("Daily Reset", "Close the day. Keep a place for tomorrow.", Icons.Outlined.Bedtime) {
                model.navigate(Destination.RESET)
            }
        }
    }
    if (capture) RecordDialog("Catch the thought", listOf("Note, idea or link"), onDismiss = { capture = false }) {
        model.saveCapture(CaptureNote(uid(), it[0], System.currentTimeMillis()))
    }
}

@Composable
private fun WeekTrace(input: DayInputs) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val monday = Calendar.getInstance().apply {
        timeInMillis = DayGuide.startOfDay(input.now)
        add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(7) { index ->
            val cal = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            val start = cal.timeInMillis
            val end = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            val seconds = input.history.filter { it.endedAt in start until end && it.sourceType != "REST" && it.sourceType != "FITNESS" }.sumOf { it.elapsedSeconds }
            val today = input.now in start until end
            val traceWidth by animateDpAsState(if (seconds > 0) 14.dp else 4.dp, if (reduced) snap() else spring(0.7f, 400f), label = "earned_trace")
            Column(Modifier.weight(1f).clip(RoundedCornerShape(17.dp)).background(if (today) colors.primaryAccent.copy(alpha = 0.16f) else Color.Transparent)
                .padding(vertical = 9.dp).semantics { contentDescription = "${SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(cal.time)}, ${seconds / 60} recorded focus minutes${if (today) ", today" else ""}" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).take(2), color = colors.textSecondary, fontSize = 10.sp)
                Text(cal.get(Calendar.DAY_OF_MONTH).toString(), color = if (today) colors.primaryAccent else colors.textPrimary, fontSize = 15.sp, fontWeight = if (today) FontWeight.Bold else FontWeight.Normal)
                Box(Modifier.width(traceWidth).height(3.dp).clip(CircleShape)
                    .background(if (seconds > 0) colors.primaryAccent else colors.textTertiary.copy(alpha = 0.3f)))
            }
        }
    }
}

@Composable
private fun NextMoveHero(cue: DayCue, input: DayInputs, model: WorkspaceViewModel, focus: TodayPrototypeViewModel) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val markOrigin = LocalFocusOrigin.current
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val move = cue.move
    val suggestedMinutes = if (cue.stage == DayStage.RECOVER) 10 else move?.minutes ?: 25
    val window = move?.let { DayGuide.window(input, suggestedMinutes, it.blockId) }
    val label = when (cue.stage) {
        DayStage.FIRST_STEP -> "Choose my first move"
        DayStage.ARRIVE -> "I'm home · start fresh"
        DayStage.FOCUS -> if (input.session.sourceType == "PREVIEW") "Open my tryout" else if (input.session.phase == FocusPhase.PAUSED) "Return to my session" else "Open my focus"
        DayStage.FINISH -> "Save where I stopped"
        DayStage.CLASS -> "Open this class"
        DayStage.COMMITMENT -> "See my plan"
        DayStage.CLOSE -> if (input.resetDone) "Open my reflection" else "Close today"
        DayStage.RECOVER -> "Make it 10 minutes"
        DayStage.READY -> if (window != null && window <= input.now + 1000) "Give it $suggestedMinutes minutes" else "Find its next window"
    }
    val stageLabel = when (cue.stage) {
        DayStage.CLASS, DayStage.COMMITMENT -> "ALREADY IN YOUR DAY"
        DayStage.ARRIVE -> "A SOFTER LANDING"
        DayStage.RECOVER -> "A FRESH WINDOW"
        DayStage.FINISH -> "KEEP THE THREAD"
        DayStage.FOCUS -> "ONE THING AT A TIME"
        DayStage.CLOSE -> "ROOM FOR TOMORROW"
        else -> "YOUR NEXT MOVE"
    }
    Column(Modifier.fillMaxWidth().onGloballyPositioned { bounds = it.boundsInRoot() }.clip(RoundedCornerShape(32.dp))
        .background(Brush.linearGradient(listOf(colors.primaryAccent.copy(alpha = 0.16f), colors.surfaceGlass, colors.secondaryAccent.copy(alpha = 0.055f))))
        .border(1.dp, Brush.linearGradient(listOf(colors.primaryAccent.copy(alpha = 0.44f), colors.borderGlass, colors.primaryAccent.copy(alpha = 0.12f))), RoundedCornerShape(32.dp))
        .padding(24.dp).testTag("next_move_hero"), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stageLabel, Modifier.weight(1f), color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Icon(when (cue.stage) { DayStage.CLASS -> Icons.Outlined.School; DayStage.CLOSE -> Icons.Outlined.Bedtime; DayStage.RECOVER -> Icons.Outlined.Refresh; DayStage.FOCUS -> Icons.Outlined.Timelapse; else -> Icons.Outlined.AutoAwesome },
                null, tint = colors.primaryAccent, modifier = Modifier.size(22.dp))
        }
        val title = when (cue.stage) {
            DayStage.READY, DayStage.RECOVER -> move?.title ?: cue.title
            DayStage.FOCUS -> input.session.title
            else -> cue.title
        }
        Text(title, color = colors.textPrimary, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium)
        if (move != null && cue.stage != DayStage.ARRIVE) {
            if (cue.stage == DayStage.RECOVER) Text("A smaller start still counts.", color = colors.primaryAccent, fontSize = 14.sp)
            if (move.firstStep.isNotBlank()) Text(move.firstStep, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 3)
        }
        Text(if (input.session.sourceType == "PREVIEW") "Preview only. Your real plan and study totals stay unchanged." else if (cue.stage == DayStage.FOCUS) "Keep your attention here. Your session stays with you across the app." else cue.detail,
            color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        if (cue.anchor != null) Text("${clock(cue.anchor.start)}–${clock(cue.anchor.end)}", color = colors.primaryAccent, fontSize = 13.sp)
        if (window != null && move != null && cue.stage in listOf(DayStage.READY, DayStage.RECOVER)) {
            val nextAnchor = DayGuide.anchors(input).firstOrNull { it.start > window }
            Text(if (window <= input.now + 1000) "$suggestedMinutes minutes fit now" + (nextAnchor?.let { " · before ${it.title}" } ?: "") else "Next room at ${clock(window)}",
                color = colors.textPrimary, fontSize = 12.sp)
        }
        MoveButton(label, Modifier.fillMaxWidth().testTag("today_primary")) {
            markOrigin(bounds)
            when (cue.stage) {
                DayStage.FIRST_STEP -> model.offerMove()
                DayStage.ARRIVE -> model.checkIn()
                DayStage.FOCUS, DayStage.FINISH -> focus.requestSessionPanel()
                DayStage.CLASS -> model.navigate(Destination.COLLEGE)
                DayStage.COMMITMENT -> model.navigate(Destination.PLAN)
                DayStage.CLOSE -> model.navigate(Destination.RESET)
                else -> model.offerMove(move?.copy(minutes = suggestedMinutes) ?: DayMove(""))
            }
        }
        val hour = Calendar.getInstance().apply { timeInMillis = input.now }.get(Calendar.HOUR_OF_DAY)
        if (cue.stage == DayStage.FIRST_STEP && hour in 13..18 && input.homeAt !in DayGuide.startOfDay(input.now)..input.now) {
            WorkspaceTextButton(onClick = model::checkIn) { Text("I'm home · mark the transition") }
        }
        if (cue.stage == DayStage.ARRIVE || input.homeAt in DayGuide.startOfDay(input.now)..input.now && cue.stage == DayStage.READY) {
            WorkspaceTextButton(onClick = { markOrigin(bounds); model.begin(focus, "A little breathing room", "REST", minutes = 5, kind = FocusKind.REST) }) { Text("Land gently · take 5 minutes") }
        } else if (cue.stage == DayStage.READY || cue.stage == DayStage.RECOVER) {
            WorkspaceTextButton(onClick = { model.offerMove(DayMove("")) }) { Text("Choose something else") }
        }
        if (cue.stage in listOf(DayStage.FIRST_STEP, DayStage.READY, DayStage.RECOVER, DayStage.FOCUS, DayStage.FINISH)) {
            SculptedLoop(when (cue.stage) { DayStage.FOCUS -> 1; DayStage.FINISH -> 2; else -> 0 })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("CHOOSE", "FOCUS", "KEEP").forEachIndexed { index, title ->
                    val active = when (cue.stage) { DayStage.FOCUS -> 1; DayStage.FINISH -> 2; else -> 0 }
                    Text("${index + 1}  $title", color = if (index == active) colors.primaryAccent else colors.textTertiary, fontSize = 9.sp, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}

@Composable
private fun SculptedLoop(step: Int) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val position = remember { Animatable(0f) }
    LaunchedEffect(step, reduced) {
        if (reduced) position.snapTo(step / 2f) else position.animateTo(step / 2f, spring(0.82f, 140f))
    }
    Canvas(Modifier.fillMaxWidth().height(26.dp)) {
        val path = Path().apply {
            moveTo(4.dp.toPx(), size.height * 0.72f)
            cubicTo(size.width * 0.3f, -size.height * 0.05f, size.width * 0.65f, size.height * 1.05f, size.width - 4.dp.toPx(), size.height * 0.3f)
        }
        drawPath(path, colors.primaryAccent.copy(alpha = 0.08f), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, colors.primaryAccent.copy(alpha = 0.24f), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
        val measure = PathMeasure().apply { setPath(path, false) }
        val point = measure.getPosition(measure.length * position.value.coerceIn(0f, 1f))
        drawCircle(Brush.radialGradient(listOf(colors.primaryAccent.copy(alpha = 0.5f), Color.Transparent), point, 12.dp.toPx()), 12.dp.toPx(), point)
        drawCircle(colors.primaryAccent, 3.dp.toPx(), point)
    }
}

@Composable
private fun SavedProgressCard(record: WorkSessionRecord, input: DayInputs, model: WorkspaceViewModel, focus: TodayPrototypeViewModel) {
    val colors = LocalLifeOsColors.current
    val canComplete = when (record.sourceType) {
        "TASK" -> input.tasks.any { it.id.toString() == record.sourceId && it.status == TaskStatus.PENDING }
        "ASSIGNMENT" -> input.assignments.any { it.id.toString() == record.sourceId && !it.isSubmitted }
        "PROJECT_STEP" -> input.steps.any { it.id == record.sourceId && !it.done }
        else -> false
    }
    val completed = when (record.sourceType) {
        "TASK" -> input.tasks.any { it.id.toString() == record.sourceId && it.status == TaskStatus.COMPLETED }
        "ASSIGNMENT" -> input.assignments.any { it.id.toString() == record.sourceId && it.isSubmitted }
        "PROJECT_STEP" -> input.steps.any { it.id == record.sourceId && it.done }
        else -> false
    }
    BackdropGlass(Modifier.fillMaxWidth().testTag("saved_progress"), radius = 32) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SavedBloom(record.id)
                Column(Modifier.weight(1f)) {
                    Text("PROGRESS SAVED", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.4.sp)
                    Text(if (record.elapsedSeconds < 60) "${record.elapsedSeconds} seconds saved." else "${record.elapsedSeconds / 60} minutes saved.", color = colors.textPrimary, fontSize = 25.sp, lineHeight = 31.sp)
                }
            }
            Text(record.title, color = colors.textPrimary, fontSize = 17.sp)
            Text(record.resumeNote.ifBlank { "Your session is in Review. Leave yourself a place to resume next time." }, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            MoveButton("Back to my day", Modifier.fillMaxWidth()) { focus.clearSavedFeedback() }
            if (canComplete) WorkspaceOutlinedButton(onClick = { model.completeSource(record) }, modifier = Modifier.fillMaxWidth()) {
                Text(when (record.sourceType) { "ASSIGNMENT" -> "Also mark this submitted"; "PROJECT_STEP" -> "Also finish this milestone"; else -> "Also mark this task done" })
            }
            if (completed) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = colors.primaryAccent)
                Text("Marked complete by you.", color = colors.primaryAccent, fontSize = 14.sp)
            }
            Text("Only you decide whether the work is complete.", color = colors.textTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TrialEndCard(trial: TrialResult, model: WorkspaceViewModel, focus: TodayPrototypeViewModel) {
    val colors = LocalLifeOsColors.current
    BackdropGlass(Modifier.fillMaxWidth(), radius = 32) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SavedBloom(trial.id, description = "Preview complete")
            Text("That's the daily loop.", color = colors.textPrimary, fontSize = 29.sp, fontWeight = FontWeight.Medium)
            Text("${trial.elapsedSeconds} seconds tried · your real day is unchanged", color = colors.primaryAccent, fontSize = 12.sp)
            Text("${trial.title}\n${trial.resumeNote.ifBlank { "In a real session, leave a line to make returning easier." }}", color = colors.textSecondary, fontSize = 14.sp, lineHeight = 21.sp)
            Text("For real work: choose → focus → save your place. Your time and note then appear in Study and Review.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            MoveButton("Pick my real next move", Modifier.fillMaxWidth()) { focus.clearSavedFeedback(); model.offerMove() }
            WorkspaceTextButton(onClick = focus::clearSavedFeedback) { Text("Back to my day") }
        }
    }
}

@Composable
fun DayAgenda(input: DayInputs, model: WorkspaceViewModel, wholeDay: Boolean = true, onAddCommitment: () -> Unit = { model.navigate(Destination.PLAN) }) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val anchors = DayGuide.anchors(input)
    val blocks = input.blocks.filter { !it.fixed && it.startMillis in DayGuide.startOfDay(input.now) until DayGuide.endOfDay(input.now) }
    val rows = (anchors.map { AgendaRow(it.id, it.title, it.start, it.end, true, it.destination, null) } +
        blocks.map { AgendaRow(it.id, it.title, it.startMillis, it.endMillis, false, Destination.STUDY, it) }).sortedBy { it.start }
    val visible = if (wholeDay) rows else rows.filter { it.end > input.now || it.block?.state == "REVIEWED" }.take(4)
    if (rows.isEmpty()) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surfaceGlass).padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("A little room is waiting.", color = colors.textPrimary, fontSize = 16.sp)
            Text("Your next move will appear here. Classes and confirmed plans sit beside it.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            WorkspaceTextButton(onClick = onAddCommitment) { Text("Add a commitment") }
        }
    } else if (visible.isEmpty()) {
        Text("The planned part of today has settled. Make a new move, or leave tomorrow a starting point.", color = colors.textSecondary, fontSize = 13.sp)
    } else Column(Modifier.animateContentSize(if (reduced) snap() else tween(240))) {
        visible.forEach { row -> key(row.id) {
            val current = input.now in row.start until row.end && (row.fixed || row.block?.state == "PLANNED")
            val saved = row.block?.state == "REVIEWED"
            val highlight by animateFloatAsState(if (current) 1f else 0f, tween(if (reduced) 0 else 380), label = "agenda_now")
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 85.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(58.dp).padding(top = 14.dp)) {
                    Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(row.start)), color = if (current) colors.primaryAccent else colors.textSecondary, fontSize = 12.sp)
                    Text(SimpleDateFormat("a", Locale.getDefault()).format(Date(row.start)), color = colors.textTertiary, fontSize = 9.sp)
                }
                Canvas(Modifier.width(14.dp).fillMaxHeight()) {
                    drawLine(colors.borderGlass, Offset(center.x, 0f), Offset(center.x, size.height), 1.dp.toPx())
                    drawCircle(if (saved || current) colors.primaryAccent else colors.textTertiary, if (current) 5.dp.toPx() else 3.dp.toPx(), Offset(center.x, 22.dp.toPx()))
                }
                Column(Modifier.weight(1f).padding(start = 10.dp, bottom = 10.dp).clip(RoundedCornerShape(19.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.035f + 0.10f * highlight))
                    .clickable { row.block?.takeIf { it.state == "PLANNED" }?.let { model.offerMove(DayGuide.fromBlock(it)) } ?: model.navigate(if (saved || row.block?.state == "DONE") Destination.REVIEW else row.destination) }
                    .padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(row.title, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
                    Text(when { saved -> "Session saved · work completion stays separate"; row.block?.state == "DONE" -> "Marked complete by you"; row.block?.state == "DEFERRED" -> "Deferred in Tasks"; row.block?.state == "DROPPED" -> "Removed from active work"; row.fixed -> "${clock(row.end)} · fixed"; current -> "${clock(row.end)} · your window is open"; else -> "${(row.end - row.start) / 60_000} min · flexible" },
                        color = if (saved || current) colors.primaryAccent else colors.textSecondary, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        } }
    }
}

private data class AgendaRow(val id: String, val title: String, val start: Long, val end: Long, val fixed: Boolean, val destination: Destination, val block: DayBlock?)
