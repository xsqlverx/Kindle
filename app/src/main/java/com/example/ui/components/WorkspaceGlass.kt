package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLifeOsColors

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalWorkspaceBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }

/** Replays the content-only layer at its root-aligned coordinates. Labels stay sharp above it.
 * MainActivity records the content into a separate softened layer; glass never captures itself.
 * Android <31 uses the same backdrop without RenderEffect and a denser readable tint.
 */
@Composable
fun BackdropGlass(modifier: Modifier = Modifier, radius: Int = 28, content: @Composable BoxScope.() -> Unit) {
    val colors = LocalLifeOsColors.current
    val backdrop = LocalWorkspaceBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(radius.dp)
    Box(modifier.onGloballyPositioned { position = it.positionInRoot() }.clip(shape)
        .drawWithContent {
            if (backdrop != null) translate(-position.x, -position.y) { drawLayer(backdrop) }
            drawContent()
        }
        .background(colors.surfaceGlass.copy(alpha = if (Build.VERSION.SDK_INT >= 31) 0.80f else 0.96f))
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, colors.primaryAccent.copy(alpha = 0.06f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), colors.borderGlass, colors.primaryAccent.copy(alpha = 0.22f))), shape),
        content = content)
}
