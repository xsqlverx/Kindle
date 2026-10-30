package com.example.ui.screens.settings

import com.example.ui.components.WorkspaceButton
import com.example.ui.components.WorkspaceOutlinedButton
import com.example.ui.components.WorkspaceTextButton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.WorkspaceLabel
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.playAppleHaptic
import com.example.ui.theme.LocalLifeOsColors
import com.example.ui.theme.RingGymRose
import com.example.ui.theme.RingHabitEmerald
import com.example.viewmodel.LifeOsViewModel
import com.example.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: LifeOsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    workspace: WorkspaceViewModel? = null,
    onPreview: () -> Unit = {},
    onWipeWorkspace: () -> Unit = {},
    canWipeWorkspace: Boolean = true
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val targetExam by viewModel.targetExam.collectAsStateWithLifecycle()
    var showExamDateDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showResetAttendanceDialog by remember { mutableStateOf(false) }
    var showTimetableImportDialog by remember { mutableStateOf(false) }
    var showWorkoutImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = lifeColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(lifeColors.surfaceGlass)
                            .border(1.dp, lifeColors.borderGlass, CircleShape)
                            .clickable {
                                playAppleHaptic(context)
                                onBack()
                            }
                            .testTag("settings_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = lifeColors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CONFIGURATION & DATA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = lifeColors.primaryAccent
                        )
                        Text(
                            text = "Make it yours",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = lifeColors.textPrimary
                        )
                    }
                }
            }

            if (workspace != null) item { NavigationEditor(workspace) }
            if (workspace != null) item {
                SettingsActionCard(Icons.Default.PlayCircleOutline, "Try the daily loop · 60 sec", "Rehearse focus, the capsule and saving your place. Your real plan and study totals stay unchanged.", "Try it", {
                    workspace.offerMove(com.example.data.model.DayMove("", "PREVIEW", reason = "A safe daily-loop preview", minutes = 1))
                })
            }
            item {
                SettingsActionCard(Icons.Default.Palette, "V1 design preview", "The three sample moments, separate from your live day", "Preview", onPreview)
            }

            // Section 1: Timetable & Academic Data
            item {
                Text(
                    text = "ACADEMIC GOALS & SCHEDULE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                val examDays = targetExam.getDaysLeft()
                val examDesc = if (targetExam.dateMillis > 0L) {
                    val daysText = if (examDays != null) {
                        if (examDays >= 0) "$examDays days left" else "Exam passed"
                    } else ""
                    "${targetExam.title.ifBlank { "Upcoming Exam" }} • ${targetExam.getFormattedDate()}${if (daysText.isNotBlank()) " ($daysText)" else ""}"
                } else {
                    "No exam date set. Tap to configure your target exam title and countdown date."
                }

                SettingsActionCard(
                    icon = Icons.Default.Event,
                    title = "Target Exam Date",
                    description = examDesc,
                    actionLabel = if (targetExam.dateMillis > 0L) "Edit Date" else "Set Date",
                    onClick = { showExamDateDialog = true },
                    testTag = "set_exam_date_button"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.FileUpload,
                    title = "Import Timetable JSON",
                    description = "Paste your weekly class schedule JSON to overwrite timetable entries",
                    actionLabel = "Import",
                    onClick = { showTimetableImportDialog = true },
                    testTag = "import_timetable_button"
                )
            }

            // Section 2: Workout & Fitness
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FITNESS & ROUTINES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.FitnessCenter,
                    title = "Import Workout Routine JSON",
                    description = "Paste PPL exercise sets and weights JSON to configure routines",
                    actionLabel = "Import",
                    onClick = { showWorkoutImportDialog = true },
                    testTag = "import_workout_button"
                )
            }

            // Section 3: Data Management & Resets
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DATA MANAGEMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.School,
                    title = "Reset Attendance Logs (0 of 0)",
                    description = "Resets attendance records for all courses to 0 lectures attended out of 0 total lectures, keeping your class schedule intact",
                    actionLabel = "Reset Attendance",
                    isDestructive = true,
                    onClick = { showResetAttendanceDialog = true },
                    testTag = "reset_attendance_button"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.RestartAlt,
                    title = "Clear Tracked Records",
                    description = "Clears tracked records and V2 workspace data. Timetable, targets, Daily Reset records and preferences remain.",
                    actionLabel = "Clear Records",
                    isDestructive = true,
                    onClick = { showResetConfirmDialog = true },
                    testTag = "reset_all_data_button"
                )
            }

            // Section 4: System Diagnostics
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ABOUT & DIAGNOSTICS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                LiquidGlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        WorkspaceLabel("ABOUT YOUR WORKSPACE")
                        SettingsInfo("App version", BuildConfig.VERSION_NAME)
                        SettingsInfo("Storage", "Saved on this device")
                        SettingsInfo("Attendance", "Targets are editable per course in College")
                        SettingsInfo("Next milestone", if (targetExam.dateMillis > 0L)
                            "${targetExam.title.ifBlank { "Exam" }} · ${targetExam.getFormattedDate()}" else "No exam date set")
                    }
                }
            }
        }
    }

    // Dialog: Wipe All App Data Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RingGymRose,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Tracked Records?",
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This permanently clears tasks, habits, assignments, exercise sets, weight, water and calorie logs, plan blocks, sessions, projects, captures, exam topics, coding attempts, study-roadmap progress and unfinished add/log drafts. Attendance returns to 0/0. The timetable, targets, Daily Reset records, appearance and navigation settings remain. Suggested projects will not be added again automatically.",
                    fontSize = 13.sp,
                    color = lifeColors.textSecondary
                )
            },
            confirmButton = {
                WorkspaceButton(
                    onClick = {
                        playAppleHaptic(context, strong = true)
                        viewModel.resetAllDataToDefault()
                        onWipeWorkspace()
                        showResetConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Record clearing requested")
                        }
                    },
                    enabled = canWipeWorkspace,
                    colors = ButtonDefaults.buttonColors(containerColor = RingGymRose)
                ) {
                    Text("Clear These Records")
                }
            },
            dismissButton = {
                WorkspaceTextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = lifeColors.textSecondary)
                }
            }
        )
    }

    // Dialog: Reset Attendance Confirmation
    if (showResetAttendanceDialog) {
        AlertDialog(
            onDismissRequest = { showResetAttendanceDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RingGymRose,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Reset All Attendance Logs?",
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will clear all lecture counts back to 0 of 0 attended for all timetable courses. Your timetable schedule will remain untouched.",
                    fontSize = 13.sp,
                    color = lifeColors.textSecondary
                )
            },
            confirmButton = {
                WorkspaceButton(
                    onClick = {
                        playAppleHaptic(context, strong = true)
                        viewModel.resetAttendanceData()
                        showResetAttendanceDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("All attendance logs reset to 0/0.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RingGymRose)
                ) {
                    Text("Yes, Reset Attendance")
                }
            },
            dismissButton = {
                WorkspaceTextButton(onClick = { showResetAttendanceDialog = false }) {
                    Text("Cancel", color = lifeColors.textSecondary)
                }
            }
        )
    }

    // Dialog: Import Timetable JSON
    if (showTimetableImportDialog) {
        val sampleJson = """
[
  {
    "courseCode": "CSE S1",
    "courseName": "Data Structures",
    "dayOfWeek": 2,
    "startHour": 9,
    "startMinute": 0,
    "endHour": 10,
    "endMinute": 0,
    "room": "A213",
    "professor": "Dr. Miller"
  }
]
        """.trimIndent()
        var jsonText by remember { mutableStateOf(sampleJson) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showTimetableImportDialog = false },
            title = {
                Text(
                    text = "Import Timetable JSON",
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a JSON array of classes (dayOfWeek: 2=Mon, 3=Tue, etc.):",
                        fontSize = 12.sp,
                        color = lifeColors.textSecondary
                    )
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = {
                            jsonText = it
                            errorMsg = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        isError = errorMsg != null
                    )
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg ?: "",
                            fontSize = 11.sp,
                            color = RingGymRose,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                WorkspaceButton(
                    onClick = {
                        val (success, msg) = viewModel.importTimetableJson(jsonText)
                        if (success) {
                            playAppleHaptic(context)
                            showTimetableImportDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        } else {
                            errorMsg = msg
                        }
                    }
                ) {
                    Text("Apply Timetable")
                }
            },
            dismissButton = {
                WorkspaceTextButton(onClick = { showTimetableImportDialog = false }) {
                    Text("Cancel", color = lifeColors.textSecondary)
                }
            }
        )
    }

    // Dialog: Import Workout JSON
    if (showWorkoutImportDialog) {
        val sampleJson = """
[
  {
    "exerciseName": "Flat Dumbbell Press",
    "splitCategory": "Push",
    "setNumber": 1,
    "reps": 10,
    "weightKg": 24.0
  },
  {
    "exerciseName": "Lat Pulldowns",
    "splitCategory": "Pull",
    "setNumber": 1,
    "reps": 12,
    "weightKg": 55.0
  }
]
        """.trimIndent()
        var jsonText by remember { mutableStateOf(sampleJson) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showWorkoutImportDialog = false },
            title = {
                Text(
                    text = "Import Workout Routine JSON",
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a JSON array of exercise sets (splitCategory: Push, Pull, Legs):",
                        fontSize = 12.sp,
                        color = lifeColors.textSecondary
                    )
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = {
                            jsonText = it
                            errorMsg = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        isError = errorMsg != null
                    )
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg ?: "",
                            fontSize = 11.sp,
                            color = RingGymRose,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                WorkspaceButton(
                    onClick = {
                        val (success, msg) = viewModel.importWorkoutJson(jsonText)
                        if (success) {
                            playAppleHaptic(context)
                            showWorkoutImportDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        } else {
                            errorMsg = msg
                        }
                    }
                ) {
                    Text("Apply Workout Sets")
                }
            },
            dismissButton = {
                WorkspaceTextButton(onClick = { showWorkoutImportDialog = false }) {
                    Text("Cancel", color = lifeColors.textSecondary)
                }
            }
        )
    }

    // Dialog: Configure Target Exam Date & Title
    if (showExamDateDialog) {
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
            onDismissRequest = { showExamDateDialog = false },
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
                        text = "Set your upcoming exam name and target date. This powers the countdown across College Hub and Today view.",
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

                        WorkspaceOutlinedButton(
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
                WorkspaceButton(
                    onClick = {
                        playAppleHaptic(context)
                        viewModel.setTargetExam(examTitle.trim().ifBlank { "Upcoming Exam" }, selectedMillis)
                        showExamDateDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Target exam updated")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lifeColors.primaryAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Target Exam", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (targetExam.dateMillis > 0L) {
                        WorkspaceTextButton(
                            onClick = {
                                playAppleHaptic(context)
                                viewModel.clearTargetExam()
                                showExamDateDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Target exam date cleared")
                                }
                            }
                        ) {
                            Text("Clear", color = RingGymRose)
                        }
                    }
                    WorkspaceTextButton(onClick = { showExamDateDialog = false }) {
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
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    testTag: String = ""
) {
    val lifeColors = LocalLifeOsColors.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDestructive) RingGymRose.copy(alpha = 0.15f)
                            else lifeColors.primaryAccent.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestructive) RingGymRose else lifeColors.primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = lifeColors.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDestructive) RingGymRose.copy(alpha = 0.18f)
                        else lifeColors.primaryAccent.copy(alpha = 0.18f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) RingGymRose else lifeColors.primaryAccent
                )
            }
        }
    }
}

@Composable
private fun SettingsInfo(label: String, value: String) {
    val colors = LocalLifeOsColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = colors.textTertiary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 21.sp)
    }
}
