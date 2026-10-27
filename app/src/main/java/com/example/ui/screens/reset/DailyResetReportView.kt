package com.example.ui.screens.reset

import com.example.ui.components.WorkspaceButton
import com.example.ui.components.WorkspaceOutlinedButton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyResetRecord
import com.example.data.model.MoodType
import com.example.data.model.Quadrant
import com.example.data.model.TaskItem
import com.example.data.model.TimetableEntry
import com.example.ui.components.WorkspaceIntro
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.playAppleHaptic
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyResetReportView(
    record: DailyResetRecord,
    tomorrowFirstClass: TimetableEntry?,
    tomorrowClasses: List<TimetableEntry>,
    tomorrowTasks: List<TaskItem>,
    onFinish: () -> Unit,
    onRevise: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val formattedCompletionTime = rememberFormattedTime(record.completedAt)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_reset_report_view"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WorkspaceIntro("YOUR RESET IS SAVED", "A little room for tomorrow.",
                "Saved at $formattedCompletionTime. You can revisit or revise your reset below.")
        }

        // Section 1: Tomorrow's Schedule & First Lecture
        item {
            ReportCardHeader(
                title = "Tomorrow's Schedule & First Lecture",
                icon = Icons.Default.School,
                accentColor = lifeColors.primaryAccent
            )

            Spacer(modifier = Modifier.height(6.dp))

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                tint = lifeColors.primaryAccent.copy(alpha = 0.12f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (tomorrowFirstClass != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(lifeColors.primaryAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = lifeColors.primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "FIRST LECTURE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = lifeColors.primaryAccent
                                )
                                Text(
                                    text = "${tomorrowFirstClass.courseCode} • ${tomorrowFirstClass.courseName}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = lifeColors.textPrimary
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%02d:%02d", tomorrowFirstClass.startHour, tomorrowFirstClass.startMinute)} in ${tomorrowFirstClass.room}",
                                    fontSize = 12.sp,
                                    color = lifeColors.textSecondary
                                )
                            }
                        }

                        if (tomorrowClasses.size > 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = lifeColors.borderGlass.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${tomorrowClasses.size} total classes scheduled for tomorrow",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = lifeColors.textSecondary
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = lifeColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "No academic lectures scheduled for tomorrow — Rest & Focus day.",
                                fontSize = 13.sp,
                                color = lifeColors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Offloaded Brain Dump
        item {
            ReportCardHeader(
                title = "Unloaded Brain Dump",
                icon = Icons.Default.Psychology,
                accentColor = SunsetAmber
            )

            Spacer(modifier = Modifier.height(6.dp))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (record.brainDump.isNotBlank()) {
                        Text(
                            text = "\"${record.brainDump.trim()}\"",
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = lifeColors.textPrimary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unloaded from short-term memory to stop mental loops.",
                            fontSize = 11.sp,
                            color = lifeColors.textTertiary
                        )
                    } else {
                        Text(
                            text = "No note saved for this reset.",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = lifeColors.textSecondary
                        )
                    }
                }
            }
        }

        // Section 3: Tasks for Tomorrow & Prioritized Backlog
        item {
            ReportCardHeader(
                title = "Tomorrow's Action Backlog",
                icon = Icons.Default.Assignment,
                accentColor = RingHabitEmerald
            )

            Spacer(modifier = Modifier.height(6.dp))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tomorrowTasks.isEmpty()) {
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
                                text = "No pending tasks in this view.",
                                fontSize = 13.sp,
                                color = lifeColors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        val displayTasks = tomorrowTasks.take(5)
                        for (task in displayTasks) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(lifeColors.surfaceGlassHighlight.copy(alpha = 0.4f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (task.quadrant) {
                                                    Quadrant.Q1_DO -> RingGymRose
                                                    Quadrant.Q2_SCHEDULE -> SunsetAmber
                                                    Quadrant.Q3_DELEGATE -> RingHabitEmerald
                                                    Quadrant.Q4_ELIMINATE -> lifeColors.textTertiary
                                                }
                                            )
                                    )
                                    Text(
                                        text = task.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = lifeColors.textPrimary
                                    )
                                }

                                Text(
                                    text = when (task.quadrant) {
                                        Quadrant.Q1_DO -> "Q1: Do First"
                                        Quadrant.Q2_SCHEDULE -> "Q2: Schedule"
                                        Quadrant.Q3_DELEGATE -> "Q3: Quick"
                                        Quadrant.Q4_ELIMINATE -> "Q4: Low"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = lifeColors.textSecondary
                                )
                            }
                        }

                        if (tomorrowTasks.size > 5) {
                            Text(
                                text = "+${tomorrowTasks.size - 5} more pending tasks ready in Tasks tab",
                                fontSize = 11.sp,
                                color = lifeColors.textTertiary
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Backpack & Essentials Check
        item {
            ReportCardHeader(
                title = if (record.gearPacked) "Your bag is ready" else "Packing still to check",
                icon = if (record.gearPacked) Icons.Default.CheckCircle else Icons.Default.Info,
                accentColor = if (record.gearPacked) RingHabitEmerald else lifeColors.textSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val gearItems = listOf(
                        "Wireless EarPods / Headphones",
                        "Phone & Charging cable",
                        "Geometry Box",
                        "Scientific Calculator",
                        "Course Books & Notes",
                        "3,000ml Water Bottle"
                    )

                    for (gear in if (record.gearPacked) gearItems else emptyList()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = RingHabitEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = gear,
                                fontSize = 13.sp,
                                color = lifeColors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (record.gearPacked) "You confirmed all six packing items in this reset." else "Packing was not fully confirmed. Revisit your reset if you want to finish the checklist.",
                        fontSize = 11.sp,
                        color = lifeColors.textSecondary
                    )
                }
            }
        }

        // Section 5: Logged Sentiment & Mood
        item {
            ReportCardHeader(
                title = "Day Mood & Sentiment",
                icon = Icons.Default.Mood,
                accentColor = when (record.mood) {
                    MoodType.DRAINING -> RingGymRose
                    MoodType.OKAY -> SunsetAmber
                    MoodType.GOOD -> RingHabitEmerald
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val moodAccent = when (record.mood) {
                        MoodType.DRAINING -> RingGymRose
                        MoodType.OKAY -> SunsetAmber
                        MoodType.GOOD -> RingHabitEmerald
                    }

                    val moodIcon = when (record.mood) {
                        MoodType.DRAINING -> Icons.Default.SentimentVeryDissatisfied
                        MoodType.OKAY -> Icons.Default.SentimentNeutral
                        MoodType.GOOD -> Icons.Default.SentimentSatisfiedAlt
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(moodAccent.copy(alpha = 0.2f))
                            .border(1.2.dp, moodAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = moodIcon,
                            contentDescription = null,
                            tint = moodAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Logged as \"${record.mood.label}\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.textPrimary
                        )
                        Text(
                            text = when (record.mood) {
                                MoodType.DRAINING -> "Acknowledged tough day. Resting tonight allows full recovery."
                                MoodType.OKAY -> "Steady day recorded. Balanced progress."
                                MoodType.GOOD -> "High energy logged. Rest up to keep momentum tomorrow."
                            },
                            fontSize = 12.sp,
                            color = lifeColors.textSecondary
                        )
                    }
                }
            }
        }

        // Section 6: Actions
        item {
            Spacer(modifier = Modifier.height(8.dp))

            WorkspaceButton(
                onClick = {
                    playAppleHaptic(context)
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to my day", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            WorkspaceOutlinedButton(
                onClick = {
                    playAppleHaptic(context)
                    onRevise()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Revise Today's Reset Entry", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ReportCardHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color
) {
    val lifeColors = LocalLifeOsColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = lifeColors.textPrimary
        )
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    return androidx.compose.runtime.remember(timestamp) {
        try {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        } catch (_: Exception) {
            "Just now"
        }
    }
}
