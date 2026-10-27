package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.LifeOsDatabase
import com.example.data.model.*

/** Transactions, rather than a UI cooldown, protect totals through rapid taps and restarts. */
class AttendanceLedger(private val db: LifeOsDatabase) {
    suspend fun mark(occurrence: AttendanceOccurrence, choice: AttendanceChoice, expectedStatus: String? = null,
        now: Long = System.currentTimeMillis()): AttendanceWriteResult = db.withTransaction {
        val dao = db.academicDao()
        val old = dao.attendanceEvent(occurrence.key)
        if (old?.status == choice.name) return@withTransaction AttendanceWriteResult(occurrence.key, false, "Already saved · ${choice.label}")
        check(old?.status == expectedStatus) { "This class changed. Reopen Change to review its saved mark." }
        check(old?.includedInBaseline != true) { "This class is included in corrected totals. Use Correct totals to reconcile it." }
        val record = dao.attendanceByCourse(occurrence.courseCode)
            ?: error("Add your real attendance totals before marking this course.")
        check(record.attendedClasses >= 0 && record.totalClasses >= record.attendedClasses) { "Correct the saved totals first." }
        check(record.totalClasses > 0 || record.baselineAt > 0) { "Add your real attendance totals before marking this course." }
        check(occurrence.startMillis <= now) { "This class has not started yet." }
        check(record.baselineAt == 0L || occurrence.startMillis > record.baselineAt) { "This class is covered by your corrected totals." }
        val entry = dao.timetableSnapshot().firstOrNull {
            runCatching { attendanceOccurrence(it, occurrence.startMillis).key == occurrence.key }.getOrDefault(false)
        }
        check(entry != null || old != null) { "This class is no longer in your saved timetable." }
        fun attended(status: String?) = if (status == "PRESENT") 1 else 0
        fun held(status: String?) = if (status == "PRESENT" || status == "ABSENT") 1 else 0
        val a = record.attendedClasses + attended(choice.name) - attended(old?.status)
        val t = record.totalClasses + held(choice.name) - held(old?.status)
        check(a >= 0 && t >= a) { "These totals need reconciliation. Use Correct totals." }
        dao.updateAttendance(record.copy(attendedClasses = a, totalClasses = t))
        dao.saveAttendanceEvent(AttendanceEvent(occurrence.key, occurrence.courseCode, occurrence.date,
            occurrence.startMillis, occurrence.startMinute, occurrence.endMinute, choice.name, now))
        AttendanceWriteResult(occurrence.key, true, when (choice) {
            AttendanceChoice.UNMARKED -> "Mark removed · totals restored"
            AttendanceChoice.CANCELLED -> "Saved · Cancelled, no class counted"
            else -> "Saved · ${choice.label}"
        })
    }

    suspend fun correctTotals(courseCode: String, attended: Int, total: Int, target: Int,
        now: Long = System.currentTimeMillis()) = db.withTransaction {
        require(courseCode.isNotBlank() && attended >= 0 && total >= attended && target in 1..100)
        val dao = db.academicDao()
        val old = dao.attendanceByCourse(courseCode)
        val record = (old ?: AttendanceRecord(courseCode = courseCode)).copy(attendedClasses = attended,
            totalClasses = total, targetPercentage = target, baselineAt = now)
        if (old == null) dao.insertAttendance(record) else dao.updateAttendance(record)
        // Keep history and deduplication identities; totals supplied now already include those marks.
        dao.absorbAttendanceEvents(courseCode)
    }
}
