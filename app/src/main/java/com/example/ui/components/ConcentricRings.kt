package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ConcentricActivityRings(
    habitProgress: Float, // 0.0f to 1.0f
    waterProgress: Float,
    workoutProgress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    strokeWidth: Dp = 13.dp
) {
    val lifeColors = LocalLifeOsColors.current

    val animatedHabits by animateFloatAsState(
        targetValue = habitProgress.coerceIn(0f, 1.25f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 280f),
        label = "ringHabits"
    )
    val animatedWater by animateFloatAsState(
        targetValue = waterProgress.coerceIn(0f, 1.25f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 280f),
        label = "ringWater"
    )
    val animatedWorkout by animateFloatAsState(
        targetValue = workoutProgress.coerceIn(0f, 1.25f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 280f),
        label = "ringWorkout"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val spacingPx = 4.dp.toPx()
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer Ring: Habits (Iris)
            val outerRadius = (this.size.width / 2f) - (strokePx / 2f)
            drawCircle(
                color = IrisRadiant.copy(alpha = 0.16f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(IrisPrimary, IrisRadiant, IrisSoft, IrisRadiant)
                ),
                startAngle = -90f,
                sweepAngle = (animatedHabits * 360f).coerceAtMost(360f),
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Middle Ring: Water (Cyan)
            val middleRadius = outerRadius - strokePx - spacingPx
            drawCircle(
                color = RingWaterCyan.copy(alpha = 0.16f),
                radius = middleRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(RingWaterCyan.copy(alpha = 0.8f), RingWaterCyan, Color(0xFF67E8F9), RingWaterCyan)
                ),
                startAngle = -90f,
                sweepAngle = (animatedWater * 360f).coerceAtMost(360f),
                useCenter = false,
                topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                size = Size(middleRadius * 2, middleRadius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Inner Ring: Movement / Workout (Rose)
            val innerRadius = middleRadius - strokePx - spacingPx
            drawCircle(
                color = RingGymRose.copy(alpha = 0.16f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(RingGymRose.copy(alpha = 0.8f), RingGymRose, Color(0xFFFDA4AF), RingGymRose)
                ),
                startAngle = -90f,
                sweepAngle = (animatedWorkout * 360f).coerceAtMost(360f),
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Center Glass Ring Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${((animatedHabits * 0.4f + animatedWater * 0.3f + animatedWorkout * 0.3f) * 100).toInt()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Text(
                text = "DAILY",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = lifeColors.textTertiary
            )
        }
    }
}
