package com.example.ui.screens.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.assistant.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.TodayPrototypeViewModel
import com.example.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.delay

@Composable
fun AssistantResponse(message: AssistantMessage, onPlan: (String, String) -> Unit) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val blocks = remember(message.text) { AssistantAnswer.parse(message.text) }
    var expanded by rememberSaveable(message.text) { mutableStateOf(false) }
    var showOriginal by rememberSaveable(message.text) { mutableStateOf(false) }
    val previewActions = blocks.filter { it.kind == AnswerBlockKind.STEP || it.kind == AnswerBlockKind.TABLE_ROW }.take(2)
    val preview = if (previewActions.isNotEmpty()) listOfNotNull(blocks.firstOrNull { it.kind == AnswerBlockKind.PARAGRAPH }) + previewActions else blocks.take(2)
    val hasMore = preview.size < blocks.size || blocks.any { it.body.length > 220 || it.title.length > 180 || it.kind == AnswerBlockKind.CODE }
    val shown = if (expanded) blocks else preview
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(colors.surfaceGlass, colors.primaryAccent.copy(alpha = 0.065f))))
        .border(1.dp, colors.borderGlass, RoundedCornerShape(26.dp))
        .animateContentSize(if (reduced) snap() else spring(0.86f, 500f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(30.dp).background(colors.primaryAccent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp), tint = colors.primaryAccent)
            }
            Column(Modifier.weight(1f)) {
                Text("ASSISTANT", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.2.sp)
                Text(if (message.provider == "On device") "Saved-day snapshot" else "${message.provider.ifBlank { "AI" }} · suggestions, not saved plans",
                    color = colors.textTertiary, fontSize = 11.sp)
            }
        }
        if (blocks.isEmpty()) SelectionContainer { Text(message.text, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 21.sp) }
        shown.forEachIndexed { index, block ->
            var visible by remember(message.text, index) { mutableStateOf(reduced) }
            LaunchedEffect(message.text, index, reduced) { if (!reduced) delay((index * 45L).coerceAtMost(180L)); visible = true }
            AnimatedVisibility(visible, enter = fadeIn(tween(if (reduced) 0 else 180)) + slideInVertically(tween(if (reduced) 0 else 230)) { it / 7 }) {
                when (block.kind) {
                    AnswerBlockKind.HEADING -> Text(block.title, color = colors.primaryAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    AnswerBlockKind.PARAGRAPH -> SelectionContainer {
                        Text(block.body, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 21.sp,
                            maxLines = if (expanded || !hasMore) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                    }
                    AnswerBlockKind.CODE -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.background.copy(alpha = 0.7f)).padding(12.dp)) {
                        Text(block.label.ifBlank { "CODE" }, color = colors.primaryAccent, fontSize = 10.sp)
                        SelectionContainer { Text(block.body, color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                            lineHeight = 18.sp, maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis) }
                    }
                    AnswerBlockKind.STEP, AnswerBlockKind.TABLE_ROW -> SuggestionTile(block, onPlan)
                }
            }
        }
        if (hasMore) WorkspaceDisclosure(if (expanded) "Show less" else "Explore the answer", expanded, { expanded = !expanded }, Modifier.fillMaxWidth())
        if (message.truncated) Text("This response was cut short. Ask the assistant to continue.", color = colors.textSecondary, fontSize = 12.sp)
        WorkspaceTextButton(onClick = { onPlan("Follow up from Assistant", message.text.take(1500)) }) { Text("Make a next move") }
        if (expanded) {
            WorkspaceTextButton(onClick = { showOriginal = !showOriginal }) { Text(if (showOriginal) "Hide original response" else "View original response") }
            if (showOriginal) SelectionContainer { Text(message.text, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp) }
        }
    }
}

@Composable
fun AssistantThinking(provider: String) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceGlass).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.AutoAwesome, null, tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
        Text("Thinking with $provider", Modifier.weight(1f), color = colors.textSecondary, fontSize = 13.sp)
        if (reduced) repeat(3) { Box(Modifier.size(5.dp).background(colors.primaryAccent, CircleShape)) }
        else {
            val transition = rememberInfiniteTransition(label = "assistant_thinking")
            repeat(3) { index ->
                val lift by transition.animateFloat(0f, -4f,
                    infiniteRepeatable(tween(520, delayMillis = index * 110), RepeatMode.Reverse), label = "thinking_dot_$index")
                Box(Modifier.size(5.dp).graphicsLayer { translationY = lift.dp.toPx() }.background(colors.primaryAccent, CircleShape))
            }
        }
    }
}

@Composable
private fun SuggestionTile(block: AnswerBlock, onPlan: (String, String) -> Unit) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    var expandedTile by rememberSaveable(block.title, block.body, block.label) { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(colors.primaryAccent.copy(alpha = 0.08f))
        .border(1.dp, colors.borderGlass, shape).semantics { stateDescription = if (expandedTile) "Expanded" else "Collapsed" }
        .clickable(onClick = { expandedTile = !expandedTile })
        .animateContentSize(if (reduced) snap() else spring(0.86f, 520f)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(colors.primaryAccent.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (block.kind == AnswerBlockKind.TABLE_ROW) Icons.Outlined.Schedule else Icons.Outlined.AutoAwesome,
                    null, Modifier.size(17.dp), tint = colors.primaryAccent)
            }
            Column(Modifier.weight(1f)) {
                Text(if (block.kind == AnswerBlockKind.TABLE_ROW) "AI SUGGESTION${if (block.label.isNotBlank()) " · ${block.label}" else ""}" else "IDEA",
                    color = colors.textTertiary, fontSize = 9.sp, letterSpacing = 0.8.sp)
                Text(block.title, color = colors.textPrimary, fontSize = 14.sp, lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium, maxLines = if (expandedTile) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, if (expandedTile) "Details open" else "Open details", Modifier.size(18.dp), tint = colors.textSecondary)
        }
        AnimatedVisibility(expandedTile) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (block.body.isNotBlank()) SelectionContainer { Text(block.body, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp) }
                WorkspaceTextButton(onClick = { onPlan(block.title, block.body) }) { Text("Make this a next move") }
            }
        }
    }
}

@Composable
fun SavedDayResponse(input: DayInputs, model: WorkspaceViewModel, focus: TodayPrototypeViewModel) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val day = AssistantDay.snapshot(input)
    val progress = day.current?.let { ((input.now - it.start).toFloat() / (it.end - it.start).coerceAtLeast(1L)).coerceIn(0f, 1f) } ?: 0f
    val animatedProgress by animateFloatAsState(progress, if (reduced) snap() else tween(500), label = "saved_day_progress")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(colors.primaryAccent.copy(alpha = 0.14f), colors.surfaceGlass)))
        .border(1.dp, colors.borderGlass, RoundedCornerShape(26.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(64.dp).semantics { contentDescription = if (day.current == null) "Your saved day" else "Time elapsed in your saved commitment" }, contentAlignment = Alignment.Center) {
                ContinuityOrb(Modifier.size(58.dp).clearAndSetSemantics { }, compact = true)
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(colors.primaryAccent.copy(alpha = 0.12f), style = Stroke(3.dp.toPx()))
                    if (day.current != null) drawArc(colors.primaryAccent, -90f, animatedProgress * 360f, false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Column {
                Text("YOUR DAY · LIVE", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.2.sp)
                Text("From your saved records · ${clock(day.at)}", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        day.session?.let { session ->
            WorkspaceActionRow(if (session.sourceType == "PREVIEW") "Preview session · ${session.title}" else session.title,
                "${session.phase.name.lowercase().replaceFirstChar(Char::uppercase)} · tap to open", Icons.Outlined.Timer, embedded = true) { focus.requestSessionPanel() }
        }
        day.current?.let { anchor ->
            WorkspaceActionRow(anchor.title, "Now · until ${clock(anchor.end)} · ${if (anchor.destination == Destination.COLLEGE) "saved timetable" else "saved commitment"}",
                Icons.Outlined.EventNote, embedded = true) { model.navigate(anchor.destination) }
        }
        day.nextAnchor?.let { anchor ->
            WorkspaceActionRow(anchor.title, "Next commitment · ${clock(anchor.start)}", Icons.Outlined.Event, embedded = true) { model.navigate(anchor.destination) }
        }
        day.nextBlock?.let { block ->
            WorkspaceActionRow(block.title, "Saved work · ${clock(block.startMillis)} · ${block.durationMinutes} min", Icons.Outlined.Schedule, embedded = true) {
                model.offerMove(DayGuide.fromBlock(block))
            }
        }
        day.suggestion?.let { move ->
            Spacer(Modifier.height(2.dp))
            Text("ONE POSSIBLE NEXT STEP", color = colors.textTertiary, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(move.title, color = colors.textPrimary, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
            Text(move.reason, color = colors.textSecondary, fontSize = 12.sp)
            WorkspaceButton(onClick = { model.offerMove(move) }, modifier = Modifier.fillMaxWidth()) { Text("Make room for this") }
        }
        if (day.session == null && day.current == null && day.nextAnchor == null && day.nextBlock == null && day.suggestion == null) {
            Text("Nothing upcoming is saved for today yet. Add one thing and we'll help find room for it.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            WorkspaceButton(onClick = { model.openEntry(EntryKind.TASK) }) { Text("Add an action") }
        }
    }
}
