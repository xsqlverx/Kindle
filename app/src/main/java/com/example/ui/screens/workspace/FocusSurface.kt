package com.example.ui.screens.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.LocalLifeOsColors
import com.example.viewmodel.TodayPrototypeViewModel

/** Lives in the same root as the source card/capsule; bounds grow and contract together. */
@Composable
fun FocusSurface(expanded: Boolean, origin: Rect?, focus: TodayPrototypeViewModel, now: Long,
                 onResume: () -> Unit, onCollapse: () -> Unit, message: String? = null) {
    val actual by focus.session.collectAsStateWithLifecycle()
    val saving by focus.saving.collectAsStateWithLifecycle()
    val error by focus.saveError.collectAsStateWithLifecycle()
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    var last by remember { mutableStateOf(actual) }
    SideEffect { if (actual.phase != FocusPhase.IDLE) last = actual }
    val session = if (actual.phase != FocusPhase.IDLE) actual else last
    val preview = session.sourceType == "PREVIEW"
    var reviewing by rememberSaveable(session.id) { mutableStateOf(false) }
    var material by rememberSaveable(session.id) { mutableStateOf(false) }
    val progress by animateFloatAsState(if (expanded && actual.phase != FocusPhase.IDLE) 1f else 0f,
        if (reduced) snap() else spring(dampingRatio = 1f, stiffness = 340f), label = "focus_bounds")
    val p = progress.coerceIn(0f, 1f)
    BackHandler(enabled = expanded || p > 0.01f) { onCollapse() }
    if (p > 0.001f) BoxWithConstraints(Modifier.fillMaxSize().testTag("focus_surface")) {
        val density = LocalDensity.current
        val fullWidth = constraints.maxWidth.toFloat()
        val fullHeight = constraints.maxHeight.toFloat()
        val fallback = Rect(fullWidth * 0.08f, fullHeight * 0.73f, fullWidth * 0.92f, fullHeight * 0.86f)
        val source = origin?.takeIf { it.width > 0 && it.height > 0 } ?: fallback
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f * p)).clickable(onClick = onCollapse))
        val shape = RoundedCornerShape((32f * (1f - p)).dp)
        Column(Modifier.offset { IntOffset((source.left * (1f - p)).toInt(), (source.top * (1f - p)).toInt()) }
            .width(with(density) { (source.width + (fullWidth - source.width) * p).toDp() })
            .height(with(density) { (source.height + (fullHeight - source.height) * p).toDp() })
            .clip(shape).background(colors.background)
            .background(Brush.verticalGradient(listOf(colors.primaryAccent.copy(alpha = 0.11f), Color.Transparent, colors.secondaryAccent.copy(alpha = 0.04f))))
            .border(1.dp, colors.primaryAccent.copy(alpha = 0.25f * (1f - p)), shape)
            .statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ContinuityOrb(Modifier.size(38.dp), compact = true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (preview) "PREVIEW · YOUR DAY STAYS AS IT IS" else if (session.kind == FocusKind.REST) "BREATHING ROOM" else "ONE THING AT A TIME", color = colors.primaryAccent, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Text(when (session.phase) { FocusPhase.FINISHED -> "Keep the thread"; FocusPhase.PAUSED -> "Your place is held"; else -> "You're here. That's enough." }, color = colors.textSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onCollapse) { Icon(Icons.Outlined.KeyboardArrowDown, "Minimize into the session capsule", tint = colors.primaryAccent) }
            }
            if (p > 0.35f) {
                Text(session.title, color = colors.textPrimary, fontSize = 24.sp, lineHeight = 31.sp)
                val remaining = session.remainingSeconds(now)
                FocusDial(1f - remaining.toFloat() / session.durationSeconds.coerceAtLeast(1), session.phase == FocusPhase.RUNNING,
                    Modifier.widthIn(max = 340.dp).fillMaxWidth()) {
                    BoxWithConstraints(Modifier.fillMaxWidth(0.65f), contentAlignment = Alignment.Center) {
                        val physicalSize = maxWidth.value / 3.7f / density.fontScale.coerceAtLeast(1f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%02d:%02d".format(remaining / 60, remaining % 60), color = colors.textPrimary, fontSize = physicalSize.sp)
                            Text(when (session.phase) { FocusPhase.PAUSED -> "PAUSED"; FocusPhase.FINISHED -> "TIME KEPT"; else -> "MINUTES FOR THIS" }, color = colors.primaryAccent, fontSize = 9.sp, letterSpacing = 1.2.sp)
                        }
                    }
                }
                if (session.firstStep.isNotBlank() && !reviewing && session.phase != FocusPhase.FINISHED) {
                    Text(session.firstStep, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                }
                if (session.material.isNotBlank()) {
                    WorkspaceTextButton(onClick = { material = !material }) { Text(if (material) "Hide material" else "My material / page") }
                    if (material) MaterialReference(session.material)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                message?.let { Text(it, color = colors.primaryAccent, fontSize = 12.sp, lineHeight = 18.sp) }
                val keep = reviewing || session.phase == FocusPhase.FINISHED
                AnimatedContent(keep, transitionSpec = {
                    fadeIn(tween(if (reduced) 0 else 180)) togetherWith fadeOut(tween(if (reduced) 0 else 100))
                }, label = "focus_to_keep") { showKeep ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (showKeep) {
                            Text("Make returning easy.", color = colors.textPrimary, fontSize = 23.sp)
                            OutlinedTextField(session.resumeNote, focus::updateResumeNote, label = { Text("Next time, start with…") },
                                placeholder = { Text("Page 12, example 3. I stopped at…") }, modifier = Modifier.fillMaxWidth().testTag("resume_point"), enabled = !saving, minLines = 2)
                            MoveButton(if (saving) "Keeping your place…" else if (preview) "Finish the preview" else "Save my progress", Modifier.fillMaxWidth().testTag("save_progress"), enabled = !saving) { focus.dismiss() }
                            Text(if (preview) "This ends the tryout. No task, plan block or study history is created." else "This saves the time and your note. It leaves the work open until you decide it's done.", color = colors.textTertiary, fontSize = 11.sp, lineHeight = 17.sp)
                            if (session.phase == FocusPhase.PAUSED) WorkspaceTextButton(onClick = { reviewing = false; onResume() }, enabled = !saving) { Text("A little more time instead") }
                        } else {
                            MoveButton(if (saving) "Checking your place…" else if (session.phase == FocusPhase.PAUSED) "Continue gently" else "Pause", Modifier.fillMaxWidth(), enabled = !saving) {
                                if (session.phase == FocusPhase.RUNNING) focus.pause() else onResume()
                            }
                            WorkspaceOutlinedButton(onClick = { if (session.phase == FocusPhase.RUNNING) focus.pause(); reviewing = true }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Save & finish this session") }
                            WorkspaceTextButton(onClick = onCollapse) { Text("Keep it in the capsule") }
                        }
                    }
                }
            }
        }
    }
}
