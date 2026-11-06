package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantOrbBounds
import com.example.data.model.AssistantOrbPlacement
import com.example.ui.theme.LocalLifeOsColors
import kotlin.math.sin

/** In-app assistant shortcut; the shared phase is paused with the app and reduced motion. */
@Composable
fun FloatingAssistantOrb(position: AssistantOrbPlacement, bottomSpacePx: Float,
    onMove: (AssistantOrbPlacement) -> Unit, onOpen: () -> Unit) {
    val scene = LocalOrbScene.current ?: return
    val colors = LocalLifeOsColors.current
    val density = LocalDensity.current
    val reduced = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    val moveCallback by rememberUpdatedState(onMove)
    val openCallback by rememberUpdatedState(onOpen)
    DisposableEffect(scene) { scene.visible++; onDispose { scene.visible-- } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val diameter = with(density) { 58.dp.toPx() }
        val status = WindowInsets.statusBars.getTop(density).toFloat()
        val screenWidth = with(density) { maxWidth.toPx() }
        val screenHeight = with(density) { maxHeight.toPx() }
        val top = status + with(density) { 82.dp.toPx() }
        val edge = with(density) { 7.dp.toPx() }
        val bounds = remember(screenWidth, screenHeight, diameter, top, bottomSpacePx, edge) {
            AssistantOrbBounds(screenWidth, screenHeight, diameter, top, bottomSpacePx, edge)
        }
        var dragging by remember { mutableStateOf(false) }
        var dragX by remember { mutableFloatStateOf(0f) }
        var dragY by remember { mutableFloatStateOf(0f) }
        val x by animateFloatAsState(if (dragging) dragX else bounds.x(position),
            if (dragging || reduced) snap() else spring(dampingRatio = 0.76f, stiffness = 480f), label = "assistant_orb_x")
        val y by animateFloatAsState(if (dragging) dragY else bounds.y(position),
            if (dragging || reduced) snap() else spring(dampingRatio = 0.86f, stiffness = 480f), label = "assistant_orb_y")
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val touchScale by animateFloatAsState(if (pressed || dragging) 0.92f else 1f,
            if (reduced) snap() else spring(dampingRatio = 0.7f, stiffness = 600f), label = "assistant_orb_touch")
        Canvas(Modifier.size(58.dp).graphicsLayer {
            translationX = x; translationY = y; scaleX = touchScale; scaleY = touchScale
        }.testTag("floating_assistant_orb")
            .pointerInput(bounds) {
                detectDragGestures(onDragStart = {
                    dragX = bounds.clampX(x); dragY = bounds.clampY(y); dragging = true
                }, onDragEnd = {
                    val next = bounds.snap(dragX, dragY)
                    moveCallback(next); dragging = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }, onDragCancel = { dragging = false }) { change, amount ->
                    change.consume(); dragX = bounds.clampX(dragX + amount.x); dragY = bounds.clampY(dragY + amount.y)
                }
            }.clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button,
                onClickLabel = "Open Assistant") { openCallback() }
            .semantics {
                contentDescription = "Assistant. Tap to open; drag to reposition."
                customActions = listOf(
                    CustomAccessibilityAction("Move assistant to left edge") { moveCallback(position.copy(left = true)); true },
                    CustomAccessibilityAction("Move assistant to right edge") { moveCallback(position.copy(left = false)); true },
                    CustomAccessibilityAction("Move assistant higher") { moveCallback(position.copy(verticalFraction = (position.verticalFraction - 0.2f).coerceIn(0f, 1f))); true },
                    CustomAccessibilityAction("Move assistant lower") { moveCallback(position.copy(verticalFraction = (position.verticalFraction + 0.2f).coerceIn(0f, 1f))); true }
                )
            }) {
            val phase = if (scene.still) 0.45f else scene.phase.floatValue
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.38f
            val accent = colors.primaryAccent
            val violet = Color(0xFF7962D3)
            val ice = Color(0xFFE9E3FF)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.34f), Color.Transparent), c, r * 1.32f), r * 1.32f, c)
            drawCircle(Brush.radialGradient(listOf(ice, accent, violet, Color(0xFF171329)),
                c - Offset(r * 0.33f, r * 0.45f), r * 1.65f), r, c)
            clipPath(Path().apply { addOval(Rect(c - Offset(r, r), Size(r * 2f, r * 2f))) }) {
                repeat(3) { band ->
                    val path = Path()
                    for (step in 0..36) {
                        val xPos = step / 18f - 1f
                        val yPos = (band - 1) * 0.32f + sin(xPos * 3.0f + phase * (1f + band * 0.16f) + band) * 0.24f
                        val point = c + Offset(xPos * r, yPos * r)
                        if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, Brush.linearGradient(listOf(Color.Transparent, ice.copy(alpha = 0.65f), accent.copy(alpha = 0.45f), Color.Transparent)),
                        style = Stroke(width = 1.6.dp.toPx()))
                }
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                    c - Offset(r * 0.36f, r * 0.46f), r * 0.75f), r * 0.75f, c - Offset(r * 0.36f, r * 0.46f))
            }
            drawCircle(ice.copy(alpha = 0.58f), r, c, style = Stroke(width = 1.dp.toPx()))
        }
    }
}
