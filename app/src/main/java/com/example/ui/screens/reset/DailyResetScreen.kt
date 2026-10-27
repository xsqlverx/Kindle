package com.example.ui.screens.reset

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MoodType
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.data.model.TimetableEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.LifeOsViewModel
import com.example.viewmodel.ResetTimerState
import java.util.Calendar

@Composable
fun DailyResetScreen(
    viewModel: LifeOsViewModel,
    onFinishReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allTimetable by viewModel.allTimetable.collectAsStateWithLifecycle()
    val resetTimerState by viewModel.resetTimerState.collectAsStateWithLifecycle()
    val resetTimerSeconds by viewModel.resetTimerSeconds.collectAsStateWithLifecycle()
    val todayResetRecord by viewModel.todayResetRecord.collectAsStateWithLifecycle()

    val pendingTasks = remember(allTasks) {
        allTasks.filter { it.status == TaskStatus.PENDING }
    }

    var currentStep by rememberSaveable { mutableStateOf(1) } // 1: Triage, 2: Brain Dump, 3: Tomorrow Preview, 4: Mood
    var brainDumpText by rememberSaveable { mutableStateOf("") }
    var selectedMood by rememberSaveable { mutableStateOf(MoodType.GOOD) }
    var isRevising by rememberSaveable { mutableStateOf(false) }
    var showCelebrationOverlay by remember { mutableStateOf(false) }

    // Pre-fill state if today's reset already exists
    LaunchedEffect(todayResetRecord) {
        todayResetRecord?.takeUnless { isRevising }?.let { record ->
            brainDumpText = record.brainDump
            selectedMood = record.mood
        }
    }

    // Tomorrow's day of week & timetable
    val tomorrowDayOfWeek = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.get(Calendar.DAY_OF_WEEK)
    }

    val tomorrowClasses = remember(allTimetable, tomorrowDayOfWeek) {
        allTimetable.filter { it.dayOfWeek == tomorrowDayOfWeek }
            .sortedBy { it.startHour * 60 + it.startMinute }
    }

    val tomorrowFirstClass = remember(tomorrowClasses) {
        tomorrowClasses.firstOrNull()
    }

    // Interactive Gear Checklist state
    var earpodsPacked by rememberSaveable { mutableStateOf(false) }
    var phonePacked by rememberSaveable { mutableStateOf(false) }
    var geometryBoxPacked by rememberSaveable { mutableStateOf(false) }
    var calculatorPacked by rememberSaveable { mutableStateOf(false) }
    var booksPacked by rememberSaveable { mutableStateOf(false) }
    var waterBottlePacked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(todayResetRecord?.id) {
        todayResetRecord?.takeUnless { isRevising }?.let { saved ->
            earpodsPacked = saved.gearPacked
            phonePacked = saved.gearPacked
            geometryBoxPacked = saved.gearPacked
            calculatorPacked = saved.gearPacked
            booksPacked = saved.gearPacked
            waterBottlePacked = saved.gearPacked
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val completedRecord = todayResetRecord
        if (completedRecord != null && !isRevising) {
            // Completed state: Show Executive Readiness Report (Single entry active)
            DailyResetReportView(
                record = completedRecord,
                tomorrowFirstClass = tomorrowFirstClass,
                tomorrowClasses = tomorrowClasses,
                tomorrowTasks = pendingTasks,
                onFinish = onFinishReset,
                onRevise = {
                    playAppleHaptic(context)
                    isRevising = true
                    currentStep = 1
                }
            )
        } else {
            // Interactive 60-Second Reset Flow
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                item {
                    val titles = listOf("Leave loose ends here.", "Clear a little headspace.", "Give tomorrow a head start.", "How did today feel?")
                    val descriptions = listOf(
                        "Decide what deserves tomorrow and what can wait.",
                        "A place for the thoughts you don't want to carry to bed.",
                        "Check the first class and the things you want by the door.",
                        "A quick check-in, then let the day be done."
                    )
                    WorkspaceIntro("CLOSE THE DAY  /  0$currentStep OF 04", titles[currentStep - 1], descriptions[currentStep - 1],
                        modifier = Modifier.testTag("daily_reset_hero"))
                    Spacer(Modifier.height(19.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Triage", "Unload", "Prepare", "Check in").forEachIndexed { index, label ->
                            Column(Modifier.weight(1f)) {
                                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                                    .background(if (index < currentStep) lifeColors.primaryAccent else lifeColors.borderGlass))
                                Spacer(Modifier.height(8.dp))
                                Text(label, color = if (index + 1 == currentStep) lifeColors.primaryAccent else lifeColors.textTertiary,
                                    fontSize = 10.sp)
                            }
                        }
                    }
                }
                item {
                    LiquidGlassCard(Modifier.fillMaxWidth().testTag("reset_timer_card")) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            WorkspaceLabel(when (resetTimerState) {
                                ResetTimerState.RUNNING -> "A MINUTE FOR YOURSELF"
                                ResetTimerState.PAUSED -> "TAKE YOUR TIME"
                                ResetTimerState.COMPLETED -> "A MINUTE, GIVEN"
                                else -> "YOUR ONE-MINUTE RESET"
                            })
                            Spacer(Modifier.height(10.dp))
                            Text(String.format(java.util.Locale.getDefault(), "00:%02d", resetTimerSeconds),
                                color = lifeColors.textPrimary, fontSize = 38.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Use the timer if it helps. Move through the steps at your own pace.",
                                color = lifeColors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                WorkspaceButton(onClick = {
                                    playAppleHaptic(context)
                                    when (resetTimerState) {
                                        ResetTimerState.READY, ResetTimerState.RESET -> viewModel.start60sTimer()
                                        ResetTimerState.RUNNING -> viewModel.pause60sTimer()
                                        ResetTimerState.PAUSED -> viewModel.resume60sTimer()
                                        ResetTimerState.COMPLETED -> viewModel.reset60sTimer()
                                    }
                                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                                    Text(when (resetTimerState) {
                                        ResetTimerState.RUNNING -> "Pause"
                                        ResetTimerState.PAUSED -> "Continue"
                                        ResetTimerState.COMPLETED -> "Start fresh"
                                        else -> "Start a minute"
                                    }, fontSize = 13.sp)
                                }
                                WorkspaceTextButton(onClick = { viewModel.reset60sTimer() }) { Text("Reset", color = lifeColors.textSecondary) }
                            }
                        }
                    }
                }

                // Step content with subtle slide & fade transitions
                item {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> (width * 0.35f).toInt() } + fadeIn(tween(260)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { width -> (-width * 0.35f).toInt() } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> (-width * 0.35f).toInt() } + fadeIn(tween(260)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { width -> (width * 0.35f).toInt() } + fadeOut(tween(200)))
                            }
                        },
                        label = "DailyResetStepTransition"
                    ) { step ->
                        when (step) {
                            1 -> StepOneTriageView(
                                pendingTasks = pendingTasks,
                                onSetTriage = { taskId, status ->
                                    viewModel.setTaskTriage(taskId, status)
                                },
                                onNext = {
                                    playAppleHaptic(context)
                                    currentStep = 2
                                }
                            )
                            2 -> StepTwoBrainDumpView(
                                brainDump = brainDumpText,
                                onBrainDumpChange = { brainDumpText = it },
                                onBack = { currentStep = 1 },
                                onNext = {
                                    playAppleHaptic(context)
                                    currentStep = 3
                                }
                            )
                            3 -> StepThreeTomorrowView(
                                tomorrowFirstClass = tomorrowFirstClass,
                                earpodsPacked = earpodsPacked,
                                onToggleEarpods = { earpodsPacked = !earpodsPacked },
                                phonePacked = phonePacked,
                                onTogglePhone = { phonePacked = !phonePacked },
                                geometryBoxPacked = geometryBoxPacked,
                                onToggleGeometry = { geometryBoxPacked = !geometryBoxPacked },
                                calculatorPacked = calculatorPacked,
                                onToggleCalculator = { calculatorPacked = !calculatorPacked },
                                booksPacked = booksPacked,
                                onToggleBooks = { booksPacked = !booksPacked },
                                waterBottlePacked = waterBottlePacked,
                                onToggleWaterBottle = { waterBottlePacked = !waterBottlePacked },
                                onBack = { currentStep = 2 },
                                onNext = {
                                    playAppleHaptic(context)
                                    currentStep = 4
                                }
                            )
                            4 -> StepFourMoodView(
                                selectedMood = selectedMood,
                                onSelectMood = { selectedMood = it },
                                onBack = { currentStep = 3 },
                                onComplete = {
                                    playAppleHaptic(context, strong = true)
                                    viewModel.submitDailyReset(
                                        mood = selectedMood,
                                        brainDump = brainDumpText,
                                        triagedCount = pendingTasks.size,
                                        gearPacked = earpodsPacked && phonePacked && geometryBoxPacked && calculatorPacked && booksPacked && waterBottlePacked
                                    )
                                    isRevising = false
                                    showCelebrationOverlay = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen celebration overlay upon completing the reset
        AnimatedVisibility(
            visible = showCelebrationOverlay,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            FullscreenResetCelebration(
                onDismiss = {
                    showCelebrationOverlay = false
                }
            )
        }
    }
}

@Composable
private fun StepOneTriageView(
    pendingTasks: List<TaskItem>,
    onSetTriage: (Long, TaskStatus) -> Unit,
    onNext: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = "1. Triage Leftover Tasks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Decide the fate of today's remaining items without guilt:",
                fontSize = 12.sp,
                color = lifeColors.textSecondary
            )
        }

        if (pendingTasks.isEmpty()) {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RingHabitEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "All open tasks are resolved!",
                            fontSize = 14.sp,
                            color = lifeColors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            for (task in pendingTasks) {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = task.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = lifeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkspaceTonalButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    onSetTriage(task.id, TaskStatus.MOVED_TOMORROW)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Text("Tomorrow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            WorkspaceTonalButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    onSetTriage(task.id, TaskStatus.MOVED_LATER)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Text("Later", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            WorkspaceOutlinedButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    onSetTriage(task.id, TaskStatus.DROPPED)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Drop", fontSize = 11.sp, color = RingGymRose)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        WorkspaceButton(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Next: Brain Dump", fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StepTwoBrainDumpView(
    brainDump: String,
    onBrainDumpChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = "2. Brain Dump",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Unload random thoughts, ideas, or reminders so your mind stops looping:",
                fontSize = 12.sp,
                color = lifeColors.textSecondary
            )
        }

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = brainDump,
                    onValueChange = onBrainDumpChange,
                    placeholder = { Text("What's lingering in your mind? (e.g., call mom tomorrow, ask professor about project, buy oat milk...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkspaceOutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }

            WorkspaceButton(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Next: Preview Tomorrow", fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StepThreeTomorrowView(
    tomorrowFirstClass: TimetableEntry?,
    earpodsPacked: Boolean,
    onToggleEarpods: () -> Unit,
    phonePacked: Boolean,
    onTogglePhone: () -> Unit,
    geometryBoxPacked: Boolean,
    onToggleGeometry: () -> Unit,
    calculatorPacked: Boolean,
    onToggleCalculator: () -> Unit,
    booksPacked: Boolean,
    onToggleBooks: () -> Unit,
    waterBottlePacked: Boolean,
    onToggleWaterBottle: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = "3. Preview Tomorrow & Gear Checklist",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Know what's coming first and make sure your backpack is ready:",
                fontSize = 12.sp,
                color = lifeColors.textSecondary
            )
        }

        // Tomorrow's First Class
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            tint = lifeColors.primaryAccent.copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(lifeColors.primaryAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = lifeColors.primaryAccent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FIRST CLASS TOMORROW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = lifeColors.primaryAccent
                    )
                    Text(
                        text = tomorrowFirstClass?.let { "${it.courseCode} • ${it.courseName}" } ?: "No morning classes scheduled tomorrow",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.textPrimary
                    )
                    if (tomorrowFirstClass != null) {
                        Text(
                            text = "${String.format("%02d:%02d", tomorrowFirstClass.startHour, tomorrowFirstClass.startMinute)} in ${tomorrowFirstClass.room}",
                            fontSize = 12.sp,
                            color = lifeColors.textSecondary
                        )
                    }
                }
            }
        }

        // Backpack Gear Checklist
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "What Gear to Pack",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )

                WorkspaceTextButton(onClick = {
                    if (!earpodsPacked) onToggleEarpods()
                    if (!phonePacked) onTogglePhone()
                    if (!geometryBoxPacked) onToggleGeometry()
                    if (!calculatorPacked) onToggleCalculator()
                    if (!booksPacked) onToggleBooks()
                    if (!waterBottlePacked) onToggleWaterBottle()
                }) { Text("I've packed everything", fontSize = 12.sp) }

                GearCheckRow("Wireless EarPods / Headphones", Icons.Default.Headphones, earpodsPacked, onToggleEarpods)
                GearCheckRow("Phone & Cable", Icons.Default.Smartphone, phonePacked, onTogglePhone)
                GearCheckRow("Geometry Box", Icons.Default.SquareFoot, geometryBoxPacked, onToggleGeometry)
                GearCheckRow("Scientific Calculator", Icons.Default.Calculate, calculatorPacked, onToggleCalculator)
                GearCheckRow("Course Books & Notes", Icons.Default.MenuBook, booksPacked, onToggleBooks)
                GearCheckRow("Water bottle", Icons.Default.WaterDrop, waterBottlePacked, onToggleWaterBottle)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkspaceOutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }

            WorkspaceButton(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Next: Mood Check", fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StepFourMoodView(
    selectedMood: MoodType,
    onSelectMood: (MoodType) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = "4. Log Today's Mood",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Honest emotional debrief. No judgment, just awareness:",
                fontSize = 12.sp,
                color = lifeColors.textSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoodCard(
                mood = MoodType.DRAINING,
                isSelected = selectedMood == MoodType.DRAINING,
                modifier = Modifier.weight(1f),
                onClick = {
                    playAppleHaptic(context)
                    onSelectMood(MoodType.DRAINING)
                }
            )
            MoodCard(
                mood = MoodType.OKAY,
                isSelected = selectedMood == MoodType.OKAY,
                modifier = Modifier.weight(1f),
                onClick = {
                    playAppleHaptic(context)
                    onSelectMood(MoodType.OKAY)
                }
            )
            MoodCard(
                mood = MoodType.GOOD,
                isSelected = selectedMood == MoodType.GOOD,
                modifier = Modifier.weight(1f),
                onClick = {
                    playAppleHaptic(context)
                    onSelectMood(MoodType.GOOD)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkspaceOutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }

            WorkspaceButton(
                onClick = onComplete,
                modifier = Modifier
                    .weight(2.5f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete 60s Reset & Close Today", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GearCheckRow(
    label: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isChecked) lifeColors.surfaceGlassHighlight else Color.Transparent)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) lifeColors.primaryAccent else lifeColors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = lifeColors.textPrimary,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isChecked) RingHabitEmerald else Color.Transparent)
                .border(1.2.dp, if (isChecked) RingHabitEmerald else lifeColors.textTertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = InkDeep,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MoodCard(
    mood: MoodType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val accent = when (mood) {
        MoodType.DRAINING -> RingGymRose
        MoodType.OKAY -> SunsetAmber
        MoodType.GOOD -> RingHabitEmerald
    }

    val moodIcon = when (mood) {
        MoodType.DRAINING -> Icons.Default.SentimentVeryDissatisfied
        MoodType.OKAY -> Icons.Default.SentimentNeutral
        MoodType.GOOD -> Icons.Default.SentimentSatisfiedAlt
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) accent.copy(alpha = 0.25f)
                else lifeColors.surfaceGlass
            )
            .border(
                1.3.dp,
                if (isSelected) accent else lifeColors.borderGlass,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = moodIcon,
                contentDescription = mood.label,
                tint = if (isSelected) accent else lifeColors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mood.label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accent else lifeColors.textPrimary
            )
        }
    }
}
