package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AttendanceRecord
import com.example.data.model.TimetableEntry
import com.example.ui.theme.LocalLifeOsColors
import com.example.ui.theme.RingGymRose
import com.example.ui.theme.RingHabitEmerald
import com.example.ui.theme.RingWaterCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Returns the fully-expanded, human-readable name of a course.
 * Ensures acronyms like ATP, MAT, BEE are converted to full names.
 */
fun getFullCourseName(entry: TimetableEntry): String {
    val code = entry.courseCode.trim().uppercase()
    val baseName = entry.courseName.trim()

    val knownExpansions = mapOf(
        "ATP" to "ALGORITHMIC THINKING WITH PYTHON",
        "ATP LAB" to "ALGORITHMIC THINKING WITH PYTHON (LAB)",
        "MAT" to "LINEAR ALGEBRA & CALCULUS",
        "CHY" to "ENGINEERING CHEMISTRY",
        "CHY LAB" to "ENGINEERING CHEMISTRY LABORATORY",
        "CHY LAB/EE-EC W/S" to "CHEMISTRY LAB & ENGINEERING WORKSHOP",
        "BEE" to "BASIC ELECTRICAL ENGINEERING",
        "BEC" to "BASIC ELECTRONICS ENGINEERING",
        "EG" to "ENGINEERING GRAPHICS & DESIGN",
        "LS" to "LIFE SKILLS & PROFESSIONAL ETHICS",
        "CSE" to "COMPUTER SCIENCE & ENGINEERING",
        "DSA" to "DATA STRUCTURES & ALGORITHMS",
        "DBMS" to "DATABASE MANAGEMENT SYSTEMS",
        "OS" to "OPERATING SYSTEMS",
        "CN" to "COMPUTER NETWORKS"
    )

    return when {
        // If the database courseName has meaningful words and isn't just the code
        baseName.length > code.length + 3 && !baseName.equals(code, ignoreCase = true) -> {
            baseName.uppercase()
        }
        knownExpansions.containsKey(code) -> {
            knownExpansions[code]!!
        }
        else -> {
            baseName.ifBlank { code }.uppercase()
        }
    }
}

/**
 * Checks if a TimetableEntry is happening right now.
 */
fun isEntryOngoingNow(entry: TimetableEntry): Boolean {
    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
    if (entry.dayOfWeek != currentDay) return false

    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val startMinutes = entry.startHour * 60 + entry.startMinute
    val endMinutes = entry.endHour * 60 + entry.endMinute

    return currentMinutes in startMinutes until endMinutes
}

/**
 * Formats hour and minute into "hh:mm AM/PM"
 */
fun formatTimeAmPm(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%02d:%02d %s", displayHour, minute, period)
}

/**
 * Modern 60% slide-up Bottom Sheet with background blur and
 * Google Play Games achievement-style toast notification when the lecture is ongoing.
 */
@Composable
fun LectureDetailSheet(
    entry: TimetableEntry?,
    isOngoingOverride: Boolean? = null,
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    onDismiss: () -> Unit,
    attendanceControl: (@Composable (TimetableEntry) -> Unit)? = null
) {
    if (entry == null) return

    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Bouncy spring animation controller for sheet entrance and exit
    val sheetAnim = remember { Animatable(0f) }

    LaunchedEffect(entry.id) {
        sheetAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.74f, // Tactile bouncy spring
                stiffness = 360f
            )
        )
    }

    val requestDismiss: () -> Unit = {
        coroutineScope.launch {
            sheetAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = 450f
                )
            )
            onDismiss()
        }
    }

    BackHandler {
        requestDismiss()
    }

    val isOngoing = isOngoingOverride ?: isEntryOngoingNow(entry)

    // State for Google Play Games achievement popup
    var showOngoingToast by remember { mutableStateOf(false) }

    // Trigger achievement popup when inspected during ongoing lecture
    LaunchedEffect(entry.id, isOngoing) {
        if (isOngoing) {
            // Small initial delay for entrance visual sync
            delay(180)
            showOngoingToast = true
            playAppleHaptic(context, strong = true)
            // Auto dismiss after 3.6 seconds
            delay(3600)
            showOngoingToast = false
        }
    }

    val fullCourseName = remember(entry) { getFullCourseName(entry) }
    val startTimeFormatted = remember(entry) { formatTimeAmPm(entry.startHour, entry.startMinute) }
    val endTimeFormatted = remember(entry) { formatTimeAmPm(entry.endHour, entry.endMinute) }

    val durationMinutes = remember(entry) {
        (entry.endHour * 60 + entry.endMinute) - (entry.startHour * 60 + entry.startMinute)
    }
    val durationText = remember(durationMinutes) {
        val hrs = durationMinutes / 60
        val mins = durationMinutes % 60
        when {
            hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
            hrs > 0 -> "${hrs} hr"
            else -> "${mins} min"
        }
    }

    // Attendance stats for this course
    val record = attendanceRecords.find { it.courseCode.equals(entry.courseCode, ignoreCase = true) }
    val attended = record?.attendedClasses ?: 0
    val total = record?.totalClasses ?: 0
    val target = record?.targetPercentage ?: 80
    val pct = if (total > 0) (attended * 100 / total) else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(300f)
    ) {
        // 1. High-Performance Glass Scrim (Zero GPU lag, buttery 60/120fps)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030509).copy(alpha = (0.72f * sheetAnim.value).coerceIn(0f, 0.72f)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    requestDismiss()
                }
        )

        // 2. Google Play Games Achievement-style Toast at the Top Center
        AnimatedVisibility(
            visible = showOngoingToast,
            enter = slideInVertically(
                initialOffsetY = { -it * 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it * 2 },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 42.dp, start = 20.dp, end = 20.dp)
                .zIndex(350f)
        ) {
            GooglePlayAchievementToast(
                title = "CAMPUS LIVE SESSION",
                subtitle = "This lecture is ongoing"
            )
        }

        // 3. Bottom Sheet Container (Bouncy Spring Physics, occupies ~60% screen height)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
                .graphicsLayer {
                    val progress = sheetAnim.value
                    translationY = (1f - progress) * size.height
                    alpha = progress.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E2435),
                            Color(0xFF101420)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            lifeColors.borderGlass.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* absorb clicks */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
                    .verticalScroll(scrollState)
            ) {
                // Top Drag Handle with Downward Drag-to-Dismiss & Close Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 16f) {
                                    requestDismiss()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(lifeColors.borderGlass.copy(alpha = 0.85f))
                            .clickable { requestDismiss() }
                    )

                    IconButton(
                        onClick = { requestDismiss() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(lifeColors.surfaceGlass)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = lifeColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Course Code Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(lifeColors.primaryAccent.copy(alpha = 0.18f))
                                .border(1.dp, lifeColors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = entry.courseCode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = lifeColors.primaryAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Live / Ongoing or Scheduled Indicator
                        if (isOngoing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RingHabitEmerald.copy(alpha = 0.2f))
                                    .border(1.dp, RingHabitEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                PulsingLiveDot(color = RingHabitEmerald)
                                Text(
                                    text = "ONGOING CLASS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = RingHabitEmerald,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(lifeColors.surfaceGlass)
                                    .border(1.dp, lifeColors.borderGlass.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(RingWaterCyan)
                                )
                                Text(
                                    text = "SCHEDULED CLASS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = RingWaterCyan,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // MAIN FOCUS: LECTURE NAME IN FULL FORM
                    Text(
                        text = fullCourseName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 28.sp,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_lecture_name")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // TIME CARD: Starts From and Ends At
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141A28))
                            .border(1.dp, lifeColors.borderGlass.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Lecture Time",
                                        tint = if (isOngoing) RingHabitEmerald else lifeColors.primaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "CLASS TIMING",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = lifeColors.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1F283E))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = durationText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Big Time Display: Start Time → End Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "STARTS AT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lifeColors.textSecondary
                                    )
                                    Text(
                                        text = startTimeFormatted,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "→",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOngoing) RingHabitEmerald else lifeColors.primaryAccent
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ENDS AT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lifeColors.textSecondary
                                    )
                                    Text(
                                        text = endTimeFormatted,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Details Grid: Room & Professor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Room Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF171F32))
                                .border(1.dp, lifeColors.borderGlass.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = RingWaterCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "ROOM / HALL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = lifeColors.textSecondary
                                    )
                                }
                                Text(
                                    text = entry.room.ifBlank { "Classroom" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Professor Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF171F32))
                                .border(1.dp, lifeColors.borderGlass.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = lifeColors.primaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "FACULTY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = lifeColors.textSecondary
                                    )
                                }
                                Text(
                                    text = entry.professor.ifBlank { "Department Faculty" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Attendance summary for this course
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141926))
                            .border(1.dp, lifeColors.borderGlass.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RingHabitEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$target% Target",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RingHabitEmerald,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Text(
                                text = "Logged Attendance",
                                fontSize = 12.sp,
                                color = lifeColors.textSecondary
                            )
                        }

                        Text(
                            text = if (total > 0) "$pct% ($attended/$total)" else "No logs yet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (total > 0 && pct >= target) RingHabitEmerald else if (total > 0) RingGymRose else lifeColors.textSecondary
                        )
                    }

                    // One shared dated-class control, backed by the persistent attendance ledger.
                    if (attendanceControl != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        attendanceControl(entry)
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }

/**
 * Google Play Games style floating achievement notification popup.
 */
@Composable
private fun GooglePlayAchievementToast(
    title: String,
    subtitle: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "achievement_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = RingHabitEmerald)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        RingHabitEmerald.copy(alpha = 0.85f),
                        RingWaterCyan.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left circular icon with green gradient & pulse (Google Play achievement look)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                RingHabitEmerald,
                                Color(0xFF047857)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = RingHabitEmerald,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right live status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RingHabitEmerald)
            )
        }
    }
}

@Composable
private fun PulsingLiveDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
