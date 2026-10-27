package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AttendanceChoice(val label: String) {
    PRESENT("Present"), ABSENT("Absent"), CANCELLED("Cancelled"), UNMARKED("Unmarked")
}

/** A timetable row can be re-imported without giving the same dated period a second identity. */
data class AttendanceOccurrence(
    val key: String,
    val courseCode: String,
    val date: String,
    val startMillis: Long,
    val startMinute: Int,
    val endMinute: Int
)

@Entity(tableName = "attendance_events")
data class AttendanceEvent(
    @PrimaryKey val key: String,
    val courseCode: String,
    val date: String,
    val startMillis: Long,
    val startMinute: Int,
    val endMinute: Int,
    val status: String,
    val updatedAt: Long,
    val includedInBaseline: Boolean = false
)

fun attendanceOccurrence(entry: TimetableEntry, dateMillis: Long): AttendanceOccurrence {
    require(entry.courseCode.isNotBlank())
    require(entry.startHour in 0..23 && entry.endHour in 0..23 && entry.startMinute in 0..59 && entry.endMinute in 0..59)
    val start = entry.startHour * 60 + entry.startMinute
    val end = entry.endHour * 60 + entry.endMinute
    require(end > start)
    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
    require(calendar.get(Calendar.DAY_OF_WEEK) == entry.dayOfWeek)
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(calendar.time)
    calendar.set(Calendar.HOUR_OF_DAY, entry.startHour)
    calendar.set(Calendar.MINUTE, entry.startMinute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return AttendanceOccurrence("$date|${entry.courseCode.length}:${entry.courseCode}|$start|$end", entry.courseCode,
        date, calendar.timeInMillis, start, end)
}

/** Explicitly displayed latest calendar occurrence, never a claim the class took place. */
fun latestAttendanceDate(entry: TimetableEntry, now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -((get(Calendar.DAY_OF_WEEK) - entry.dayOfWeek + 7) % 7))
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

data class AttendanceWriteResult(val key: String, val changed: Boolean, val message: String)

data class AttendanceActionState(
    val busy: Boolean = false,
    val key: String = "",
    val error: String? = null,
    val confirmation: String = "",
    val confirmationId: Long = 0
)
