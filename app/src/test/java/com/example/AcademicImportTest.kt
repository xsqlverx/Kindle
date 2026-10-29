package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LifeOsDatabase
import com.example.data.model.*
import com.example.data.repository.AcademicStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** External cloud runner only. Uses the actual bundled source catalog and Room. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AcademicImportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun database() = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java).build()
    private fun resetPrefs() { context.getSharedPreferences("academic_s1_2026", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun `original source pack has six courses not seven and exact college date lists`() {
        val db = database()
        try {
            val pack = AcademicStore(context, db).load()
            assertEquals(6, pack.courses.size)
            assertEquals(7, pack.sources.size)
            assertEquals(140, pack.items.size)
            assertEquals("A105EST04", pack.courses.single { it.id == "electrical" }.code)
            assertEquals(listOf("2026-11-13", "2026-11-16", "2026-11-17", "2026-11-18", "2026-11-19"), pack.events.single { it.id == "s1-internal2" }.dates)
            assertEquals(listOf("2026-12-07", "2026-12-09", "2026-12-11", "2026-12-14", "2026-12-16"), pack.events.single { it.id == "s1-final" }.dates)
            pack.sources.forEach { source ->
                val bytes = context.assets.open(source.asset).use { it.readBytes() }
                val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
                assertEquals(source.sha256, hash)
            }
        } finally { db.close() }
    }
    @Test fun `repeated and forced imports preserve user edits outcomes attendance and actual blocks`() = runBlocking {
        resetPrefs(); val db = database()
        try {
            val store = AcademicStore(context, db); val pack = store.load(); val dao = db.workspaceDao()
            store.install(pack)
            assertTrue(dao.topicsSnapshot().all { it.confidence == 0 && it.revisionAt == 0L })
            assertTrue(dao.attemptsSnapshot().all { it.outcome == "Unattempted" && it.notes.isBlank() && it.createdAt == 0L })
            val topic = dao.topicsSnapshot().first().copy(title = "My edited label", material = "My notebook", confidence = 3, revisionAt = 123)
            val attempt = dao.attemptsSnapshot().first().copy(outcome = "Solved", notes = "My explanation", retryAt = 456)
            dao.saveTopic(topic); dao.saveAttempt(attempt)
            val block = DayBlock("real", "Real commitment", 100, 37, fixed = true)
            dao.saveBlock(block)
            db.openHelper.writableDatabase.execSQL("INSERT INTO attendance_records (id, courseCode, attendedClasses, totalClasses, targetPercentage, baselineAt) VALUES (77, 'User course', 17, 20, 80, 0)")
            store.install(pack); store.install(pack, force = true)
            assertEquals(topic, dao.topicById(topic.id))
            assertEquals(attempt, dao.attemptById(attempt.id))
            assertEquals(122, dao.topicsSnapshot().size)
            assertEquals(18, dao.attemptsSnapshot().size)
            assertEquals(block, dao.blockById(block.id))
            db.openHelper.readableDatabase.query("SELECT attendedClasses, totalClasses FROM attendance_records WHERE id = 77").use {
                assertTrue(it.moveToFirst()); assertEquals(17, it.getInt(0)); assertEquals(20, it.getInt(1))
            }
        } finally { db.close(); resetPrefs() }
    }
    @Test fun `roadmap roundtrip preserves explicit progress and user wipe prevents automatic reimport`() = runBlocking {
        resetPrefs(); val db = database()
        try {
            val store = AcademicStore(context, db); val pack = store.load()
            store.install(pack)
            val targets = SyllabusPlanner.build(pack, "2026-10-30", 37)
            val roadmap = AcademicRoadmap(targets, 37, setOf(pack.items.first().id), setOf(targets.first().id), setOf("mathematics:m1"))
            store.saveRoadmap(roadmap)
            assertEquals(roadmap, AcademicStore(context, db).readRoadmap())
            store.clearRoadmap(); db.workspaceDao().clearWorkspace()
            val reopened = AcademicStore(context, db)
            reopened.install(pack)
            assertTrue(db.workspaceDao().topicsSnapshot().isEmpty())
            assertTrue(db.workspaceDao().attemptsSnapshot().isEmpty())
            assertEquals(AcademicRoadmap(), reopened.readRoadmap())
            reopened.addItem(pack, pack.items.first()) // explicit work selection restores only its own identity
            assertEquals(1, db.workspaceDao().topicsSnapshot().size)
        } finally { db.close(); resetPrefs() }
    }
    @Test fun `a wipe before the first import also stays empty on restart`() = runBlocking {
        resetPrefs(); val db = database()
        try {
            val store = AcademicStore(context, db)
            store.clearRoadmap()
            store.install(store.load())
            assertTrue(db.workspaceDao().topicsSnapshot().isEmpty())
            assertTrue(db.workspaceDao().attemptsSnapshot().isEmpty())
        } finally { db.close(); resetPrefs() }
    }
}
