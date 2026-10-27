package com.example.ui.screens.academics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceProjection
import com.example.data.model.AssignmentItem
import com.example.data.model.AttendanceRecord
import com.example.data.model.ExamTarget
import com.example.data.model.TimetableEntry
import com.example.ui.components.WorkspaceIntro
import com.example.ui.components.WorkspaceLabel
import com.example.ui.components.LectureDetailSheet
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.playAppleHaptic
import com.example.ui.theme.LocalLifeOsColors
import com.example.ui.theme.RingGymRose
import com.example.ui.theme.RingHabitEmerald
import com.example.ui.theme.RingWaterCyan
import com.example.viewmodel.LifeOsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun AcademicsScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier,
    onStartWork: ((AssignmentItem) -> Unit)? = null
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val timetable by viewModel.dayTimetable.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val lectureStatus by viewModel.currentLectureStatus.collectAsStateWithLifecycle()
    val targetExam by viewModel.targetExam.collectAsStateWithLifecycle()

    var showConfigureExamDialog by remember { mutableStateOf(false) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var assignmentToEdit by remember { mutableStateOf<AssignmentItem?>(null) }
    var assignmentToDelete by remember { mutableStateOf<AssignmentItem?>(null) }
    var classToDelete by remember { mutableStateOf<TimetableEntry?>(null) }

    val days = listOf(
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // 1. Academic Target & First Exam Banner (Redesigned per Req 3)
        item {
            AcademicTargetBanner(
                targetExam = targetExam,
                attendanceRecords = attendanceRecords,
                onConfigureExam = { showConfigureExamDialog = true },
                testTag = "academic_exam_banner"
            )
        }

        // 2. Active / Live Lecture Status Pill
        if (lectureStatus.isOngoing || lectureStatus.timeRemainingOrUntil.isNotBlank()) {
            item {
                ActiveLectureCard(
                    lectureStatus = lectureStatus,
                    onClick = {
                        playAppleHaptic(context)
                        if (lectureStatus.entry != null) {
                            viewModel.openLectureDetail(lectureStatus.entry!!)
                        } else if (timetable.isNotEmpty()) {
                            viewModel.openLectureDetail(timetable.first())
                        }
                    }
                )
            }
        }

        // 3. Timetable Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Timetable",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.textPrimary
                    )
                    Text(
                        text = "Class schedules & lecture halls",
                        fontSize = 12.sp,
                        color = lifeColors.textSecondary
                    )
                }

                FilledTonalButton(
                    onClick = { showAddClassDialog = true },
                    modifier = Modifier.testTag("add_class_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = lifeColors.primaryAccent.copy(alpha = 0.15f),
                        contentColor = lifeColors.primaryAccent
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Class", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        // Day Selector Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(days) { (dayInt, dayLabel) ->
                    val isSelected = (dayInt == selectedDay)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) lifeColors.primaryAccent
                                else lifeColors.surfaceGlass
                            )
                            .border(
                                1.dp,
                                if (isSelected) lifeColors.primaryAccent else lifeColors.borderGlass,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                playAppleHaptic(context)
                                viewModel.setSelectedDay(dayInt)
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("day_tab_$dayLabel")
                    ) {
                        Text(
                            text = dayLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else lifeColors.textPrimary
                        )
                    }
                }
            }
        }

        // Timetable Items
        if (timetable.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventBusy,
                                contentDescription = null,
                                tint = lifeColors.textTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No classes scheduled for this day",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = lifeColors.textSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(timetable, key = { it.id }) { classEntry ->
                TimetableGlassCard(
                    entry = classEntry,
                    onClick = {
                        playAppleHaptic(context)
                        viewModel.openLectureDetail(classEntry)
                    },
                    onDelete = { classToDelete = classEntry }
                )
            }
        }

        // 4. Attendance Monitor Section (Redesigned per Req 4 & 5)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance Monitor",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.textPrimary
                    )
                    Text(
                        text = "Based on your saved class counts",
                        fontSize = 12.sp,
                        color = lifeColors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(lifeColors.primaryAccent.copy(alpha = 0.15f))
                        .border(1.dp, lifeColors.primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Your targets",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.primaryAccent
                    )
                }
            }
        }

        items(attendanceRecords, key = { it.courseCode }) { record ->
            AttendanceGlassRow(
                record = record,
                onLogAttendance = { attended ->
                    playAppleHaptic(context)
                    viewModel.recordAttendance(record.courseCode, attended)
                },
                onUpdate = { attended, total, target -> viewModel.correctAttendance(record.courseCode, attended, total, target) }
            )
        }

        // 5. Project Deadlines Section (CRUD per Req 6)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Project Deadlines",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.textPrimary
                    )
                    Text(
                        text = "Milestones & grade weight trackers",
                        fontSize = 12.sp,
                        color = lifeColors.textSecondary
                    )
                }

                FilledTonalButton(
                    onClick = { showAddAssignmentDialog = true },
                    modifier = Modifier.testTag("add_deadline_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = lifeColors.primaryAccent.copy(alpha = 0.15f),
                        contentColor = lifeColors.primaryAccent
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Deadline", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        if (assignments.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = lifeColors.textTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No active deadlines recorded",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = lifeColors.textSecondary
                            )
                            Button(
                                onClick = { showAddAssignmentDialog = true },
                                modifier = Modifier.padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Project Deadline", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            items(assignments, key = { it.id }) { assignment ->
                AssignmentGlassCard(
                    assignment = assignment,
                    onToggle = {
                        playAppleHaptic(context)
                        viewModel.toggleAssignment(assignment)
                    },
                    onEdit = { assignmentToEdit = assignment },
                    onDelete = { assignmentToDelete = assignment },
                    onStartWork = if (onStartWork != null && !assignment.isSubmitted) ({ onStartWork(assignment) }) else null
                )
            }
        }
    }

    // Modal: Add Class to Timetable
    if (showAddClassDialog) {
        var courseCode by remember { mutableStateOf("") }
        var courseName by remember { mutableStateOf("") }
        var room by remember { mutableStateOf("") }
        var professor by remember { mutableStateOf("") }
        var startHour by remember { mutableStateOf("10") }
        var startMin by remember { mutableStateOf("00") }

        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = { Text("Add Timetable Class", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code (e.g. CS301)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Course Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Room / Hall") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = professor,
                            onValueChange = { professor = it },
                            label = { Text("Professor") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { startHour = it },
                            label = { Text("Start Hour (0-23)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = startMin,
                            onValueChange = { startMin = it },
                            label = { Text("Start Min (0-59)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (courseCode.isNotBlank()) {
                            val sh = startHour.toIntOrNull() ?: 10
                            val sm = startMin.toIntOrNull() ?: 0
                            val eh = if (sm + 50 >= 60) sh + 1 else sh
                            val em = (sm + 50) % 60
                            viewModel.addTimetableEntry(
                                TimetableEntry(
                                    courseCode = courseCode.trim().uppercase(),
                                    courseName = courseName.ifBlank { "Lecture" },
                                    dayOfWeek = selectedDay,
                                    startHour = sh,
                                    startMinute = sm,
                                    endHour = eh,
                                    endMinute = em,
                                    room = room.ifBlank { "A213" },
                                    professor = professor.trim()
                                )
                            )
                            showAddClassDialog = false
                        }
                    }
                ) {
                    Text("Add to Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Add Assignment / Deadline (Create)
    if (showAddAssignmentDialog) {
        DeadlineEditorDialog(
            initialAssignment = null,
            onDismiss = { showAddAssignmentDialog = false },
            onSave = { code, title, desc, cat, daysAhead, weight ->
                val dueMillis = System.currentTimeMillis() + (daysAhead * 24L * 3600L * 1000L)
                viewModel.addAssignment(
                    courseCode = code,
                    title = title,
                    dueDateMillis = dueMillis,
                    description = desc,
                    category = cat,
                    weightPercentage = weight
                )
                showAddAssignmentDialog = false
            }
        )
    }

    // Modal: Edit Assignment (Update)
    assignmentToEdit?.let { itemToEdit ->
        DeadlineEditorDialog(
            initialAssignment = itemToEdit,
            onDismiss = { assignmentToEdit = null },
            onSave = { code, title, desc, cat, daysAhead, weight ->
                val newDueMillis = System.currentTimeMillis() + (daysAhead * 24L * 3600L * 1000L)
                viewModel.updateAssignment(
                    itemToEdit.copy(
                        courseCode = code,
                        title = title,
                        description = desc,
                        category = cat,
                        dueDateMillis = newDueMillis,
                        weightPercentage = weight
                    )
                )
                assignmentToEdit = null
            }
        )
    }

    // Modal: Delete Assignment Confirmation
    assignmentToDelete?.let { itemToDelete ->
        AlertDialog(
            onDismissRequest = { assignmentToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RingGymRose,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete Deadline?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove '${itemToDelete.title}' (${itemToDelete.courseCode})? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        playAppleHaptic(context, strong = true)
                        viewModel.deleteAssignment(itemToDelete)
                        assignmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RingGymRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { assignmentToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Modal: Delete Class Confirmation
    classToDelete?.let { entryToDelete ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("Remove Class?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Remove '${entryToDelete.courseCode} - ${entryToDelete.courseName}' from your schedule?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        playAppleHaptic(context, strong = true)
                        viewModel.deleteTimetableEntry(entryToDelete)
                        classToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RingGymRose)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Configure Exam Date directly from College Hub
    if (showConfigureExamDialog) {
        var examTitle by remember { mutableStateOf(targetExam.title.ifBlank { "Midterm Examination" }) }
        var selectedMillis by remember {
            mutableStateOf(
                if (targetExam.dateMillis > 0L) targetExam.dateMillis
                else System.currentTimeMillis() + 14 * 86400000L
            )
        }

        val formattedDate = remember(selectedMillis) {
            val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            sdf.format(Date(selectedMillis))
        }

        AlertDialog(
            onDismissRequest = { showConfigureExamDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = lifeColors.primaryAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Configure Target Exam",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Set your upcoming exam name and target date to track the countdown in College Hub.",
                        fontSize = 13.sp,
                        color = lifeColors.textSecondary,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = examTitle,
                        onValueChange = { examTitle = it },
                        label = { Text("Exam Name / Milestone") },
                        placeholder = { Text("e.g., S1 Midterm Exam, Finals") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Target Date",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = lifeColors.textSecondary
                        )

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedMillis
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth, 9, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        selectedMillis = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = lifeColors.primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = formattedDate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = lifeColors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "Change",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = lifeColors.primaryAccent
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        playAppleHaptic(context)
                        viewModel.setTargetExam(examTitle.trim().ifBlank { "Upcoming Exam" }, selectedMillis)
                        showConfigureExamDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lifeColors.primaryAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Exam Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (targetExam.dateMillis > 0L) {
                        TextButton(
                            onClick = {
                                playAppleHaptic(context)
                                viewModel.clearTargetExam()
                                showConfigureExamDialog = false
                            }
                        ) {
                            Text("Clear", color = RingGymRose)
                        }
                    }
                    TextButton(onClick = { showConfigureExamDialog = false }) {
                        Text("Cancel", color = lifeColors.textSecondary)
                    }
                }
            },
            containerColor = lifeColors.surfaceGlass,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AcademicTargetBanner(
    targetExam: ExamTarget,
    attendanceRecords: List<AttendanceRecord>,
    onConfigureExam: () -> Unit,
    testTag: String
) {
    val colors = LocalLifeOsColors.current
    val days = targetExam.getDaysLeft()
    Column(Modifier.fillMaxWidth().testTag(testTag)) {
        WorkspaceIntro("ON YOUR HORIZON", "Stay a step ahead.", "Classes, deadlines and a little more clarity.")
        Spacer(Modifier.height(16.dp))
        LiquidGlassCard(Modifier.fillMaxWidth(), onClick = onConfigureExam) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                WorkspaceLabel(if (days == null) "YOUR NEXT MILESTONE" else "EXAM COUNTDOWN")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (days == null) "Give yourself a date." else targetExam.title.ifBlank { "Upcoming exam" },
                            color = colors.textPrimary, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text(if (days == null) "Tap to set an exam or milestone" else targetExam.getFormattedDate(),
                            color = colors.textSecondary, fontSize = 12.sp)
                    }
                    if (days != null) {
                        Column(Modifier.padding(start = 16.dp), horizontalAlignment = Alignment.End) {
                            Text(days.coerceAtLeast(0).toString(), color = colors.primaryAccent,
                                fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                            WorkspaceLabel(if (days < 0) "PASSED" else if (days == 1) "DAY LEFT" else "DAYS LEFT")
                        }
                    } else Icon(Icons.Outlined.Add, "Set exam date", tint = colors.primaryAccent)
                }
                if (attendanceRecords.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    val attended = attendanceRecords.sumOf { it.attendedClasses.toLong() }
                    val total = attendanceRecords.sumOf { it.totalClasses.toLong() }
                    Text(if (total > 0L) "${attended * 100L / total}% overall · $attended/$total saved classes"
                        else "${attendanceRecords.size} courses · no classes logged yet", color = colors.textTertiary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ActiveLectureCard(lectureStatus: com.example.viewmodel.CurrentLectureStatus, onClick: () -> Unit) {
    val colors = LocalLifeOsColors.current
    LiquidGlassCard(Modifier.fillMaxWidth().testTag("active_lecture_tile"), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(when {
                lectureStatus.isOngoing -> "IN CLASS NOW"
                lectureStatus.entry != null -> "UP NEXT"
                else -> "TIMETABLE"
            }, color = if (lectureStatus.isOngoing) RingHabitEmerald else colors.primaryAccent,
                fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text(lectureStatus.entry?.courseName ?: lectureStatus.message,
                color = colors.textPrimary, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(lectureStatus.timeRemainingOrUntil, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TimetableGlassCard(entry: TimetableEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalLifeOsColors.current
    LiquidGlassCard(Modifier.fillMaxWidth().testTag("timetable_card_${entry.id}"), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(56.dp).padding(top = 2.dp)) {
                    Text(String.format(Locale.getDefault(), "%02d:%02d", entry.startHour, entry.startMinute),
                        color = colors.primaryAccent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(7.dp))
                    Text(String.format(Locale.getDefault(), "%02d:%02d", entry.endHour, entry.endMinute),
                        color = colors.textTertiary, fontSize = 11.sp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    WorkspaceLabel(entry.courseCode)
                    Spacer(Modifier.height(5.dp))
                    Text(entry.courseName, color = colors.textPrimary, fontSize = 15.sp, lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(7.dp))
                    Text(listOf(entry.room, entry.professor).filter { it.isNotBlank() }.joinToString(" · "),
                        color = colors.textSecondary, fontSize = 11.sp, lineHeight = 17.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Delete ${entry.courseCode} class", tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AttendanceGlassRow(record: AttendanceRecord, onLogAttendance: (Boolean) -> Unit, onUpdate: (Int, Int, Int) -> Unit) {
    val colors = LocalLifeOsColors.current
    var adjust by rememberSaveable(record.courseCode) { mutableStateOf(false) }
    val valid = record.attendedClasses >= 0 && record.totalClasses >= record.attendedClasses && record.targetPercentage in 1..100
    val projection = if (valid) AttendanceProjection(record.attendedClasses, record.totalClasses, record.targetPercentage) else null
    val accent = if (projection?.isAtTarget == true) RingHabitEmerald else if (record.totalClasses == 0) colors.primaryAccent else RingGymRose
    val progress by animateFloatAsState(((projection?.percentage ?: 0) / 100f).coerceIn(0f, 1f), tween(350), label = "attendance_${record.courseCode}")
    LiquidGlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.courseCode, color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("${record.attendedClasses} of ${record.totalClasses} attended", color = colors.textSecondary, fontSize = 12.sp)
                }
                Text(projection?.percentage?.let { "$it%" } ?: "—", color = accent, fontSize = 32.sp, fontWeight = FontWeight.Light)
            }
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.surfaceGlassHighlight)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(accent))
            }
            Text(when {
                projection == null -> "Check your saved totals."
                projection.percentage == null -> "Add your baseline to start tracking."
                projection.isAtTarget -> "At ${record.targetPercentage}% target · ${projection.absenceBuffer} class buffer"
                projection.classesNeeded == null -> "A 100% target cannot be recovered after a recorded absence."
                else -> "Attend the next ${projection.classesNeeded} classes to reach ${record.targetPercentage}%."
            }, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { onLogAttendance(true) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = RingHabitEmerald.copy(alpha = 0.12f), contentColor = RingHabitEmerald)) {
                    Text("Present", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { onLogAttendance(false) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Absent", color = colors.textSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = { adjust = true }) { Icon(Icons.Outlined.Edit, "Correct attendance totals", tint = colors.primaryAccent, modifier = Modifier.size(19.dp)) }
            }
        }
    }
    if (adjust) AttendanceBaselineDialog(record, onDismiss = { adjust = false }, onSave = { attended, total, target ->
        onUpdate(attended, total, target)
        adjust = false
    })
}

@Composable
private fun AttendanceBaselineDialog(record: AttendanceRecord, onDismiss: () -> Unit, onSave: (Int, Int, Int) -> Unit) {
    var attended by remember { mutableStateOf(record.attendedClasses.toString()) }
    var total by remember { mutableStateOf(record.totalClasses.toString()) }
    var target by remember { mutableStateOf(record.targetPercentage.toString()) }
    val a = attended.toIntOrNull()
    val t = total.toIntOrNull()
    val goal = target.toIntOrNull()
    val valid = a != null && t != null && goal != null && a >= 0 && t >= a && goal in 1..100
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("${record.courseCode} · correct totals", fontWeight = FontWeight.Medium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Copy the counts from your college portal. This changes only this course's saved attendance.", fontSize = 13.sp, lineHeight = 20.sp)
                OutlinedTextField(attended, { attended = it }, label = { Text("Classes attended") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(total, { total = it }, label = { Text("Total classes held") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(target, { target = it }, label = { Text("Your target (%)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                if (!valid) Text("Attended must be between 0 and total. Target must be 1–100%.", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = {
            if (valid) onSave(requireNotNull(a), requireNotNull(t), requireNotNull(goal))
        }) { Text("Save totals") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AssignmentGlassCard(assignment: AssignmentItem, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onStartWork: (() -> Unit)? = null) {
    val colors = LocalLifeOsColors.current
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val overdue = !assignment.isSubmitted && assignment.dueDateMillis < today
    val date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(assignment.dueDateMillis))
    LiquidGlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WorkspaceLabel("${assignment.courseCode}  /  ${assignment.category.uppercase(Locale.getDefault())}")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Checkbox(assignment.isSubmitted, { onToggle() }, modifier = Modifier.testTag("assignment_check_${assignment.id}"))
                Column(Modifier.weight(1f).padding(start = 8.dp, top = 8.dp)) {
                    Text(assignment.title, color = if (assignment.isSubmitted) colors.textTertiary else colors.textPrimary,
                        fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium,
                        textDecoration = if (assignment.isSubmitted) TextDecoration.LineThrough else TextDecoration.None)
                    if (assignment.description.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Text(assignment.description, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            }
            if (assignment.weightPercentage > 0) Text("${assignment.weightPercentage}% of course grade", color = colors.textTertiary, fontSize = 11.sp)
            if (onStartWork != null) TextButton(onClick = onStartWork) { Text("Work on this deadline") }
            HorizontalDivider(color = colors.borderGlass)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(when {
                    assignment.isSubmitted -> "Submitted · $date"
                    overdue -> "Overdue · $date"
                    else -> "Due $date"
                }, color = if (overdue) RingGymRose else if (assignment.isSubmitted) RingHabitEmerald else colors.primaryAccent,
                    fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit deadline", tint = colors.textSecondary, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete deadline", tint = colors.textTertiary, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun DeadlineEditorDialog(
    initialAssignment: AssignmentItem?,
    onDismiss: () -> Unit,
    onSave: (code: String, title: String, desc: String, category: String, daysAhead: Int, weight: Int) -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    var code by remember { mutableStateOf(initialAssignment?.courseCode ?: "CSE S1") }
    var title by remember { mutableStateOf(initialAssignment?.title ?: "") }
    var desc by remember { mutableStateOf(initialAssignment?.description ?: "") }
    var category by remember { mutableStateOf(initialAssignment?.category ?: "Project") }
    var daysAhead by remember {
        val days = initialAssignment?.let {
            ((it.dueDateMillis - System.currentTimeMillis()) / (24 * 3600 * 1000L)).toInt().coerceAtLeast(1)
        } ?: 7
        mutableStateOf(days.toString())
    }
    var weight by remember { mutableStateOf((initialAssignment?.weightPercentage ?: 15).toString()) }

    val categories = listOf("Project", "Assignment", "Exam", "Lab")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAssignment == null) "New Project Deadline" else "Edit Deadline",
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Pills
                Text("Category", fontSize = 12.sp, color = lifeColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val isSel = cat.equals(category, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) lifeColors.primaryAccent else lifeColors.surfaceGlassHighlight)
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else lifeColors.textPrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Course") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight %") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = daysAhead,
                    onValueChange = { daysAhead = it },
                    label = { Text("Due (days from now)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            code.trim().uppercase(),
                            title.trim(),
                            desc.trim(),
                            category,
                            daysAhead.toIntOrNull() ?: 5,
                            weight.toIntOrNull() ?: 15
                        )
                    }
                }
            ) {
                Text(if (initialAssignment == null) "Create Deadline" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
