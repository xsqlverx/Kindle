package com.example.ui.screens.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.example.data.model.FocusKind
import com.example.data.model.FocusPhase
import com.example.data.model.FocusSession
import com.example.data.model.TaskStatus
import com.example.ui.components.LifeOsTab
import com.example.ui.theme.DayInk
import com.example.viewmodel.LifeOsViewModel
import com.example.viewmodel.TodayPrototypeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: LifeOsViewModel,
    onNavigateToTab: (LifeOsTab) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val focusModel: TodayPrototypeViewModel = composeViewModel()
    val session by focusModel.session.collectAsStateWithLifecycle()
    val openSessionPanel by focusModel.openSessionPanel.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val water by viewModel.totalWaterTodayMl.collectAsStateWithLifecycle()
    val selectedLecture by viewModel.selectedLectureForDetail.collectAsStateWithLifecycle()
    var momentName by rememberSaveable { mutableStateOf(DayMoment.HOME.name) }
    val moment = DayMoment.valueOf(momentName)
    var recovered by rememberSaveable { mutableStateOf(false) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var showAllTools by rememberSaveable { mutableStateOf(false) }
    var showTaskPicker by rememberSaveable { mutableStateOf(false) }
    var showSession by rememberSaveable { mutableStateOf(false) }
    var chosenTaskId by rememberSaveable { mutableLongStateOf(-1L) }
    var customFocusTitle by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pending = tasks.filter { it.status == TaskStatus.PENDING }.sortedByDescending { it.isTopThree }
    val chosenTask = pending.firstOrNull { it.id == chosenTaskId }
    val focusTitle = chosenTask?.title ?: customFocusTitle.ifBlank {
        pending.firstOrNull()?.title ?: "Review today's notes"
    }

    // Lecture details are drawn by the activity; dismiss this dialog so they remain reachable.
    LaunchedEffect(selectedLecture?.id) {
        if (selectedLecture != null) showAllTools = false
    }

    LaunchedEffect(openSessionPanel) {
        if (openSessionPanel) {
            showSession = true
            focusModel.consumeSessionPanelRequest()
        }
    }

    // Re-read the persisted deadline on resume. There is no timer job while the UI is stopped.
    LaunchedEffect(session.phase, lifecycle) {
        now = System.currentTimeMillis()
        focusModel.refresh()
        if (session.phase == FocusPhase.RUNNING) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    now = System.currentTimeMillis()
                    focusModel.refresh()
                    delay(1000L)
                }
            }
        }
    }

    val openFocus: () -> Unit = {
        focusModel.start(focusTitle)
        showSession = true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(DayInk.Background).testTag("today_prototype"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val date = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())
                    Eyebrow(date.uppercase(Locale.getDefault()))
                    Spacer(Modifier.height(5.dp))
                    Text("Your day", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = DayInk.Text)
                }
                Text(
                    "FIRST LOOK", color = DayInk.Lavender, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    modifier = Modifier.clip(CircleShape).background(DayInk.Raised)
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("today_settings")) {
                    Icon(Icons.Outlined.Tune, "Open settings", tint = DayInk.Muted, modifier = Modifier.size(21.dp))
                }
            }
        }
        item {
            Column {
                AnimatedContent(
                    targetState = moment,
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(120)) },
                    label = "moment_heading"
                ) { shown ->
                    Column {
                        Text(
                            shown.heading, color = DayInk.Text, fontSize = 32.sp,
                            lineHeight = 37.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(shown.caption, color = DayInk.Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
                Spacer(Modifier.height(23.dp))
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DayMoment.entries.forEach { option ->
                        val selected = option == moment
                        val tint by animateColorAsState(
                            if (selected) DayInk.Raised else Color.Transparent, label = "moment_tint"
                        )
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(tint)
                                .border(1.dp, if (selected) DayInk.Line else Color.Transparent, RoundedCornerShape(14.dp))
                                .selectable(selected, role = Role.Tab, onClick = {
                                    momentName = option.name
                                    recovered = false
                                }).heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 12.dp)
                                .testTag("moment_${option.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(option.label, color = if (selected) DayInk.Text else DayInk.Muted,
                                fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        item { DayRibbon(moment) }
        if (session.phase != FocusPhase.IDLE) {
            item {
                SessionStrip(session, now, onClick = { showSession = true })
            }
        }
        item {
            NextMove(
                moment = moment,
                recovered = recovered,
                focusTitle = focusTitle,
                sessionActive = session.isActive,
                onFocus = openFocus,
                onRecover = { recovered = true },
                onCollege = { onNavigateToTab(LifeOsTab.ACADEMICS) },
                onChooseTask = { showTaskPicker = true }
            )
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (recovered) "A smaller plan. Still a good day." else "A little structure",
                        color = DayInk.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Schedule, null, tint = DayInk.Quiet, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text("Suggested times · try a moment above", color = DayInk.Quiet, fontSize = 11.sp)
                Spacer(Modifier.height(20.dp))
                suggestedSteps(moment, recovered).forEachIndexed { index, step ->
                    DayStepRow(step, index == 0, index == suggestedSteps(moment, recovered).lastIndex)
                }
                Spacer(Modifier.height(15.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DayInk.Surface).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.NightsStay, null, tint = DayInk.Lavender, modifier = Modifier.size(19.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your evening has its place.", color = DayInk.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(5.dp))
                        Text("Gym, family prayer, time together.\nBuilt around your usual routine.", color = DayInk.Muted,
                            fontSize = 11.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
        item {
            Column {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { toolsExpanded = !toolsExpanded }
                        .heightIn(min = 56.dp).padding(vertical = 8.dp).testTag("today_expand_tools"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(DayInk.Raised), contentAlignment = Alignment.Center) {
                        Icon(if (toolsExpanded) Icons.Outlined.Close else Icons.Outlined.Add, "Toggle everyday tools",
                            tint = DayInk.Lavender, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your everyday tools", color = DayInk.Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Water, a breather, your daily reset", color = DayInk.Quiet, fontSize = 11.sp)
                    }
                    Icon(if (toolsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        null, tint = DayInk.Muted)
                }
                AnimatedVisibility(
                    visible = toolsExpanded,
                    enter = expandVertically(tween(280)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(220)) + fadeOut(tween(140))
                ) {
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToolRow(Icons.Outlined.WaterDrop, "${water} ml today", "+250 ml · log water", "Add", {
                            viewModel.addWaterMl(250)
                        })
                        ToolRow(Icons.Outlined.Coffee, "Take a breather", "20-minute timer", if (session.isActive) "Session" else "Rest", {
                            focusModel.start("A little breathing room", FocusKind.REST)
                            showSession = true
                        })
                        ToolRow(Icons.Outlined.Bedtime, "Close the day", "Your 60-second daily reset", "Open", {
                            onNavigateToTab(LifeOsTab.RESET)
                        })
                        TextButton(onClick = { showAllTools = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("All daily tools & activity rings", color = DayInk.Lavender, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.ArrowForward, null, tint = DayInk.Lavender, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Keep a few things in sight", fontSize = 17.sp, fontWeight = FontWeight.Medium,
                        color = DayInk.Text, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onNavigateToTab(LifeOsTab.TASKS) }) {
                        Text("All tasks", color = DayInk.Lavender, fontSize = 11.sp)
                    }
                }
                if (pending.isEmpty()) {
                    Text("Choose something that deserves your next 25 minutes.", color = DayInk.Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    TextButton(onClick = { onNavigateToTab(LifeOsTab.TASKS) }) {
                        Text("Add your first task", color = DayInk.Lavender)
                    }
                } else {
                    pending.take(3).forEach { task ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = false, onCheckedChange = { viewModel.toggleTaskDone(task) },
                                colors = CheckboxDefaults.colors(uncheckedColor = DayInk.Quiet, checkedColor = DayInk.Lavender),
                                modifier = Modifier.testTag("prototype_task_${task.id}")
                            )
                            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                                Text(task.title, color = DayInk.Text, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                Text(task.category, color = DayInk.Quiet, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        if (habits.isNotEmpty()) {
            item {
                Column {
                    Eyebrow("SMALL THINGS, KEPT")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(habits, key = { it.id }) { habit ->
                            FilterChip(
                                selected = habit.isCompletedToday, onClick = { viewModel.toggleHabit(habit) },
                                label = { Text(habit.title, fontSize = 12.sp) },
                                leadingIcon = if (habit.isCompletedToday) {
                                    { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DayInk.Surface, labelColor = DayInk.Muted,
                                    selectedContainerColor = DayInk.Raised, selectedLabelColor = DayInk.Lavender,
                                    selectedLeadingIconColor = DayInk.Lavender
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            Text("FIRST LOOK  /  01\nSample moments above. Your saved tools below.", color = DayInk.Quiet,
                fontSize = 10.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp)
        }
    }

    if (showSession) {
        ModalBottomSheet(onDismissRequest = { showSession = false }, containerColor = DayInk.Surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            FocusSessionContent(session, now, onPause = focusModel::pause, onResume = focusModel::resume,
                onEnd = { focusModel.dismiss(); showSession = false })
        }
    }
    if (showTaskPicker) {
        ModalBottomSheet(onDismissRequest = { showTaskPicker = false }, containerColor = DayInk.Surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Text("Give these minutes a purpose.", color = DayInk.Text, fontSize = 24.sp, lineHeight = 30.sp)
                Spacer(Modifier.height(8.dp))
                Text("Pick a saved task or name your next small step.", color = DayInk.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = customFocusTitle,
                    onValueChange = { customFocusTitle = it; chosenTaskId = -1L },
                    label = { Text("What will you work on?") },
                    modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DayInk.Text, unfocusedTextColor = DayInk.Text,
                        focusedBorderColor = DayInk.Lavender, unfocusedBorderColor = DayInk.Line,
                        focusedLabelColor = DayInk.Lavender, unfocusedLabelColor = DayInk.Muted,
                        cursorColor = DayInk.Lavender
                    )
                )
                Spacer(Modifier.height(12.dp))
                DayButton("Use this focus", onClick = { showTaskPicker = false })
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp).padding(top = 12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(pending, key = { it.id }) { task ->
                        Row(Modifier.fillMaxWidth().clickable {
                            chosenTaskId = task.id; customFocusTitle = ""; showTaskPicker = false
                        }.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MenuBook, null, tint = DayInk.Lavender, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(task.title, color = DayInk.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (showAllTools) {
        ModalBottomSheet(onDismissRequest = { showAllTools = false }, containerColor = DayInk.Background,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily tools", color = DayInk.Text, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAllTools = false }) { Icon(Icons.Outlined.Close, "Close daily tools", tint = DayInk.Text) }
                }
                TodayToolsScreen(viewModel, onNavigateToTab = {
                    showAllTools = false
                    onNavigateToTab(it)
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayRibbon(moment: DayMoment) {
    val position by animateFloatAsState(moment.position, tween(600), label = "day_position")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("THE SHAPE OF YOUR DAY")
            Text("Sample · ${moment.sampleTime}", color = DayInk.Lavender, fontSize = 10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(128.dp)) {
            val w = size.width
            val h = size.height
            val ribbon = Path().apply {
                moveTo(w * 0.035f, h * 0.71f)
                cubicTo(w * 0.20f, h * 0.83f, w * 0.28f, h * 0.28f, w * 0.49f, h * 0.40f)
                cubicTo(w * 0.68f, h * 0.50f, w * 0.78f, h * 0.87f, w * 0.965f, h * 0.35f)
            }
            drawPath(ribbon, Color.Black.copy(alpha = 0.35f), style = Stroke(29.dp.toPx(), cap = StrokeCap.Round))
            drawPath(ribbon, Brush.verticalGradient(listOf(Color(0xFF66606F), Color(0xFF292731), Color(0xFF17171E))),
                style = Stroke(22.dp.toPx(), cap = StrokeCap.Round))
            drawPath(ribbon, Brush.horizontalGradient(listOf(DayInk.Lavender.copy(alpha = 0.13f),
                DayInk.Lavender.copy(alpha = 0.48f), DayInk.Lavender.copy(alpha = 0.10f))),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
            val measure = PathMeasure().apply { setPath(ribbon, false) }
            repeat(23) { tick ->
                val point = measure.getPosition(measure.length * tick / 22f)
                drawLine(DayInk.Lavender.copy(alpha = 0.20f), point + Offset(0f, -3.dp.toPx()),
                    point + Offset(0f, 3.dp.toPx()), strokeWidth = 1.dp.toPx())
            }
            val active = measure.getPosition(measure.length * position)
            drawCircle(Brush.radialGradient(listOf(DayInk.Lavender.copy(alpha = 0.24f), Color.Transparent),
                center = active, radius = 52.dp.toPx()), radius = 52.dp.toPx(), center = active)
            drawCircle(DayInk.Lavender.copy(alpha = 0.12f), radius = 14.dp.toPx(), center = active)
            drawCircle(DayInk.Lavender, radius = 6.dp.toPx(), center = active)
            drawCircle(DayInk.Text, radius = 2.5.dp.toPx(), center = active + Offset(-1.dp.toPx(), -1.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RibbonLabel("08:00", "COLLEGE", moment == DayMoment.COLLEGE)
            RibbonLabel("14:45", "HOME", moment == DayMoment.HOME)
            RibbonLabel("20:00", "GYM", false)
        }
    }
}

@Composable
private fun RibbonLabel(time: String, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, color = if (active) DayInk.Text else DayInk.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(5.dp))
        Text(label, color = if (active) DayInk.Lavender else DayInk.Quiet, fontSize = 8.sp, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun NextMove(
    moment: DayMoment, recovered: Boolean, focusTitle: String, sessionActive: Boolean,
    onFocus: () -> Unit, onRecover: () -> Unit, onCollege: () -> Unit, onChooseTask: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF3B3549), Color(0xFF25232E), Color(0xFF1D1D25))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xFF6D627F), Color(0xFF35313F))), shape)
            .padding(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (moment == DayMoment.COLLEGE) Icons.Outlined.School else Icons.Outlined.AutoAwesome,
                null, tint = DayInk.Lavender, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Eyebrow(when {
                moment == DayMoment.COLLEGE -> "YOUR NEXT MOVE"
                moment == DayMoment.BEHIND && !recovered -> "ROOM TO BEGIN AGAIN"
                recovered -> "A FRESH START"
                else -> "AFTER YOU SETTLE IN"
            }, DayInk.Lavender)
        }
        Spacer(Modifier.height(17.dp))
        Text(when {
            moment == DayMoment.COLLEGE -> "Know what's coming next."
            moment == DayMoment.BEHIND && !recovered -> "You don't need to rescue every hour."
            else -> "Give one thing 25 minutes."
        }, color = DayInk.Text, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(10.dp))
        Text(when {
            moment == DayMoment.COLLEGE -> "Your saved timetable, attendance and deadlines are one tap away."
            moment == DayMoment.BEHIND && !recovered -> "Make a smaller plan. Keep space for the evening you already have."
            else -> focusTitle
        }, color = DayInk.Muted, fontSize = 13.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(22.dp))
        DayButton(
            text = when {
                moment == DayMoment.COLLEGE -> "Open College"
                sessionActive -> "Return to your session"
                moment == DayMoment.BEHIND && !recovered -> "Make a smaller plan"
                else -> "Start 25 minutes"
            },
            onClick = when {
                moment == DayMoment.COLLEGE -> onCollege
                sessionActive -> onFocus
                moment == DayMoment.BEHIND && !recovered -> onRecover
                else -> onFocus
            }, modifier = Modifier.testTag("today_primary_action")
        )
        if (moment != DayMoment.COLLEGE) {
            TextButton(onClick = onChooseTask, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 3.dp)) {
                Text("Choose what to work on", color = DayInk.Lavender, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DayStepRow(step: SuggestedDayStep, current: Boolean, last: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)) {
        Text(step.time, fontSize = 10.sp, color = if (current) DayInk.Lavender else DayInk.Quiet,
            fontWeight = FontWeight.Medium, modifier = Modifier.width(54.dp).padding(top = 2.dp))
        Column(Modifier.width(18.dp).height(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (current) DayInk.Lavender else DayInk.Line))
            if (!last) Box(Modifier.width(1.dp).weight(1f).background(DayInk.Line))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, bottom = 20.dp)) {
            Text(step.title, color = if (current) DayInk.Text else DayInk.Muted, fontSize = 13.sp,
                fontWeight = if (current) FontWeight.Medium else FontWeight.Normal)
            Spacer(Modifier.height(5.dp))
            Text(step.detail, color = DayInk.Quiet, fontSize = 11.sp, lineHeight = 17.sp)
        }
        if (step.isAnchor) Icon(Icons.Outlined.Lock, "Usual commitment", tint = DayInk.Quiet, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun ToolRow(icon: ImageVector, title: String, description: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DayInk.Surface).padding(start = 14.dp, end = 5.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DayInk.Lavender, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = DayInk.Text, fontSize = 13.sp)
            Text(description, color = DayInk.Quiet, fontSize = 11.sp)
        }
        TextButton(onClick = onClick) { Text(action, color = DayInk.Lavender, fontSize = 12.sp) }
    }
}

@Composable
private fun SessionStrip(session: FocusSession, now: Long, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DayInk.Raised)
        .clickable(onClick = onClick).padding(17.dp).testTag("today_active_session"), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (session.phase == FocusPhase.FINISHED) Icons.Outlined.CheckCircle else Icons.Outlined.Timelapse,
            null, tint = DayInk.Mint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(when (session.phase) {
                FocusPhase.FINISHED -> "A little progress, made."
                FocusPhase.PAUSED -> "Your session is paused"
                else -> if (session.kind == FocusKind.REST) "A little breathing room" else "One thing at a time"
            }, color = DayInk.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(session.title, color = DayInk.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text(clockText(session.remainingSeconds(now)), color = DayInk.Mint, fontSize = 19.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FocusSessionContent(session: FocusSession, now: Long, onPause: () -> Unit, onResume: () -> Unit, onEnd: () -> Unit) {
    val remaining = session.remainingSeconds(now)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(if (session.kind == FocusKind.REST) "A LITTLE BREATHING ROOM" else "ONE THING AT A TIME")
        Spacer(Modifier.height(25.dp))
        Text(if (session.phase == FocusPhase.FINISHED) "Time well given." else session.title,
            fontSize = 23.sp, lineHeight = 30.sp, color = DayInk.Text, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(25.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                drawArc(DayInk.Line, -90f, 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                val progress = remaining.toFloat() / session.durationSeconds
                drawArc(Brush.linearGradient(listOf(DayInk.Lavender, DayInk.Mint)), -90f, 360f * progress, false,
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clockText(remaining), fontSize = 48.sp, letterSpacing = (-2).sp, color = DayInk.Text, fontWeight = FontWeight.Light)
                Text(when (session.phase) {
                    FocusPhase.PAUSED -> "PAUSED"
                    FocusPhase.FINISHED -> "COMPLETE"
                    else -> "MINUTES, JUST FOR THIS"
                }, color = DayInk.Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("The timer keeps its place when you leave.\nSound and notification alerts aren't connected in this prototype.",
            color = DayInk.Muted, fontSize = 12.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(22.dp))
        if (session.isActive) {
            DayButton(if (session.phase == FocusPhase.PAUSED) "Continue" else "Pause for a moment",
                if (session.phase == FocusPhase.PAUSED) onResume else onPause)
            TextButton(onClick = onEnd) { Text("End this session", color = DayInk.Muted) }
        } else {
            Text("Your task stays open until you mark it complete.", color = DayInk.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            DayButton("Back to my day", onEnd)
        }
    }
}

@Composable
private fun DayButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DayInk.Lavender, contentColor = DayInk.Background),
        shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Eyebrow(text: String, color: Color = DayInk.Quiet) {
    Text(text, color = color, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
}

private fun clockText(seconds: Int): String = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
