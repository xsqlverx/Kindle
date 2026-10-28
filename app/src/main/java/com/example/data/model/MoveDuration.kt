package com.example.data.model

/** Whole-minute input has one meaning across the editor and the final write guard. */
object MoveDuration {
    const val MIN = 1
    const val MAX = 180
    fun parse(text: String): Int? {
        val value = text.trim()
        if (value.isEmpty() || value.any { it !in '0'..'9' }) return null
        return value.toIntOrNull()?.takeIf { it in MIN..MAX }
    }
}
