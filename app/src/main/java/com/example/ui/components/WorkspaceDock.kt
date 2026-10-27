package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Destination
import com.example.ui.theme.LocalLifeOsColors
import kotlin.math.roundToInt

fun Destination.icon(): ImageVector = when (this) {
    Destination.TODAY -> Icons.Outlined.WbSunny
    Destination.PLAN -> Icons.Outlined.CalendarToday
    Destination.COLLEGE -> Icons.Outlined.School
    Destination.STUDY -> Icons.Outlined.MenuBook
    Destination.FITNESS -> Icons.Outlined.FitnessCenter
    Destination.TASKS -> Icons.Outlined.CheckCircle
    Destination.PROJECTS -> Icons.Outlined.AccountTree
    Destination.CAPTURE -> Icons.Outlined.EditNote
    Destination.EXAMS -> Icons.Outlined.AutoStories
    Destination.PRACTICE -> Icons.Outlined.Code
    Destination.REVIEW -> Icons.Outlined.History
    Destination.RESET -> Icons.Outlined.Bedtime
    Destination.SETTINGS -> Icons.Outlined.Tune
}

@Composable
fun workspaceDockHeight() = 76.dp + ((LocalDensity.current.fontScale - 1f).coerceIn(0f, 1f) * 36f).dp

@Composable
fun NavbarPreview(slots: List<Destination>, selected: Int, onReorder: (Int, Int) -> Unit, onSelect: (Int) -> Unit) {
    var width by remember { mutableFloatStateOf(1f) }
    BackdropGlass(Modifier.fillMaxWidth(), radius = 24) {
        Row(Modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat() }.padding(4.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { position ->
                if (position == 2) Box(Modifier.weight(1f).height(72.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Apps, "Fixed centre launcher", tint = LocalLifeOsColors.current.primaryAccent)
                } else {
                    val index = if (position < 2) position else position - 1
                    var dragged by remember { mutableFloatStateOf(0f) }
                    DockItem(slots[index], selected == index, Modifier.weight(1f).graphicsLayer { translationX = dragged }
                        .pointerInput(slots, index, width) {
                            detectDragGesturesAfterLongPress(onDragStart = { onSelect(index) }, onDragCancel = { dragged = 0f },
                                onDragEnd = {
                                    var target = (position + dragged / (width / 5f)).roundToInt().coerceIn(0, 4)
                                    if (target == 2) target = if (dragged > 0f) 3 else 1
                                    val targetSlot = if (target < 2) target else target - 1
                                    dragged = 0f
                                    if (targetSlot != index) onReorder(index, targetSlot)
                                }) { change, amount -> change.consume(); dragged += amount.x }
                        }) { onSelect(index) }
                }
            }
        }
    }
}

@Composable
private fun DockItem(destination: Destination, selected: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalLifeOsColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, if (reduced) snap() else spring(0.65f, 700f), label = "dock_touch")
    val selectedAmount by animateFloatAsState(if (selected) 1f else 0f, if (reduced) snap() else spring(0.72f, 500f), label = "dock_lift")
    val tint by animateColorAsState(if (selected) colors.primaryAccent else colors.textSecondary, tween(if (reduced) 0 else 180), label = "dock_tint")
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationY = -1.5.dp.toPx() * selectedAmount }.clip(RoundedCornerShape(20.dp))
        .background(colors.primaryAccent.copy(alpha = (0.18f * selectedAmount).coerceIn(0f, 1f)))
        .selectable(selected, enabled = enabled, interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
        .heightIn(min = 66.dp).padding(horizontal = 2.dp, vertical = 10.dp).testTag("destination_${destination.id}"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(destination.icon(), null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(5.dp))
        Text(if (destination == Destination.RESET) "Reset" else destination.title, fontSize = 10.sp,
            color = tint, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** One centre surface changes bounds, anchor and corner radius, then reveals its library.
 * Overlay bounds never participate in the content's reserved bottom inset.
 */
@Composable
fun WorkspaceDock(slots: List<Destination>, active: Destination, expanded: Boolean,
                  onExpanded: (Boolean) -> Unit, onNavigate: (Destination) -> Unit, modifier: Modifier = Modifier, onQuickMove: () -> Unit = {}) {
    val colors = LocalLifeOsColors.current
    val context = LocalContext.current
    val reduced = LocalReducedMotion.current
    val transition = updateTransition(expanded, label = "centre_unfold")
    val dockHeight = workspaceDockHeight()
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val close: () -> Unit = { focusManager.clearFocus(); keyboard?.hide(); onExpanded(false) }
    val rawProgress by transition.animateFloat(transitionSpec = { if (reduced) snap() else spring(dampingRatio = 0.9f, stiffness = 320f) }, label = "centre_shape") { if (it) 1f else 0f }
    val progress = rawProgress.coerceIn(0f, 1f)
    LaunchedEffect(expanded) { if (!expanded) { query = ""; focusManager.clearFocus(); keyboard?.hide() } }
    BoxWithConstraints(modifier.fillMaxSize().imePadding()) {
        val panelHeight = (maxHeight * 0.80f).coerceAtMost(640.dp)
        if (expanded || progress > 0f) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.42f * progress))
                .clickable { close() }.semantics { contentDescription = "Close destination launcher" })
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth().height(dockHeight).graphicsLayer { alpha = 1f - progress; translationY = progress * 18.dp.toPx() },
            verticalAlignment = Alignment.CenterVertically) {
            if (progress < 0.99f) {
                BackdropGlass(Modifier.weight(2f), radius = 26) {
                    Row { DockItem(slots[0], active == slots[0], Modifier.weight(1f), !expanded && progress < 0.01f) { onNavigate(slots[0]) }
                        DockItem(slots[1], active == slots[1], Modifier.weight(1f), !expanded && progress < 0.01f) { onNavigate(slots[1]) } }
                }
                Spacer(Modifier.weight(1f))
                BackdropGlass(Modifier.weight(2f), radius = 26) {
                    Row { DockItem(slots[2], active == slots[2], Modifier.weight(1f), !expanded && progress < 0.01f) { onNavigate(slots[2]) }
                        DockItem(slots[3], active == slots[3], Modifier.weight(1f), !expanded && progress < 0.01f) { onNavigate(slots[3]) } }
                }
            }
        }
        BackdropGlass(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 15.dp)
            .width(62.dp + (maxWidth - 94.dp) * progress).height((dockHeight - 10.dp) + (panelHeight - dockHeight + 10.dp) * progress)
            .testTag("centre_launcher"), radius = (30 - 4 * progress).toInt()) {
            if (progress < 0.55f) {
                IconButton(onClick = { playAppleHaptic(context); onExpanded(!expanded) },
                    modifier = Modifier.fillMaxSize().semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
                    Icon(Icons.Outlined.Apps, "Open all destinations", tint = colors.primaryAccent,
                        modifier = Modifier.graphicsLayer { rotationZ = 45f * progress; alpha = 1f - progress })
                }
            } else {
                Column(Modifier.fillMaxSize().graphicsLayer {
                    alpha = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleY = 0.95f + 0.05f * progress
                }.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("YOUR SPACES", color = colors.primaryAccent, fontSize = 11.sp, letterSpacing = 1.8.sp)
                            Text(active.title, color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = { playAppleHaptic(context); close() }) {
                            Icon(Icons.Outlined.Close, "Close launcher", tint = colors.textPrimary)
                        }
                    }
                    HorizontalDivider(color = colors.borderGlass, modifier = Modifier.padding(vertical = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoveButton("Make a next move", Modifier.fillMaxWidth()) { close(); onQuickMove() }
                        OutlinedTextField(query, { query = it }, label = { Text("Find a space") },
                            modifier = Modifier.fillMaxWidth().testTag("launcher_search"), singleLine = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val groups = Destination.groups
                        var found = false
                        groups.forEach { (heading, entries) ->
                            val matches = entries.filter { it.title.contains(query, true) || it.purpose.contains(query, true) }
                            if (matches.isNotEmpty()) {
                                found = true
                                Text(heading.uppercase(), color = colors.textTertiary, fontSize = 10.sp, letterSpacing = 1.2.sp)
                                matches.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pair.forEach { destination ->
                                            LiquidGlassCard(Modifier.weight(1f).testTag("launcher_${destination.id}"), shape = RoundedCornerShape(18.dp),
                                                tint = colors.primaryAccent.copy(alpha = if (active == destination) 0.18f else 0.045f),
                                                onClick = { close(); onNavigate(destination) }) {
                                                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(destination.icon(), null, tint = colors.primaryAccent, modifier = Modifier.size(20.dp))
                                                        if (destination in slots) Text("  PINNED", color = colors.textTertiary, fontSize = 8.sp)
                                                    }
                                                    Text(destination.title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(destination.purpose, color = colors.textSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                                                }
                                            }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        if (!found) Text("No matching space. All 13 are here when you clear the search.", color = colors.textSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
