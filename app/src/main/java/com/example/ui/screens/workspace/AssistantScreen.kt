package com.example.ui.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.assistant.AssistantDay
import com.example.data.assistant.AssistantProvider
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Context is bounded and labelled; missing records are not filled with guesses. */
private fun assistantContext(input: DayInputs, habits: List<HabitItem>, attendance: List<AttendanceRecord>,
    water: Int, waterLogged: Boolean, weights: List<BodyWeightLog>, pack: AcademicPack?, question: String): String {
    fun at(time: Long) = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()).format(Date(time))
    val terms = question.lowercase(Locale.ROOT).split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }.toSet()
    val related = pack?.items.orEmpty().filter { item -> terms.any { term -> item.title.contains(term, true) || item.detail.contains(term, true) } }.take(5)
    return buildString {
        appendLine("Current local time: ${at(input.now)}. Student: first-year B.Tech CSE. Do not treat suggestions as completed work.")
        appendLine("Today's saved classes and fixed commitments: " + DayGuide.anchors(input).joinToString("; ") { "${it.title} ${at(it.start)}–${at(it.end)}" }.ifBlank { "none recorded" })
        appendLine("Planned work: " + input.blocks.filter { it.state == "PLANNED" }.sortedBy { it.startMillis }.take(10).joinToString("; ") { "${it.title}, ${at(it.startMillis)}, ${it.durationMinutes} minutes" }.ifBlank { "none recorded" })
        appendLine("Pending tasks: " + input.tasks.filter { it.status == TaskStatus.PENDING }.take(12).joinToString("; ") { it.title }.ifBlank { "none recorded" })
        appendLine("Open deadlines: " + input.assignments.filter { !it.isSubmitted }.sortedBy { it.dueDateMillis }.take(8).joinToString("; ") { "${it.title}, ${at(it.dueDateMillis)}" }.ifBlank { "none recorded" })
        appendLine("Active projects: " + input.projects.filterNot { it.archived }.take(8).joinToString("; ") { it.title }.ifBlank { "none recorded" })
        appendLine("Coding attempts: " + input.attempts.filterNot { it.id.startsWith("aisat26:") }.take(8).joinToString("; ") { "${it.problem}: ${it.outcome}, lesson: ${it.notes.take(200)}" }.ifBlank { "none recorded" })
        appendLine("Recently saved sessions: " + input.history.sortedByDescending { it.endedAt }.take(5).joinToString("; ") { "${it.title}, ${it.elapsedSeconds / 60} minutes, note: ${it.resumeNote.take(160)}" }.ifBlank { "none recorded" })
        appendLine("Habit records: " + habits.take(8).joinToString("; ") { "${it.title}: ${if (it.isCompletedToday) "marked today" else "not marked today"}" }.ifBlank { "none recorded" })
        appendLine("Attendance aggregates, provenance may be incomplete: " + attendance.take(8).joinToString("; ") { "${it.courseCode} ${it.attendedClasses}/${it.totalClasses}" }.ifBlank { "none recorded" })
        appendLine("Water today: ${if (waterLogged) "$water ml recorded" else "no record"}. Latest weight: " + (weights.maxByOrNull { it.timestamp }?.let { "${it.weightKg} kg on ${at(it.timestamp)}" } ?: "none recorded"))
        appendLine("Available syllabus courses: " + pack?.courses.orEmpty().joinToString("; ") { "${it.short} (${it.code})" }.ifBlank { "not loaded" })
        appendLine("Next college exam dates, not subject papers: " + (pack?.nextExam(AcademicDates.key(input.now))?.let { "${it.title}: ${it.dates.joinToString()}" } ?: "not available"))
        appendLine("Relevant supplied syllabus entries: " + related.joinToString("; ") { "${it.title}: ${it.detail.take(250)} [source ${it.source}, page ${it.page}]" }.ifBlank { "none matched; ask for a topic if needed" })
        appendLine("This is a bounded snapshot, not the user's entire life or proof of attendance, progress or completion.")
    }.take(12000)
}

@Composable
fun AssistantScreen(life: LifeOsViewModel, model: WorkspaceViewModel, focus: TodayPrototypeViewModel, assistant: AssistantViewModel, now: Long) {
    val data by assistant.data.collectAsStateWithLifecycle()
    val busy by assistant.busy.collectAsStateWithLifecycle()
    val error by assistant.error.collectAsStateWithLifecycle()
    val using by assistant.using.collectAsStateWithLifecycle()
    val habits by life.allHabits.collectAsStateWithLifecycle()
    val attendance by life.attendanceRecords.collectAsStateWithLifecycle()
    val water by life.totalWaterTodayMl.collectAsStateWithLifecycle()
    val waterLogs by life.waterLogs.collectAsStateWithLifecycle()
    val weights by life.weightLogs.collectAsStateWithLifecycle()
    val pack by model.academicPack.collectAsStateWithLifecycle()
    val input = dayInputs(life, model, focus, now)
    val colors = LocalLifeOsColors.current
    var text by rememberSaveable { mutableStateOf("") }
    var settings by rememberSaveable { mutableStateOf(false) }
    var rehearsal by rememberSaveable { mutableStateOf(false) }
    var clinic by rememberSaveable { mutableStateOf(false) }
    var rescue by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(data.messages.size, busy, error) {
        if (data.messages.isNotEmpty() || busy || error != null) {
            withFrameNanos { }
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.animateScrollToItem(last)
        }
    }
    fun ask(question: String, dayBrief: Boolean = false): Boolean = if (dayBrief || AssistantDay.isNextQuestion(question))
        assistant.replyFromSavedDay(question, AssistantDay.snapshot(input))
    else assistant.send(question, assistantContext(input, habits, attendance, water,
        waterLogs.any { it.timestamp in DayGuide.startOfDay(now) until DayGuide.endOfDay(now) }, weights, pack, question))
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("YOUR CONTEXT, READY", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Text("What can we move forward?", color = colors.textPrimary, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
                        Text("I know the app and use your saved context when you ask.", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    WorkspaceIconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Tune, "Assistant settings") }
                }
            }
            if (data.keys.isEmpty()) item {
                WorkspaceActionRow("Connect your first AI", "Add a Groq, Cerebras, OpenRouter or NVIDIA key", Icons.Outlined.Key) { settings = true }
            }
            item {
                Text("A LITTLE MORE POWER", color = colors.textTertiary, fontSize = 10.sp, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                WorkspaceActionRow("The briefing", "A useful read on your real day", Icons.Outlined.WbSunny) {
                    if (!busy) ask("What's next in my day?", dayBrief = true)
                }
                Spacer(Modifier.height(8.dp))
                WorkspaceActionRow("Exam rehearsal", "Turn your syllabus into a practice session", Icons.Outlined.School) { rehearsal = true }
                Spacer(Modifier.height(8.dp))
                WorkspaceActionRow("Code clinic", "Get unstuck using a saved problem", Icons.Outlined.Code) { clinic = true }
                Spacer(Modifier.height(8.dp))
                WorkspaceActionRow("Rescue a day", "Find a smaller opening around your commitments", Icons.Outlined.Schedule) { rescue = true }
            }
            if (data.messages.isNotEmpty()) item { Text("CONVERSATION", color = colors.textTertiary, fontSize = 10.sp, letterSpacing = 1.2.sp) }
            items(data.messages.size) { index ->
                val message = data.messages[index]
                if (message.role == "user") Column(Modifier.fillMaxWidth().background(colors.primaryAccent.copy(alpha = 0.11f), RoundedCornerShape(22.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("YOU", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(message.text, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 21.sp)
                } else if (message.provider == "On device" && index == data.messages.lastIndex) {
                    SavedDayResponse(input, model, focus)
                } else AssistantResponse(message) { title, detail ->
                    model.offerMove(DayMove(title.take(300), firstStep = detail.take(1500), reason = "AI suggestion · review and edit before saving"))
                }
            }
            if (busy) item { AssistantThinking(using ?: "your AI") }
            if (error != null) item { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
        AssistantComposer(text, { text = it.take(4000) }, busy, data.shareContext, data.fallback, data.selected.label) {
            if (ask(text)) { text = ""; true } else false
        }
    }
    if (settings) AssistantSettings(data, busy, assistant) { settings = false }
    if (rehearsal) {
        val choices = pack?.items.orEmpty().filter { it.scope in listOf("THEORY", "CODE", "FOUNDATION") }
        var search by rememberSaveable { mutableStateOf("") }
        AlertDialog(onDismissRequest = { rehearsal = false }, title = { Text("Rehearse from your syllabus") }, text = {
            Column {
                OutlinedTextField(search, { search = it }, label = { Text("Find a topic") }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(choices.filter { it.title.contains(search, true) || it.detail.contains(search, true) }.take(60), key = { it.id }) { item ->
                        WorkspaceActionRow(item.title, "${item.course} · page ${item.page}", Icons.Outlined.MenuBook) {
                            rehearsal = false
                            ask("Quiz me interactively on this exact syllabus topic: ${item.title}. Source details: ${item.detail}. Source ${item.source}, page ${item.page}. Ask one question at a time, do not assume I have studied it, and explain after my answer.")
                        }
                    }
                }
                if (choices.isEmpty()) Text("Your syllabus is still loading or unavailable.")
            }
        }, confirmButton = { WorkspaceTextButton(onClick = { rehearsal = false }) { Text("Close") } })
    }
    if (clinic) AlertDialog(onDismissRequest = { clinic = false }, title = { Text("Choose a saved problem") }, text = {
        val attempts = input.attempts.filterNot { it.id.startsWith("aisat26:") }.take(30)
        if (attempts.isEmpty()) Text("Add a real problem in Coding first; nothing is invented here.")
        else LazyColumn(Modifier.heightIn(max = 360.dp)) { items(attempts, key = { it.id }) { attempt ->
            WorkspaceActionRow(attempt.problem, "${attempt.outcome} · ${attempt.difficulty}", Icons.Outlined.Code) {
                clinic = false
                ask("Help me make progress on my saved coding problem: ${attempt.problem}. My reported outcome: ${attempt.outcome}. My note: ${attempt.notes}. Link: ${attempt.link}. Start with a hint and a small test case; don't claim it is solved.")
            }
        } }
    }, confirmButton = { WorkspaceTextButton(onClick = { clinic = false }) { Text("Close") } })
    if (rescue) AlertDialog(onDismissRequest = { rescue = false }, title = { Text("Find a smaller opening") }, text = {
        val next = DayGuide.choices(input).firstOrNull()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (next == null) "Add a task or choose work in Today first." else "Next saved step: ${next.title}. Pick a starting size; you can edit the exact minutes before saving.")
            if (next != null) listOf(10, 25, 45).forEach { minutes ->
                val slot = DayGuide.window(input, minutes)
                WorkspaceActionRow("$minutes minutes", slot?.let { "Next opening ${clock(it)}" } ?: "No free opening today", Icons.Outlined.Schedule) {
                    if (slot != null) { rescue = false; model.offerMove(next.copy(minutes = minutes, reason = "A smaller restart around your saved commitments")) }
                }
            }
        }
    }, confirmButton = { WorkspaceTextButton(onClick = { rescue = false }) { Text("Close") } })
}

@Composable
private fun AssistantComposer(value: String, onChange: (String) -> Unit, busy: Boolean, shareContext: Boolean,
    fallback: Boolean, provider: String, onSend: () -> Boolean) {
    val colors = LocalLifeOsColors.current
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val canSend = !busy && value.isNotBlank()
    fun send() { if (canSend && onSend()) focus.clearFocus() }
    Column(Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val shape = RoundedCornerShape(25.dp)
        Row(Modifier.fillMaxWidth().clip(shape).background(colors.surfaceGlass)
            .border(1.dp, if (focused) colors.primaryAccent.copy(alpha = 0.65f) else colors.borderGlass, shape)
            .padding(start = 15.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = colors.primaryAccent)
            BasicTextField(value, onChange, Modifier.weight(1f).heightIn(min = 42.dp).onFocusChanged { focused = it.isFocused }.semantics { contentDescription = "Ask Assistant" },
                enabled = !busy, maxLines = 4, textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary, lineHeight = 20.sp),
                cursorBrush = SolidColor(colors.primaryAccent), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }), decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().heightIn(min = 42.dp), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text("Ask about your day, study or code", color = colors.textTertiary, fontSize = 13.sp)
                        inner()
                    }
                })
            IconButton(onClick = { send() }, enabled = canSend,
                modifier = Modifier.size(44.dp).background(if (canSend) colors.primaryAccent else colors.primaryAccent.copy(alpha = 0.10f), CircleShape)) {
                Icon(Icons.Outlined.ArrowUpward, "Send message", Modifier.size(20.dp), tint = if (canSend) colors.background else colors.textTertiary)
            }
        }
        Text(if (shareContext) "Next-step answers stay local · AI shares context with $provider${if (fallback) " / fallback" else ""}."
            else "Next-step answers stay local · AI uses $provider${if (fallback) " / fallback" else ""}; context is off.",
            color = colors.textTertiary, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
    }
}

@Composable
private fun AssistantSettings(data: com.example.data.assistant.AssistantData, busy: Boolean, assistant: AssistantViewModel, onClose: () -> Unit) {
    var provider by rememberSaveable { mutableStateOf(data.selected) }
    var key by rememberSaveable(provider) { mutableStateOf("") }
    var memory by rememberSaveable { mutableStateOf(data.memory) }
    val handler = LocalUriHandler.current
    AlertDialog(onDismissRequest = onClose, title = { Text("Your assistant") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Keys, chat and personal memory are encrypted on this phone. When you send, the selected provider receives your message and recent conversation. If context is on, it also receives a bounded snapshot of saved app data and your memory.", fontSize = 12.sp, lineHeight = 18.sp)
            Text("OpenRouter's free router may send the request to another model provider. Fallback may send the same information to each configured provider. Free limits and data policies vary; Groq, Cerebras or NVIDIA can charge if your account has paid billing enabled.", fontSize = 12.sp, lineHeight = 18.sp)
            AssistantProvider.entries.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = provider == p, onClick = { provider = p; assistant.select(p) }, enabled = !busy)
                    Text("${p.label} · ${if (p in data.keys) "key saved" else "not connected"}")
                }
            }
            OutlinedTextField(key, { key = it }, label = { Text("${provider.label} API key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), enabled = !busy, modifier = Modifier.fillMaxWidth())
            Row {
                WorkspaceButton(onClick = { if (key.isNotBlank() && assistant.setKey(provider, key)) key = "" }, enabled = !busy && key.isNotBlank()) { Text("Save key") }
                WorkspaceTextButton(onClick = { assistant.setKey(provider, "") }, enabled = !busy && provider in data.keys) { Text("Remove") }
            }
            WorkspaceTextButton(onClick = { runCatching { handler.openUri(provider.signup) } }) { Text("Get a ${provider.label} key ↗") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use other configured providers when needed", Modifier.weight(1f), fontSize = 12.sp)
                Switch(data.fallback, onCheckedChange = assistant::setFallback, enabled = !busy)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Share saved app context and personal memory", Modifier.weight(1f), fontSize = 12.sp)
                Switch(data.shareContext, onCheckedChange = assistant::setShareContext, enabled = !busy)
            }
            OutlinedTextField(memory, { memory = it.take(4000) }, label = { Text("Things to remember about me") },
                placeholder = { Text("Preferences, goals or context you want carried into conversations") }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !busy)
            WorkspaceTextButton(onClick = { assistant.setMemory(memory) }, enabled = !busy) { Text("Save personal memory") }
            WorkspaceTextButton(onClick = assistant::clearChat, enabled = !busy && data.messages.isNotEmpty()) { Text("Clear conversation") }
        }
    }, confirmButton = { WorkspaceTextButton(onClick = onClose) { Text("Done") } })
}
