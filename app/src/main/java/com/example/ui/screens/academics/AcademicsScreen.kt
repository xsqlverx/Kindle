package com.example.ui.screens.academics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.example.data.model.AssignmentItem
import com.example.data.model.AttendanceRecord
import com.example.data.model.ExamTarget
import com.example.data.model.TimetableEntry
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
    modifier: Modifier = Modifier
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
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
                            color = if (isSelected) Color.White else lifeColors.textPrimary
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
                        text = "80% threshold requirement",
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
                        text = "Target: 80%",
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
                }
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
                    onDelete = { assignmentToDelete = assignment }
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
    val lifeColors = LocalLifeOsColors.current
    val daysLeft = targetExam.getDaysLeft()

    val totalAttended = attendanceRecords.sumOf { it.attendedClasses }
    val totalClasses = attendanceRecords.sumOf { it.totalClasses }
    val currentAttendancePct = if (totalClasses > 0) (totalAttended * 100 / totalClasses) else 0

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigureExam() }
            .testTag(testTag),
        tint = lifeColors.primaryAccent.copy(alpha = 0.12f),
        borderAlpha = 0.8f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Academic Icon, Exam Title & Countdown Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(lifeColors.primaryAccent.copy(alpha = 0.2f))
                            .border(1.dp, lifeColors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Academic Target",
                            tint = lifeColors.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (targetExam.dateMillis > 0L) targetExam.title.ifBlank { "Upcoming Exam" } else "No Exam Date Configured",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = lifeColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (targetExam.dateMillis > 0L) targetExam.getFormattedDate() else "Tap here or visit Settings to set date",
                            fontSize = 12.sp,
                            color = lifeColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Days Countdown Pill
                if (targetExam.dateMillis > 0L && daysLeft != null) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (daysLeft >= 0) "$daysLeft" else "0",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = lifeColors.primaryAccent
                        )
                        Text(
                            text = if (daysLeft == 1) "DAY LEFT" else if (daysLeft < 0) "PASSED" else "DAYS LEFT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = lifeColors.textSecondary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(lifeColors.surfaceGlass)
                            .border(1.dp, lifeColors.borderGlass, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Set Date",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.primaryAccent
                        )
                    }
                }
            }

            // Divider separating exam info from the attendance threshold bar
            HorizontalDivider(
                color = lifeColors.borderGlass.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            // Row 2: Fixed 80% Attendance Target Badge (Properly Aligned and Never Truncated)
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(RingHabitEmerald.copy(alpha = 0.18f))
                            .border(1.dp, RingHabitEmerald.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "80% Attendance Target",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RingHabitEmerald,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = "College Policy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = lifeColors.textSecondary
                    )
                }

                Text(
                    text = if (totalClasses > 0) "Current: $currentAttendancePct% ($totalAttended/$totalClasses)" else "0 / 0 Lectures Logged",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalClasses > 0 && currentAttendancePct >= 80) RingHabitEmerald else if (totalClasses > 0) RingGymRose else lifeColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ActiveLectureCard(
    lectureStatus: com.example.viewmodel.CurrentLectureStatus,
    onClick: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("active_lecture_tile"),
        tint = if (lectureStatus.isOngoing) RingWaterCyan.copy(alpha = 0.12f) else lifeColors.surfaceGlass
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Guaranteed non-wrapping Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (lectureStatus.isOngoing) RingHabitEmerald.copy(alpha = 0.18f)
                            else lifeColors.primaryAccent.copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (lectureStatus.isOngoing) RingHabitEmerald else lifeColors.primaryAccent)
                    )
                    Text(
                        text = if (lectureStatus.isOngoing) "ONGOING LECTURE" else "UPCOMING LECTURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = if (lectureStatus.isOngoing) RingHabitEmerald else lifeColors.primaryAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Subtitle / Time details (e.g. "Starts in 25 min", "No further classes")
                Text(
                    text = lectureStatus.timeRemainingOrUntil,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = lifeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            // Main lecture name or summary
            Text(
                text = lectureStatus.entry?.courseName ?: lectureStatus.message.ifBlank { "No immediate class" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TimetableGlassCard(
    entry: TimetableEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val timeFormatted = "${String.format("%02d:%02d", entry.startHour, entry.startMinute)} - ${String.format("%02d:%02d", entry.endHour, entry.endMinute)}"

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("timetable_card_${entry.id}")
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
                // Time Pillar
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(lifeColors.surfaceGlassHighlight)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.primaryAccent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.courseCode,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = lifeColors.textPrimary
                        )
                        Text(
                            text = "• ${entry.room}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = lifeColors.primaryAccent
                        )
                    }
                    Text(
                        text = entry.courseName,
                        fontSize = 12.sp,
                        color = lifeColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.professor.isNotEmpty()) {
                        Text(
                            text = entry.professor,
                            fontSize = 11.sp,
                            color = lifeColors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Class",
                    tint = lifeColors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AttendanceGlassRow(
    record: AttendanceRecord,
    onLogAttendance: (Boolean) -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val total = record.totalClasses
    val attended = record.attendedClasses

    val percentage = if (total > 0) {
        (attended.toFloat() / total.toFloat() * 100f)
    } else 0f

    val isSafe = if (total == 0) true else percentage >= record.targetPercentage

    // Calculate buffer
    val bufferStatusText = remember(attended, total, record.targetPercentage) {
        if (total == 0) {
            "No classes logged yet"
        } else if (isSafe) {
            // Can miss how many?
            // (attended) / (total + x) >= 0.8 => total + x <= attended / 0.8 => x <= attended/0.8 - total
            val canMiss = floor((attended / 0.80f) - total).toInt()
            if (canMiss > 0) "Safe: Can miss $canMiss class${if (canMiss > 1) "es" else ""}"
            else "At threshold: Cannot miss next class"
        } else {
            // Need consecutive attendances
            // (attended + x) / (total + x) >= 0.8 => attended + x >= 0.8 total + 0.8 x => 0.2 x >= 0.8 total - attended
            // x >= (4 * total - 5 * attended)
            val needed = ceil(4.0 * total - 5.0 * attended).toInt().coerceAtLeast(1)
            "Warning: Need $needed class${if (needed > 1) "es" else ""} for 80%"
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (total == 0) 0f else (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "attendanceProgress"
    )

    val progressColor by animateColorAsState(
        targetValue = if (total == 0) lifeColors.textSecondary.copy(alpha = 0.3f) else if (isSafe) RingHabitEmerald else RingGymRose,
        label = "attendanceColor"
    )

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = record.courseCode,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.textPrimary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (total == 0) lifeColors.borderGlass.copy(alpha = 0.4f)
                                    else if (isSafe) RingHabitEmerald.copy(alpha = 0.15f)
                                    else RingGymRose.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (total == 0) lifeColors.borderGlass
                                    else if (isSafe) RingHabitEmerald.copy(alpha = 0.3f)
                                    else RingGymRose.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (total == 0) "0%" else "${percentage.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (total == 0) lifeColors.textSecondary else if (isSafe) RingHabitEmerald else RingGymRose
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (total == 0) "0 of 0 lectures attended • No classes logged yet" else "$attended of $total lectures attended • $bufferStatusText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (total == 0) lifeColors.textSecondary else if (isSafe) lifeColors.textSecondary else RingGymRose,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onLogAttendance(true) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = RingHabitEmerald.copy(alpha = 0.2f),
                            contentColor = RingHabitEmerald
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Present", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { onLogAttendance(false) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RingGymRose.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = RingGymRose, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Absent", fontSize = 11.sp, color = RingGymRose, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            // Animated Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(lifeColors.surfaceGlassHighlight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }
        }
    }
}

@Composable
private fun AssignmentGlassCard(
    assignment: AssignmentItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    val diffMillis = assignment.dueDateMillis - System.currentTimeMillis()
    val daysRemaining = (diffMillis / (24 * 3600 * 1000L)).toInt()

    val isOverdue = daysRemaining < 0 && !assignment.isSubmitted

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Category, Course Code, Dedicated Grade Weight Badge, and Due Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(lifeColors.primaryAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = assignment.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.primaryAccent,
                            maxLines = 1
                        )
                    }

                    // Course Code Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(lifeColors.surfaceGlassHighlight)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = assignment.courseCode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.textSecondary,
                            maxLines = 1
                        )
                    }

                    // Grade Weight Chip - High Contrast & Dedicated Pill
                    if (assignment.weightPercentage > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${assignment.weightPercentage}% Grade",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Due Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                assignment.isSubmitted -> RingHabitEmerald.copy(alpha = 0.15f)
                                isOverdue -> RingGymRose.copy(alpha = 0.2f)
                                daysRemaining <= 2 -> RingGymRose.copy(alpha = 0.15f)
                                else -> lifeColors.surfaceGlassHighlight
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            assignment.isSubmitted -> "Turned in"
                            isOverdue -> "Overdue"
                            daysRemaining == 0 -> "Due today"
                            daysRemaining == 1 -> "Due tomorrow"
                            else -> "Due in $daysRemaining d"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            assignment.isSubmitted -> RingHabitEmerald
                            isOverdue || daysRemaining <= 2 -> RingGymRose
                            else -> lifeColors.primaryAccent
                        },
                        maxLines = 1
                    )
                }
            }

            // Main Content Row: Checkbox, Title & Description, and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (assignment.isSubmitted) RingHabitEmerald else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (assignment.isSubmitted) RingHabitEmerald else lifeColors.textTertiary,
                            CircleShape
                        )
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (assignment.isSubmitted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Submitted",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Title and Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (assignment.isSubmitted) lifeColors.textTertiary else lifeColors.textPrimary,
                        textDecoration = if (assignment.isSubmitted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (assignment.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = assignment.description,
                            fontSize = 11.sp,
                            color = lifeColors.textTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = lifeColors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = lifeColors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
