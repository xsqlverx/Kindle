package com.example.data.model

/** Projections from saved aggregate counts, not a verification of the college portal. */
data class AttendanceProjection(val attended: Int, val total: Int, val target: Int) {
    init { require(attended >= 0 && total >= attended && target in 1..100) }

    val percentage: Int? get() = if (total == 0) null else (attended * 100L / total).toInt()
    val isAtTarget: Boolean get() = total > 0 && attended * 100L >= total * target.toLong()
    val absenceBuffer: Int get() = if (!isAtTarget) 0 else
        ((attended * 100L - total * target.toLong()) / target).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    /** null means a perfect target cannot be reached finitely after a recorded absence. */
    val classesNeeded: Int? get() {
        if (total == 0 || isAtTarget) return 0
        if (target == 100) return null
        val deficit = total * target.toLong() - attended * 100L
        val gain = 100 - target
        return ((deficit + gain - 1) / gain).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
