package com.example.ui.components

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.model.EntryKind
import com.example.data.model.FocusPhase
import com.example.data.model.FocusSession
import com.example.ui.theme.LocalLifeOsColors
import kotlinx.coroutines.delay
import kotlin.math.*

val LocalEntryLauncher = staticCompositionLocalOf<((EntryKind?) -> Unit)?> { null }
val LocalEntryPrefill = staticCompositionLocalOf<((String, String) -> Unit)?> { null }
val LocalOrbScene = staticCompositionLocalOf<OrbScene?> { null }
val LocalOrbActivity = staticCompositionLocalOf { true }

class OrbScene {
    val phase = mutableFloatStateOf(0f)
    val bloom = Animatable(0f)
    var visible by mutableIntStateOf(0)
    var mode by mutableStateOf(FocusPhase.IDLE)
    var preview by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var still by mutableStateOf(false)
}

/** One phase clock for both sizes. Draw reads do not recompose the screen every frame. */
@Composable
fun rememberOrbScene(session: FocusSession, now: Long, saveToken: String, reduced: Boolean, enabled: Boolean): OrbScene {
    val scene = remember { OrbScene() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resolver = LocalContext.current.contentResolver
    var systemStill by remember { mutableStateOf(false) }
    DisposableEffect(resolver) {
        fun updateScale() { systemStill = runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= 0f }.getOrDefault(false) }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) = updateScale() }
        updateScale()
        val registered = runCatching { resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer) }.isSuccess
        onDispose { if (registered) runCatching { resolver.unregisterContentObserver(observer) } }
    }
    val still = reduced || systemStill
    SideEffect {
        scene.mode = session.phase; scene.preview = session.sourceType == "PREVIEW"; scene.still = still
        scene.progress = if (session.phase == FocusPhase.IDLE) 0f else (1f - session.remainingSeconds(now).toFloat() / session.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    }
    LaunchedEffect(lifecycle, still, enabled) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastFrame = 0L
            while (true) {
                if (enabled && !still && scene.visible > 0) {
                    withFrameNanos { frame ->
                        if (lastFrame != 0L) scene.phase.floatValue = (scene.phase.floatValue + ((frame - lastFrame).coerceAtMost(80_000_000L) / 1_000_000_000f) * 0.32f) % (2f * PI.toFloat())
                        lastFrame = frame
                    }
                    delay(24) // cap decorative sampling below display refresh; no per-pixel shader/bitmap work
                } else { lastFrame = 0L; delay(120) }
            }
        }
    }
    val seenSaves = remember { mutableSetOf<String>() }
    LaunchedEffect(saveToken, still) {
        val firstAppearance = saveToken.isNotBlank() && seenSaves.add(saveToken)
        if (firstAppearance && !still) { scene.bloom.snapTo(1f); scene.bloom.animateTo(0f, tween(850, easing = FastOutSlowInEasing)) }
        else scene.bloom.snapTo(0f)
    }
    return scene
}

@Composable
fun ContinuityOrb(modifier: Modifier = Modifier, compact: Boolean = false, onClick: (() -> Unit)? = null) {
    val scene = LocalOrbScene.current ?: return
    val colors = LocalLifeOsColors.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val active = LocalOrbActivity.current
    var visible by remember { mutableStateOf(false) }
    DisposableEffect(scene, visible, active) {
        if (visible && active) scene.visible++
        onDispose { if (visible && active) scene.visible-- }
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, if (scene.still) snap() else spring(0.7f, 500f), label = "orb_press")
    val description = (if (scene.preview) "Preview session · " else "") + when (scene.mode) {
        FocusPhase.RUNNING -> "Focus in progress"
        FocusPhase.PAUSED -> "Session paused"
        FocusPhase.FINISHED -> "Session ready to review"
        FocusPhase.IDLE -> "Add or log something"
    }
    Box(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        visible = bounds.bottom > 0 && bounds.top < view.height && bounds.right > 0 && bounds.left < view.width
    }.testTag(if (compact) "orb_compact" else "orb_today")
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(CircleShape).then(if (focused && onClick != null) Modifier.border(2.dp, colors.textPrimary, CircleShape) else Modifier)
        .then(if (onClick != null) Modifier.clickable(source, indication = null, role = Role.Button, onClickLabel = "Add or log", onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }) else Modifier)
        .semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxSize()) {
            val phase = if (scene.still) 0.45f else scene.phase.floatValue
            val pulse = scene.bloom.value
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * (if (compact) 0.34f else 0.33f) * (1f + sin(phase * 1.7f) * 0.014f)
            val accent = if (scene.mode == FocusPhase.PAUSED) colors.textSecondary else colors.primaryAccent
            val pale = if (colors.isDark) Color(0xFFE6DEFF) else Color(0xFFFAF7FF)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f + pulse * 0.15f), Color.Transparent), center, radius * 1.48f), radius * 1.48f, center)
            // A translucent, shaded sphere, then refracted latitude ribbons clipped to its surface.
            drawCircle(Brush.radialGradient(listOf(pale.copy(alpha = 0.92f), accent.copy(alpha = 0.88f), Color(0xFF282537), Color(0xFF100F18)), center - Offset(radius * 0.36f, radius * 0.44f), radius * 1.7f), radius, center)
            clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(center - Offset(radius, radius), Size(radius * 2, radius * 2))) }) {
                drawCircle(Brush.radialGradient(listOf(colors.secondaryAccent.copy(alpha = 0.58f), Color.Transparent), center + Offset(sin(phase) * radius * 0.42f, radius * 0.45f), radius), radius, center + Offset(0f, radius * 0.25f))
                repeat(if (compact) 4 else 9) { ribbon ->
                    val path = Path()
                    val band = ribbon / (if (compact) 3f else 8f) * 2f - 1f
                    for (step in 0..64) {
                        val x = step / 32f - 1f
                        val y = band * 0.85f + sin(x * 2.6f + phase + band * 1.8f) * (0.13f + 0.11f * (1f - abs(band)))
                        val point = center + Offset(x * radius * 1.12f, (y + x * 0.23f) * radius)
                        if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, Brush.linearGradient(listOf(Color.Transparent, pale.copy(alpha = 0.52f), accent.copy(alpha = 0.14f), Color.Transparent)), style = Stroke(if (compact) 1.2f else 2.2f))
                }
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.65f), Color.Transparent), center - Offset(radius * 0.36f, radius * 0.48f), radius * 0.63f), radius * 0.66f, center - Offset(radius * 0.35f, radius * 0.48f))
            }
            drawCircle(pale.copy(alpha = 0.32f), radius, center, style = Stroke(1.1f))
            // Orbital filaments connect foreground and rear halves; dot depth follows actual angle.
            repeat(if (compact) 1 else 2) { orbit ->
                val orbitPath = Path()
                val tilt = if (orbit == 0) -0.32f else 0.48f
                for (step in 0..96) {
                    val angle = step / 96f * 2f * PI.toFloat()
                    val x = cos(angle) * radius * 1.3f
                    val y = sin(angle) * radius * 0.38f
                    val point = center + Offset(x * cos(tilt) - y * sin(tilt), x * sin(tilt) + y * cos(tilt))
                    if (step == 0) orbitPath.moveTo(point.x, point.y) else orbitPath.lineTo(point.x, point.y)
                }
                drawPath(orbitPath, pale.copy(alpha = if (compact) 0.25f else 0.38f), style = Stroke(if (compact) 1f else 1.5f))
                val angle = phase * (if (orbit == 0) 1f else -0.8f) + orbit * 2.7f
                val x = cos(angle) * radius * 1.3f; val y = sin(angle) * radius * 0.38f
                drawCircle(pale, if (compact) 1.8f else 3.4f, center + Offset(x * cos(tilt) - y * sin(tilt), x * sin(tilt) + y * cos(tilt)))
            }
            if (scene.mode != FocusPhase.IDLE) drawArc(accent, -90f, scene.progress * 360f, false,
                center - Offset(radius * 1.42f, radius * 1.42f), Size(radius * 2.84f, radius * 2.84f), style = Stroke(if (compact) 2f else 3.5f, cap = StrokeCap.Round))
            if (pulse > 0f) drawCircle(accent.copy(alpha = pulse * 0.45f), radius * (1.05f + (1f - pulse) * 0.4f), center, style = Stroke(2f))
        }
    }
}
