package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cloud runner only. No Android tests were executed locally. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkspaceDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun `migration preserves legacy records and Room validates the resulting schema`() = runBlocking {
        val name = "migration-workspace-test"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val fixture = javaClass.classLoader!!.getResourceAsStream("v2-schema.sql")!!.bufferedReader().use { it.readText() }
            fixture.split(';').filter { it.isNotBlank() }.forEach { old.execSQL(it) }
            old.execSQL("INSERT INTO tasks VALUES (42, 'User task', 'Q1_DO', 'PENDING', 'College', '', 1, 123)")
            old.execSQL("INSERT INTO attendance_records VALUES (7, 'CS', 17, 20, 80)")
            old.execSQL("INSERT INTO body_weight_logs VALUES (3, 61.2, 'User supplied', 123)")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, LifeOsDatabase::class.java, name).addMigrations(WORKSPACE_MIGRATION_2_3).build()
        try {
            assertEquals("User task", db.taskDao().getAllTasks().first().single().title)
            assertEquals(17, db.academicDao().getAllAttendance().first().single().attendedClasses)
            assertEquals(61.2f, db.fitnessDao().getWeightLogs().first().single().weightKg)
            assertTrue(db.workspaceDao().sessions().first().isEmpty())
            db.workspaceDao().saveBlock(DayBlock("block", "Real next step", 100, 25))
            assertEquals(1, db.workspaceDao().blocks().first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun `capture conversion is idempotent and session saving never completes its task`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java).build()
        try {
            val dao = db.workspaceDao()
            val note = CaptureNote("note", "Understand recursion", 100)
            dao.saveCapture(note)
            val firstId = dao.captureToTask(note, db.taskDao())
            assertEquals(firstId, dao.captureToTask(note, db.taskDao()))
            assertEquals(1, db.taskDao().getAllTasks().first().size)
            dao.saveBlock(DayBlock("block", note.text, 100, 25, sourceType = "TASK", sourceId = firstId.toString()))
            val record = WorkSessionRecord("session", note.text, "TASK", firstId.toString(), "block", "Notes p4", "Resume at example 2", 100, 200, 60, "Stopped early")
            dao.finish(record)
            dao.finish(record.copy(resumeNote = "Revised resume point", elapsedSeconds = 999))
            assertEquals(1, dao.sessions().first().size)
            assertEquals(60, dao.sessions().first().single().elapsedSeconds)
            assertEquals("Revised resume point", dao.sessions().first().single().resumeNote)
            assertEquals("REVIEWED", dao.blocks().first().single().state)
            assertEquals(TaskStatus.PENDING, db.taskDao().getAllTasks().first().single().status)
        } finally { db.close() }
    }

    @Test fun `explicit completion frees planned work without rewriting a saved session`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java).build()
        try {
            val dao = db.workspaceDao()
            dao.saveBlock(DayBlock("planned", "Next pass", 100, 25, sourceType = "TASK", sourceId = "7"))
            dao.saveBlock(DayBlock("reviewed", "First pass", 0, 25, sourceType = "TASK", sourceId = "7", state = "REVIEWED"))
            dao.sourcePlanState("TASK", "7", "DONE")
            val done = dao.blocksSnapshot().associateBy { it.id }
            assertEquals("DONE", done.getValue("planned").state)
            assertEquals("REVIEWED", done.getValue("reviewed").state)
            dao.sourcePlanState("TASK", "7", "PLANNED")
            assertEquals("PLANNED", dao.blockById("planned")!!.state)
            assertEquals("REVIEWED", dao.blockById("reviewed")!!.state)
        } finally { db.close() }
    }
}
