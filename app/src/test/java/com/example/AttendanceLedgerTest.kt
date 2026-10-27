package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.AttendanceLedger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/** Supplied for the external cloud runner; never executed locally. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttendanceLedgerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val entry = TimetableEntry(courseCode = "CS", courseName = "Computer science", dayOfWeek = Calendar.MONDAY,
        startHour = 9, startMinute = 0, endHour = 9, endMinute = 45, room = "A")
    private val date = Calendar.getInstance().apply {
        set(2025, Calendar.JANUARY, 6, 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val period = attendanceOccurrence(entry, date)
    private val now = period.startMillis + 3_600_000
    private fun database() = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java).build()

    @Test fun `concurrent repeated marks across ledger instances count only once`() = runBlocking {
        val db = database()
        try {
            db.academicDao().insertTimetableEntry(entry)
            db.academicDao().insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 17, totalClasses = 20))
            coroutineScope {
                (1..50).map { async(Dispatchers.Default) { AttendanceLedger(db).mark(period, AttendanceChoice.PRESENT, now = now) } }.awaitAll()
            }
            val record = db.academicDao().attendanceByCourse("CS")!!
            assertEquals(18, record.attendedClasses); assertEquals(21, record.totalClasses)
            assertEquals(1, db.academicDao().attendanceEvents().first().size)
            assertFalse(AttendanceLedger(db).mark(period, AttendanceChoice.PRESENT, now = now).changed)
        } finally { db.close() }
    }

    @Test fun `correction cancellation undo and remark replace one occurrence`() = runBlocking {
        val db = database()
        try {
            val dao = db.academicDao(); val ledger = AttendanceLedger(db)
            dao.insertTimetableEntry(entry)
            dao.insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 17, totalClasses = 20))
            ledger.mark(period, AttendanceChoice.PRESENT, now = now)
            ledger.mark(period, AttendanceChoice.ABSENT, "PRESENT", now)
            assertEquals(17, dao.attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(21, dao.attendanceByCourse("CS")!!.totalClasses)
            ledger.mark(period, AttendanceChoice.CANCELLED, "ABSENT", now)
            assertEquals(20, dao.attendanceByCourse("CS")!!.totalClasses)
            ledger.mark(period, AttendanceChoice.UNMARKED, "CANCELLED", now)
            assertFalse(ledger.mark(period, AttendanceChoice.UNMARKED, "CANCELLED", now).changed)
            ledger.mark(period, AttendanceChoice.PRESENT, "UNMARKED", now)
            assertEquals(18, dao.attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(21, dao.attendanceByCourse("CS")!!.totalClasses)
            assertEquals(1, dao.attendanceEvents().first().size)
        } finally { db.close() }
    }

    @Test fun `distinct periods count independently but identical imported rows keep their identity`() = runBlocking {
        val db = database()
        try {
            val dao = db.academicDao(); val ledger = AttendanceLedger(db)
            dao.insertTimetableEntry(entry)
            val second = entry.copy(startHour = 10, endHour = 10)
            dao.insertTimetableEntry(second)
            dao.insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 5, totalClasses = 8))
            ledger.mark(period, AttendanceChoice.PRESENT, now = now)
            val reimport = attendanceOccurrence(entry.copy(id = 999, room = "Changed room"), date)
            assertEquals(period.key, reimport.key)
            assertFalse(ledger.mark(reimport, AttendanceChoice.PRESENT, now = now).changed)
            ledger.mark(attendanceOccurrence(second, date), AttendanceChoice.ABSENT, now = now + 3_600_000)
            assertEquals(6, dao.attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(10, dao.attendanceByCourse("CS")!!.totalClasses)
            assertEquals(2, dao.attendanceEvents().first().size)
        } finally { db.close() }
    }

    @Test fun `missing or unconfirmed zero baseline never invents historical totals`() = runBlocking {
        val db = database()
        try {
            val dao = db.academicDao(); val ledger = AttendanceLedger(db)
            dao.insertTimetableEntry(entry)
            assertTrue(runCatching { ledger.mark(period, AttendanceChoice.PRESENT, now = now) }.isFailure)
            assertNull(dao.attendanceByCourse("CS"))
            dao.insertAttendance(AttendanceRecord(courseCode = "CS"))
            assertTrue(runCatching { ledger.mark(period, AttendanceChoice.PRESENT, now = now) }.isFailure)
            ledger.correctTotals("CS", 0, 0, 80, period.startMillis - 1)
            ledger.mark(period, AttendanceChoice.PRESENT, now = now)
            assertEquals(1, dao.attendanceByCourse("CS")!!.totalClasses)
        } finally { db.close() }
    }

    @Test fun `baseline reconciliation retains history and prevents historical correction and recounting`() = runBlocking {
        val db = database()
        try {
            val dao = db.academicDao(); val ledger = AttendanceLedger(db)
            dao.insertTimetableEntry(entry)
            dao.insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 17, totalClasses = 20))
            ledger.mark(period, AttendanceChoice.PRESENT, now = now)
            ledger.correctTotals("CS", 19, 22, 85, now)
            assertTrue(dao.attendanceEvents().first().single().includedInBaseline)
            assertTrue(runCatching { ledger.mark(period, AttendanceChoice.UNMARKED, "PRESENT", now) }.isFailure)
            val older = attendanceOccurrence(entry, date - 7 * 86_400_000L)
            assertTrue(runCatching { ledger.mark(older, AttendanceChoice.PRESENT, now = now) }.isFailure)
            assertEquals(19, dao.attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(22, dao.attendanceByCourse("CS")!!.totalClasses)
            val next = attendanceOccurrence(entry, date + 7 * 86_400_000L)
            ledger.mark(next, AttendanceChoice.ABSENT, now = next.startMillis + 60_000)
            assertEquals(23, dao.attendanceByCourse("CS")!!.totalClasses)
        } finally { db.close() }
    }

    @Test fun `future missing timetable and stale correction attempts leave totals unchanged`() = runBlocking {
        val db = database()
        try {
            val dao = db.academicDao(); val ledger = AttendanceLedger(db)
            dao.insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 17, totalClasses = 20))
            assertTrue(runCatching { ledger.mark(period, AttendanceChoice.PRESENT, now = now) }.isFailure)
            dao.insertTimetableEntry(entry)
            assertTrue(runCatching { ledger.mark(period, AttendanceChoice.PRESENT, now = period.startMillis - 1) }.isFailure)
            ledger.mark(period, AttendanceChoice.PRESENT, now = now)
            assertTrue(runCatching { ledger.mark(period, AttendanceChoice.ABSENT, now = now) }.isFailure)
            assertEquals(18, dao.attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(21, dao.attendanceByCourse("CS")!!.totalClasses)
        } finally { db.close() }
    }

    @Test fun `ledger insertion failure rolls back its aggregate update`() = runBlocking {
        val db = database()
        try {
            val dao = db.academicDao()
            dao.insertTimetableEntry(entry)
            dao.insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 17, totalClasses = 20))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_mark BEFORE INSERT ON attendance_events BEGIN SELECT RAISE(ABORT, 'Storage rejected'); END")
            assertTrue(runCatching { AttendanceLedger(db).mark(period, AttendanceChoice.PRESENT, now = now) }.isFailure)
            assertEquals(17, dao.attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(20, dao.attendanceByCourse("CS")!!.totalClasses)
            assertTrue(dao.attendanceEvents().first().isEmpty())
        } finally { db.close() }
    }

    @Test fun `version three upgrade preserves records and Room validates ledger schema`() = runBlocking {
        val name = "attendance-v3-migration-test"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val fixture = javaClass.classLoader!!.getResourceAsStream("v2-schema.sql")!!.bufferedReader().use { it.readText() }
            fixture.split(';').filter { it.isNotBlank() }.forEach { old.execSQL(it) }
            old.execSQL("INSERT INTO attendance_records VALUES (7, 'CS', 17, 20, 85)")
            old.version = 2
        }
        val v3 = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        WORKSPACE_MIGRATION_2_3.migrate(db)
                    }
                }).build())
        v3.writableDatabase
        v3.close()
        val db = Room.databaseBuilder(context, LifeOsDatabase::class.java, name).addMigrations(ATTENDANCE_MIGRATION_3_4).build()
        try {
            assertEquals(17, db.academicDao().attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(85, db.academicDao().attendanceByCourse("CS")!!.targetPercentage)
            assertEquals(0L, db.academicDao().attendanceByCourse("CS")!!.baselineAt)
            assertTrue(db.academicDao().attendanceEvents().first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun `a saved mark still deduplicates after database close and reopen`() = runBlocking {
        val name = "attendance-persistence-test"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, LifeOsDatabase::class.java, name).build()
        try {
            db.academicDao().insertTimetableEntry(entry)
            db.academicDao().insertAttendance(AttendanceRecord(courseCode = "CS", attendedClasses = 17, totalClasses = 20))
            AttendanceLedger(db).mark(period, AttendanceChoice.PRESENT, now = now)
            db.close()
            db = Room.databaseBuilder(context, LifeOsDatabase::class.java, name).build()
            assertFalse(AttendanceLedger(db).mark(period, AttendanceChoice.PRESENT, now = now).changed)
            assertEquals(18, db.academicDao().attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(21, db.academicDao().attendanceByCourse("CS")!!.totalClasses)
            AttendanceLedger(db).mark(period, AttendanceChoice.ABSENT, "PRESENT", now)
            assertEquals(17, db.academicDao().attendanceByCourse("CS")!!.attendedClasses)
            assertEquals(21, db.academicDao().attendanceByCourse("CS")!!.totalClasses)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
