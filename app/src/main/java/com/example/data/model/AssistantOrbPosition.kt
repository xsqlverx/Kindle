package com.example.data.model

/** Normalised position survives rotation and different screen/inset sizes. */
data class AssistantOrbPlacement(val left: Boolean, val verticalFraction: Float)

class AssistantOrbBounds(width: Float, height: Float, size: Float, topInset: Float, bottomSpace: Float, edge: Float) {
    private fun finite(value: Float) = if (value.isFinite()) value.coerceAtLeast(0f) else 0f
    private val w = finite(width)
    private val h = finite(height)
    private val diameter = finite(size).coerceAtMost(w.coerceAtMost(h))
    private val horizontalRoom = (w - diameter).coerceAtLeast(0f)
    val leftX = finite(edge).coerceAtMost(horizontalRoom / 2f)
    val rightX = (horizontalRoom - leftX).coerceAtLeast(leftX)
    val topY = finite(topInset).coerceAtMost((h - diameter).coerceAtLeast(0f))
    val bottomY = (h - diameter - finite(bottomSpace)).coerceIn(topY, (h - diameter).coerceAtLeast(topY))

    fun clampX(x: Float): Float = (if (x.isFinite()) x else leftX).coerceIn(leftX, rightX)
    fun clampY(y: Float): Float = (if (y.isFinite()) y else topY).coerceIn(topY, bottomY)
    fun x(placement: AssistantOrbPlacement): Float = if (placement.left) leftX else rightX
    fun y(placement: AssistantOrbPlacement): Float {
        val fraction = if (placement.verticalFraction.isFinite()) placement.verticalFraction.coerceIn(0f, 1f) else 0.5f
        return topY + (bottomY - topY) * fraction
    }
    fun snap(x: Float, y: Float): AssistantOrbPlacement {
        val boundedX = clampX(x)
        val fraction = if (bottomY > topY) (clampY(y) - topY) / (bottomY - topY) else 0.5f
        return AssistantOrbPlacement(boundedX + diameter / 2f < w / 2f, fraction)
    }
}
