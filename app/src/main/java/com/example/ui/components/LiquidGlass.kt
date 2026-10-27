package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLifeOsColors

fun playAppleHaptic(context: Context, strong: Boolean = false) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (strong) {
                    VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(12, 100)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (strong) 25 else 12)
            }
        }
    } catch (_: Exception) {}
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color? = null,
    borderAlpha: Float = 0.4f,
    elevation: Dp = 0.dp,
    hasSpecularSheen: Boolean = true
): Modifier {
    val lifeColors = LocalLifeOsColors.current
    val baseGlassColor = tint ?: lifeColors.surfaceGlass

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            lifeColors.textTertiary.copy(alpha = (borderAlpha * 0.55f).coerceAtMost(0.4f)),
            lifeColors.borderGlass.copy(alpha = 0.55f),
            lifeColors.borderGlass.copy(alpha = 0.25f),
            lifeColors.borderGlass.copy(alpha = 0.5f)
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 800f)
    )

    val specularBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (lifeColors.isDark) 0.035f else 0.45f),
            Color.Transparent,
            Color.White.copy(alpha = if (lifeColors.isDark) 0.012f else 0.15f)
        ),
        start = Offset(0f, 0f),
        end = Offset(300f, 400f)
    )

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = lifeColors.ambientGlow,
            spotColor = lifeColors.ambientGlow
        )
        .clip(shape)
        .background(baseGlassColor)
        .drawBehind {
            if (hasSpecularSheen) {
                // Subtle liquid light ray across the top surface
                drawRect(brush = specularBrush)
            }
        }
        .border(
            width = 0.8.dp,
            brush = borderBrush,
            shape = shape
        )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color? = null,
    borderAlpha: Float = 0.4f,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = if (LocalReducedMotion.current) snap() else spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glassCardScale"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            playAppleHaptic(context, strong = false)
            onClick()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(animatedScale)
            .liquidGlass(
                shape = shape,
                tint = tint,
                borderAlpha = borderAlpha,
                elevation = elevation
            )
            .then(clickableModifier),
        content = content
    )
}
