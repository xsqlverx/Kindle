package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalLifeOsColors
import kotlin.math.*
import kotlinx.coroutines.isActive

val LocalFocusOrigin = staticCompositionLocalOf<(androidx.compose.ui.geometry.Rect) -> Unit> { {} }

/** Observes the gesture without consuming it; nested fields, buttons and scrolling keep ownership. */
@Composable
fun Modifier.focusSource(): Modifier {
    val mark = LocalFocusOrigin.current
    var bounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    return this.onGloballyPositioned { bounds = it.boundsInRoot() }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            mark(bounds)
            waitForUpOrCancellation()
        }
    }
}

@Composable
fun MoveButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalLifeOsColors.current
    WorkspaceButton(onClick = { playAppleHaptic(context); onClick() }, enabled = enabled,
        shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent, contentColor = colors.background),
        modifier = modifier.heightIn(min = 56.dp)) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(19.dp))
    }
}

/** The luminous arc is actual timer progress. Only the low-amplitude light travels. */
@Composable
fun FocusDial(progress: Float, running: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val actual by animateFloatAsState(progress.coerceIn(0f, 1f), if (reduced) snap() else tween(900), label = "elapsed_arc")
    val light = remember { Animatable(0.18f) }
    LaunchedEffect(running, reduced) {
        if (running && !reduced) {
            while (isActive) {
                light.animateTo(light.value + 1f, tween(14_000, easing = LinearEasing))
            }
        }
    }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.42f
            val middle = center
            drawCircle(Brush.radialGradient(listOf(colors.primaryAccent.copy(alpha = 0.13f), Color.Transparent), middle, radius * 1.25f), radius * 1.25f, middle)
            drawCircle(Brush.radialGradient(listOf(colors.surfaceGlassHighlight.copy(alpha = 0.8f), colors.surfaceGlass.copy(alpha = 0.2f)), Offset(size.width * 0.36f, size.height * 0.3f), radius * 1.6f), radius, middle)
            drawCircle(colors.primaryAccent.copy(alpha = 0.13f), radius, middle, style = Stroke(1.dp.toPx()))
            drawCircle(colors.primaryAccent.copy(alpha = 0.10f), radius - 12.dp.toPx(), middle, style = Stroke(1.dp.toPx()))
            repeat(48) { index ->
                val angle = index * PI * 2 / 48 - PI / 2
                val outer = radius + if (index % 4 == 0) 9.dp.toPx() else 5.dp.toPx()
                drawLine(colors.primaryAccent.copy(alpha = if (index / 48f <= actual) 0.8f else 0.13f),
                    Offset(middle.x + cos(angle).toFloat() * radius, middle.y + sin(angle).toFloat() * radius),
                    Offset(middle.x + cos(angle).toFloat() * outer, middle.y + sin(angle).toFloat() * outer), 1.dp.toPx(), StrokeCap.Round)
            }
            drawArc(Brush.sweepGradient(listOf(colors.primaryAccent.copy(alpha = 0.35f), colors.primaryAccent, colors.secondaryAccent)),
                -90f, 360f * actual, false, topLeft = Offset(middle.x - radius, middle.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            val angle = (light.value * 2 * PI - PI / 2)
            val dot = Offset(middle.x + cos(angle).toFloat() * radius, middle.y + sin(angle).toFloat() * radius)
            drawCircle(Brush.radialGradient(listOf(colors.primaryAccent.copy(alpha = 0.5f), Color.Transparent), dot, 18.dp.toPx()), 18.dp.toPx(), dot)
            drawCircle(colors.primaryAccent, 2.dp.toPx(), dot)
        }
        content()
    }
}

@Composable
fun SavedBloom(recordId: String, modifier: Modifier = Modifier, description: String = "Progress saved") {
    val colors = LocalLifeOsColors.current
    val reduced = LocalReducedMotion.current
    val bloom = remember(recordId) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(recordId, reduced) { if (reduced) bloom.snapTo(1f) else bloom.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }
    Canvas(modifier.size(80.dp).semantics { contentDescription = description }) {
        val p = bloom.value
        drawCircle(colors.primaryAccent.copy(alpha = 0.12f), size.minDimension * 0.32f, center)
        drawCircle(colors.primaryAccent.copy(alpha = 0.65f * (1f - p)), size.minDimension * (0.3f + p * 0.19f), center, style = Stroke(1.5.dp.toPx()))
        repeat(10) { index ->
            val angle = 2 * PI * index / 10
            val radius = size.minDimension * (0.28f + p * 0.24f)
            drawCircle(colors.primaryAccent.copy(alpha = (1f - p).coerceIn(0f, 1f)), 2.dp.toPx(),
                Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius))
        }
        val path = Path().apply { moveTo(size.width * 0.36f, size.height * 0.51f); lineTo(size.width * 0.46f, size.height * 0.61f); lineTo(size.width * 0.66f, size.height * 0.38f) }
        val measure = PathMeasure().apply { setPath(path, false) }
        val segment = Path()
        measure.getSegment(0f, measure.length * (p * 1.7f).coerceIn(0f, 1f), segment)
        drawPath(segment, colors.primaryAccent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
