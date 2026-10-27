package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.LifeOsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CourseAttendanceControl(entries: List<TimetableEntry>, viewModel: LifeOsViewModel) {
    val colors = LocalLifeOsColors.current
    if (entries.isEmpty()) {
        Text("Add a timetable class to mark a dated period.", color = colors.textSecondary, fontSize = 12.sp)
        return
    }
    val now = System.currentTimeMillis()
    val ordered = entries.sortedByDescending { latestAttendanceDate(it, now) + (it.startHour * 60 + it.startMinute) * 60_000L }
    val suggested = ordered.firstOrNull { latestAttendanceDate(it, now) + (it.startHour * 60 + it.startMinute) * 60_000L <= now } ?: ordered.first()
    var selected by rememberSaveable(entries.first().courseCode) { mutableStateOf(suggested.id) }
    val entry = entries.firstOrNull { it.id == selected } ?: ordered.first()
    var picking by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { picking = true }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Choose class · ${classTimeLabel(entry)}", fontSize = 12.sp)
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
            ordered.forEach { option ->
                DropdownMenuItem(text = { Text(classTimeLabel(option)) }, onClick = { selected = option.id; picking = false })
            }
        }
    }
    AttendanceClassControl(entry, viewModel)
}

private fun classTimeLabel(entry: TimetableEntry): String {
    val day = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(latestAttendanceDate(entry)))
    return "$day · %02d:%02d–%02d:%02d".format(entry.startHour, entry.startMinute, entry.endHour, entry.endMinute)
}

@Composable
fun AttendanceHistory(courseCode: String, viewModel: LifeOsViewModel) {
    val colors = LocalLifeOsColors.current
    val events by viewModel.attendanceEvents.collectAsStateWithLifecycle()
    val marks = events.filter { it.courseCode == courseCode }
    var historyOpen by rememberSaveable(courseCode) { mutableStateOf(false) }
    if (marks.isEmpty()) { return }
    TextButton(onClick = { historyOpen = true }) {
        Icon(Icons.Outlined.History, null, Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Class history · ${marks.size}", fontSize = 12.sp)
    }
    if (historyOpen) AlertDialog(onDismissRequest = { historyOpen = false }, title = { Text("$courseCode · class history") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your dated marks since this update. Older saved totals do not identify individual classes.", fontSize = 12.sp, color = colors.textSecondary)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(marks, key = { it.key }) { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(event.startMillis)),
                            color = colors.textPrimary, fontSize = 13.sp)
                        val label = if (event.status == "UNMARKED") "Mark removed" else AttendanceChoice.entries.firstOrNull { it.name == event.status }?.label ?: event.status
                        Text(label + if (event.includedInBaseline) " · included in corrected totals" else "",
                            color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { historyOpen = false }) { Text("Done") } })
}

/** Both entry points render the same stored mark, never optimistic counter increments. */
@Composable
fun AttendanceClassControl(entry: TimetableEntry, viewModel: LifeOsViewModel) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val events by viewModel.attendanceEvents.collectAsStateWithLifecycle()
    val records by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val action by viewModel.attendanceAction.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    var dateMillis by rememberSaveable(entry.courseCode, entry.dayOfWeek, entry.startHour, entry.startMinute, entry.endHour, entry.endMinute) {
        mutableLongStateOf(latestAttendanceDate(entry))
    }
    val occurrence = runCatching { attendanceOccurrence(entry, dateMillis) }.getOrNull()
    if (occurrence == null) {
        Text("Correct this class's saved time before marking attendance.", color = colors.textSecondary, fontSize = 12.sp)
        return
    }
    val mark = events.firstOrNull { it.key == occurrence.key }
    val record = records.firstOrNull { it.courseCode == entry.courseCode }
    val covered = mark?.includedInBaseline == true || (record != null && record.baselineAt > 0 && occurrence.startMillis <= record.baselineAt)
    val hasBaseline = record != null && (record.totalClasses > 0 || record.baselineAt > 0)
    val started = occurrence.startMillis <= now
    var changing by remember(occurrence.key, mark?.status) { mutableStateOf(false) }
    var confirmedHere by remember(occurrence.key) { mutableLongStateOf(action.confirmationId) }
    val pulse = remember(occurrence.key) { Animatable(1f) }
    LaunchedEffect(action.busy, action.confirmation, action.confirmationId, reduced) {
        if (reduced) pulse.snapTo(1f)
        if (action.key == occurrence.key && !action.busy && action.confirmation.isNotBlank()) changing = false
        if (action.key == occurrence.key && !action.busy && action.confirmation.isNotBlank() && action.confirmationId > confirmedHere) {
            confirmedHere = action.confirmationId
            if (!reduced) {
                pulse.snapTo(0.91f)
                pulse.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 500f))
            }
        }
    }
    val saved = mark != null && mark.status != AttendanceChoice.UNMARKED.name
    val busyHere = action.busy && action.key == occurrence.key
    val canMark = !action.busy && hasBaseline && started && !covered
    val dateLabel = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
    Column(Modifier.fillMaxWidth().then(if (reduced) Modifier else Modifier.animateContentSize(tween(220))),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !action.busy, onClick = {
                dateMillis = Calendar.getInstance().apply { timeInMillis = dateMillis; add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous week's class") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dateLabel, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("%02d:%02d–%02d:%02d · saved timetable".format(entry.startHour, entry.startMinute, entry.endHour, entry.endMinute),
                    color = colors.textSecondary, fontSize = 11.sp)
            }
            val next = Calendar.getInstance().apply { timeInMillis = dateMillis; add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
            IconButton(enabled = !action.busy && next <= latestAttendanceDate(entry, now), onClick = { dateMillis = next }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next week's class")
            }
        }
        if (saved && !changing) {
            Row(Modifier.fillMaxWidth().border(1.dp, colors.primaryAccent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                .background(colors.primaryAccent.copy(alpha = 0.08f), RoundedCornerShape(20.dp)).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                    .background(colors.primaryAccent.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = colors.primaryAccent)
                }
                Text("Saved · ${AttendanceChoice.entries.firstOrNull { it.name == mark?.status }?.label ?: mark?.status}",
                    Modifier.weight(1f).padding(horizontal = 10.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (!covered) TextButton(enabled = !action.busy, onClick = { changing = true }) { Text("Change", fontSize = 12.sp) }
            }
        } else if (!covered) {
            Row(Modifier.fillMaxWidth().background(colors.surfaceGlassHighlight.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .border(1.dp, colors.borderGlass, RoundedCornerShape(22.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AttendanceChoiceButton("Present", true, canMark, Modifier.weight(1f)) {
                    viewModel.markAttendance(occurrence, AttendanceChoice.PRESENT, mark?.status)
                }
                AttendanceChoiceButton("Absent", false, canMark, Modifier.weight(1f)) {
                    viewModel.markAttendance(occurrence, AttendanceChoice.ABSENT, mark?.status)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = canMark, onClick = { viewModel.markAttendance(occurrence, AttendanceChoice.CANCELLED, mark?.status) }) {
                    Text("Class cancelled", fontSize = 12.sp)
                }
                if (changing) TextButton(enabled = canMark, onClick = { viewModel.markAttendance(occurrence, AttendanceChoice.UNMARKED, mark?.status) }) {
                    Text("Remove mark", fontSize = 12.sp)
                }
            }
            if (changing) TextButton(onClick = { changing = false }) { Text("Keep saved mark", fontSize = 12.sp) }
        }
        val message = when {
            busyHere -> "Saving this class…"
            action.error != null && (action.key == occurrence.key || action.key == entry.courseCode) -> action.error
            !hasBaseline -> "Add your real totals with Correct totals in College first."
            covered -> "Included in your corrected totals · historical mark locked."
            !started -> "This class has not started yet."
            action.key == occurrence.key && action.confirmation.isNotBlank() -> action.confirmation
            saved -> "One saved mark for this dated period."
            else -> "Mark this dated period once. You can correct it afterward."
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (busyHere) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.primaryAccent)
            Text(message.orEmpty(), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun AttendanceChoiceButton(label: String, present: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f,
        if (reduced) snap() else spring(dampingRatio = 0.6f, stiffness = 700f), label = "attendance_press")
    Button(onClick = { playAppleHaptic(context); onClick() }, enabled = enabled, interactionSource = interaction,
        modifier = modifier.heightIn(min = 52.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(17.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (present) colors.primaryAccent else colors.surfaceGlass,
            contentColor = if (present) colors.background else colors.textPrimary)) {
        Icon(if (present) Icons.Outlined.Check else Icons.Outlined.Remove, null, Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
