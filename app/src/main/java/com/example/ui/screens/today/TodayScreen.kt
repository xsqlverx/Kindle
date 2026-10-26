package com.example.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HabitItem
import com.example.data.model.Quadrant
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.data.model.TimetableEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.CurrentLectureStatus
import com.example.viewmodel.LifeOsViewModel

@Composable
fun TodayToolsScreen(
    viewModel: LifeOsViewModel,
    onNavigateToTab: (LifeOsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val topThree by viewModel.topThreeTasks.collectAsStateWithLifecycle()
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val lectureStatus by viewModel.currentLectureStatus.collectAsStateWithLifecycle()
    val habitRing by viewModel.habitRingProgress.collectAsStateWithLifecycle()
    val waterRing by viewModel.waterRingProgress.collectAsStateWithLifecycle()
    val workoutRing by viewModel.workoutRingProgress.collectAsStateWithLifecycle()
    val waterTodayMl by viewModel.totalWaterTodayMl.collectAsStateWithLifecycle()
    val targetExam by viewModel.targetExam.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // 1. Apple-style Activity Rings & Summary Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_activity_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(lifeColors.primaryAccent)
                            )
                            Text(
                                text = "DAILY PULSE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = lifeColors.primaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "In Flow",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = lifeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Ring Legend Rows
                        LegendRow(
                            label = "Habits",
                            value = "${habits.count { it.isCompletedToday }}/${habits.size}",
                            color = IrisRadiant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LegendRow(
                            label = "Hydration",
                            value = "${waterTodayMl}ml",
                            color = RingWaterCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LegendRow(
                            label = "Workout",
                            value = "${(workoutRing * 100).toInt()}%",
                            color = RingGymRose
                        )
                    }

                    ConcentricActivityRings(
                        habitProgress = habitRing,
                        waterProgress = waterRing,
                        workoutProgress = workoutRing,
                        size = 136.dp,
                        strokeWidth = 12.dp
                    )
                }
            }
        }

        // 2. Current / Next College Class Hero Banner
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("college_schedule_banner"),
                onClick = {
                    playAppleHaptic(context)
                    if (lectureStatus.entry != null) {
                        viewModel.openLectureDetail(lectureStatus.entry!!)
                    } else {
                        onNavigateToTab(LifeOsTab.ACADEMICS)
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (lectureStatus.isOngoing) RingGymRose.copy(alpha = 0.2f)
                                    else lifeColors.primaryAccent.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if (lectureStatus.isOngoing) RingGymRose else lifeColors.primaryAccent,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (lectureStatus.isOngoing) Icons.Default.Timer else Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (lectureStatus.isOngoing) RingGymRose else lifeColors.primaryAccent
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (lectureStatus.isOngoing) "LIVE NOW" else "COLLEGE SCHEDULE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (lectureStatus.isOngoing) RingGymRose else lifeColors.primaryAccent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (targetExam.dateMillis > 0L) {
                                    val days = targetExam.getDaysLeft()
                                    val badgeLabel = if (days != null) {
                                        if (days >= 0) "${targetExam.title.ifBlank { "Exam" }} • ${days}d"
                                        else "${targetExam.title.ifBlank { "Exam" }} • Done"
                                    } else targetExam.title.ifBlank { "Exam Target" }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(lifeColors.primaryAccent.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = lifeColors.primaryAccent,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RingHabitEmerald.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "80% Target",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RingHabitEmerald,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lectureStatus.message,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = lifeColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lectureStatus.timeRemainingOrUntil,
                                fontSize = 12.sp,
                                color = lifeColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Academics",
                        modifier = Modifier.size(16.dp),
                        tint = lifeColors.textTertiary
                    )
                }
            }
        }

        // 3. Top 3 Priority Tasks
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Three Focus",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
                Text(
                    text = "Eisenhower Matrix",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = lifeColors.primaryAccent,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToTab(LifeOsTab.TASKS) }
                )
            }
        }

        if (topThree.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All priority tasks conquered for today!",
                            fontSize = 14.sp,
                            color = lifeColors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(topThree) { task ->
                TaskGlassRow(
                    task = task,
                    onToggle = {
                        playAppleHaptic(context)
                        viewModel.toggleTaskDone(task)
                    }
                )
            }
        }

        // 4. Daily Habits Quick Check-in
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Rituals",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
                Text(
                    text = "${habits.count { it.isCompletedToday }}/${habits.size} done",
                    fontSize = 12.sp,
                    color = lifeColors.textSecondary
                )
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(habits) { habit ->
                    HabitPillItem(
                        habit = habit,
                        onToggle = {
                            playAppleHaptic(context)
                            viewModel.toggleHabit(habit)
                        }
                    )
                }
            }
        }

        // 5. Quick Shortcut to 60-Second Daily Reset
        item {
            Spacer(modifier = Modifier.height(6.dp))
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_reset_shortcut"),
                tint = if (lifeColors.isDark) Color(0x334F46E5) else Color(0x226366F1),
                onClick = { onNavigateToTab(LifeOsTab.RESET) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(lifeColors.primaryAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = lifeColors.primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "60-Second Daily Reset",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = lifeColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Triage leftovers, prep gear & clear mind",
                                fontSize = 12.sp,
                                color = lifeColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(lifeColors.primaryAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Begin",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.primaryAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, value: String, color: Color) {
    val lifeColors = LocalLifeOsColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = lifeColors.textSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = lifeColors.textPrimary
        )
    }
}

@Composable
fun TaskGlassRow(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lifeColors = LocalLifeOsColors.current
    val isDone = (task.status == TaskStatus.COMPLETED)

    val quadColor = when (task.quadrant) {
        Quadrant.Q1_DO -> Q1DoColor
        Quadrant.Q2_SCHEDULE -> Q2ScheduleColor
        Quadrant.Q3_DELEGATE -> Q3DelegateColor
        Quadrant.Q4_ELIMINATE -> Q4DropColor
    }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Apple-style circular checkbox with testTag
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) lifeColors.primaryAccent
                            else Color.Transparent
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isDone) lifeColors.primaryAccent else lifeColors.textTertiary,
                            shape = CircleShape
                        )
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDone) lifeColors.textTertiary else lifeColors.textPrimary,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = lifeColors.primaryAccent
                        )
                        if (task.dueTime.isNotEmpty()) {
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = lifeColors.textTertiary
                            )
                            Text(
                                text = task.dueTime,
                                fontSize = 11.sp,
                                color = lifeColors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quadrant badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(quadColor.copy(alpha = 0.15f))
                        .border(1.dp, quadColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.quadrant.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = quadColor
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = lifeColors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitPillItem(
    habit: HabitItem,
    onToggle: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val isDone = habit.isCompletedToday

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDone) lifeColors.primaryAccent.copy(alpha = 0.22f)
                else lifeColors.surfaceGlass
            )
            .border(
                width = 1.dp,
                color = if (isDone) lifeColors.primaryAccent else lifeColors.borderGlass,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone) lifeColors.primaryAccent else Color.Transparent
                    )
                    .border(
                        1.2.dp,
                        if (isDone) lifeColors.primaryAccent else lifeColors.textTertiary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Column {
                Text(
                    text = habit.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) lifeColors.primaryAccent else lifeColors.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (habit.currentStreak > 0) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = SunsetAmber,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = "${habit.currentStreak}d streak",
                        fontSize = 10.sp,
                        color = lifeColors.textSecondary
                    )
                }
            }
        }
    }
}
