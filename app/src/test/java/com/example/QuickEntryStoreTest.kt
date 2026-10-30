package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.ProjectQueueStore
import com.example.data.repository.QuickEntryStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Supplied for the external cloud runner; never executed locally. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickEntryStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun db() = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java).build()
    private fun reset() { listOf("quick_entry", "github_project_queue").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() } }
    private fun draft(id: String, kind: EntryKind, title: String) = EntryDraft(id, kind).edit("title", title)

    @Test fun `concurrent retries create one task and never overwrite later user edits`() = runBlocking {
        reset(); val db = db()
        try {
            val store = QuickEntryStore(context, db)
            val input = draft("once", EntryKind.TASK, "My actual task")
            val results = (1..30).map { async(Dispatchers.IO) { store.save(input, 123) } }.awaitAll()
            assertEquals(1, results.map { it.receipt.recordId }.distinct().size)
            val task = db.taskDao().tasksSnapshot().single()
            assertEquals("My actual task", task.title)
            assertEquals(Quadrant.Q2_SCHEDULE, task.quadrant)
            db.taskDao().updateTask(task.copy(title = "Edited later", status = TaskStatus.COMPLETED))
            store.save(input.edit("title", "A stale draft"), 456)
            assertEquals("Edited later", db.taskDao().taskById(task.id)!!.title)
            assertEquals(TaskStatus.COMPLETED, db.taskDao().taskById(task.id)!!.status)
            assertEquals(1, db.taskDao().tasksSnapshot().size)
        } finally { db.close(); reset() }
    }
    @Test fun `project creation is one transaction and retries preserve completion and custom edits`() = runBlocking {
        reset(); val db = db()
        try {
            val store = QuickEntryStore(context, db)
            val input = draft("project-1", EntryKind.PROJECT, "https://github.com/example/my-app")
                .edit("goal", "Make a prototype").edit("steps", "First screen\n- Working path\nFirst screen")
            val result = store.save(input, 1)
            val project = db.workspaceDao().projectsSnapshot().single()
            assertEquals("my-app", project.title)
            assertTrue(project.goal.contains("https://github.com/example/my-app"))
            assertEquals(listOf("First screen", "Working path", "First screen"), db.workspaceDao().stepsSnapshot().sortedBy { it.position }.map { it.title })
            val step = db.workspaceDao().stepsSnapshot().first()
            db.workspaceDao().saveStep(step.copy(title = "My new action", done = true))
            store.save(input.edit("steps", "Other action"), 2)
            assertEquals(result.receipt.recordId, project.id)
            assertEquals("My new action", db.workspaceDao().stepById(step.id)!!.title)
            assertTrue(db.workspaceDao().stepById(step.id)!!.done)
            assertEquals(3, db.workspaceDao().stepsSnapshot().size)
            store.save(draft("local-project", EntryKind.PROJECT, "Physics/Math"), 3)
            val localProject = db.workspaceDao().projectsSnapshot().first { it.id == "entry:local-project" }
            assertEquals("Physics/Math", localProject.title)
            assertEquals("", localProject.goal)
        } finally { db.close(); reset() }
    }
    @Test fun `logging and planned sets remain distinct and duplicate retries do not double count`() = runBlocking {
        reset(); val db = db()
        try {
            val store = QuickEntryStore(context, db)
            val water = draft("water", EntryKind.WATER, "250")
            store.save(water, 123); store.save(water, 456)
            assertEquals(listOf(250), db.fitnessDao().getWaterLogs().first().map { it.amountMl })
            val weight = draft("weight", EntryKind.WEIGHT, "64.5")
            store.save(weight, 123)
            assertEquals(64.5f, db.fitnessDao().getWeightLogs().first().single().weightKg, 0.001f)
            val set = draft("set-1", EntryKind.SET, "Push up").edit("reps", "8").edit("kg", "0").edit("split", "Push")
            store.save(set, 123); store.save(set, 456)
            store.save(set.copy(id = "set-2"), 789)
            val sets = db.fitnessDao().getSetsForSplit("Push").first()
            assertEquals(listOf(1, 2), sets.map { it.setNumber })
            assertTrue(sets.all { !it.isCompleted && it.weightKg == 0f })
            val training = draft("training", EntryKind.TRAINING, "Upper").edit("minutes", "37").edit("note", "Finish rows next time")
            store.save(training, 2_220_000)
            val session = db.workspaceDao().sessions().first().single()
            assertEquals("Training · self reported", session.outcome)
            assertEquals(37 * 60, session.elapsedSeconds)
            assertEquals("Finish rows next time", session.resumeNote)
        } finally { db.close(); reset() }
    }
    @Test fun `invalid drafts do not mutate storage and exact class dates and times survive`() = runBlocking {
        reset(); val db = db()
        try {
            val store = QuickEntryStore(context, db)
            val bad = draft("bad", EntryKind.CLASS, "Python").edit("course", "PY").edit("day", "2").edit("start", "10:00").edit("end", "09:00")
            assertTrue(runCatching { store.save(bad, 123) }.isFailure)
            assertTrue(db.academicDao().timetableSnapshot().isEmpty())
            val valid = bad.edit("end", "10:45").edit("room", "A12")
            store.save(valid, 123)
            val entry = db.academicDao().timetableSnapshot().single()
            assertEquals(2, entry.dayOfWeek); assertEquals(10, entry.startHour); assertEquals(45, entry.endMinute); assertEquals("A12", entry.room)
            val deadline = draft("deadline", EntryKind.DEADLINE, "Lab record").edit("date", "2026-11-15")
            store.save(deadline, 123)
            val assignment = db.academicDao().assignmentsSnapshot().single()
            assertEquals(AcademicDates.millis("2026-11-15"), assignment.dueDateMillis)
            assertEquals(0, assignment.weightPercentage)
            assertEquals("", assignment.courseCode)
        } finally { db.close(); reset() }
    }
    @Test fun `draft recreation and save preserve other kinds of unfinished entry`() {
        reset(); val db = db()
        try {
            val store = QuickEntryStore(context, db)
            val task = draft("draft-id", EntryKind.TASK, "Task draft").edit("due", "After college")
            val both = task.copy(kind = EntryKind.PROJECT).edit("title", "Project draft").edit("steps", "First step")
            store.draft(both)
            assertEquals(both, QuickEntryStore(context, db).readDraft())
            val afterProjectSave = store.afterSave(both)
            assertNotEquals(both.id, afterProjectSave.id)
            assertEquals("", afterProjectSave.get("title"))
            assertEquals("Task draft", afterProjectSave.copy(kind = EntryKind.TASK).get("title"))
            assertEquals(afterProjectSave, QuickEntryStore(context, db).readDraft())
        } finally { db.close(); reset() }
    }
    @Test fun `suggested queue installs once without replacing edits and stays absent after wipe`() = runBlocking {
        reset(); val db = db()
        try {
            val queue = ProjectQueueStore(context, db); val items = queue.load()
            assertEquals(4, items.size)
            val ownProject = WorkProject("mine", "My AURA work", "https://github.com/xsqlverx/AURA-V2")
            db.workspaceDao().saveProject(ownProject)
            queue.install(items)
            assertFalse(db.workspaceDao().projectsSnapshot().any { it.id == "github:xsqlverx:AURA-V2" })
            assertEquals(ownProject, db.workspaceDao().projectsSnapshot().first { it.id == "mine" })
            val project = db.workspaceDao().projectsSnapshot().first { it.id.startsWith("github:xsqlverx:") }
            val step = db.workspaceDao().stepsSnapshot().first { it.projectId == project.id }
            db.workspaceDao().saveProject(project.copy(goal = "My edited goal", archived = true))
            db.workspaceDao().saveStep(step.copy(title = "My chosen next step", done = true))
            queue.install(items); queue.install(items, force = true)
            assertEquals("My edited goal", db.workspaceDao().projectsSnapshot().first { it.id == project.id }.goal)
            assertTrue(db.workspaceDao().projectsSnapshot().first { it.id == project.id }.archived)
            assertEquals("My chosen next step", db.workspaceDao().stepById(step.id)!!.title)
            assertTrue(db.workspaceDao().stepById(step.id)!!.done)
            db.workspaceDao().saveEntryReceipt(EntryReceipt("kept", "TASK", "42"))
            queue.retainWipeIntent(); db.workspaceDao().clearWorkspace()
            ProjectQueueStore(context, db).install(items)
            assertTrue(db.workspaceDao().projectsSnapshot().isEmpty())
            assertTrue(db.workspaceDao().stepsSnapshot().isEmpty())
            assertNull(db.workspaceDao().entryReceipt("kept"))
        } finally { db.close(); reset() }
    }
    @Test fun `a wipe before first queue installation prevents unsolicited restoration`() = runBlocking {
        reset(); val db = db()
        try {
            val queue = ProjectQueueStore(context, db)
            queue.retainWipeIntent(); queue.install(queue.load())
            assertTrue(db.workspaceDao().projectsSnapshot().isEmpty())
        } finally { db.close(); reset() }
    }
    @Test fun `version four upgrade keeps user projects sessions and attendance while adding receipts`() = runBlocking {
        val name = "entry-v4-migration-test"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val fixture = javaClass.classLoader!!.getResourceAsStream("v2-schema.sql")!!.bufferedReader().use { it.readText() }
            fixture.split(';').filter { it.isNotBlank() }.forEach { old.execSQL(it) }
            old.execSQL("INSERT INTO attendance_records VALUES (7, 'CS', 17, 20, 80)")
            old.version = 2
        }
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        WORKSPACE_MIGRATION_2_3.migrate(db); ATTENDANCE_MIGRATION_3_4.migrate(db)
                    }
                }).build())
        helper.writableDatabase.execSQL("INSERT INTO work_projects VALUES ('p', 'My project', 'My goal', '', 0)")
        helper.writableDatabase.execSQL("INSERT INTO project_steps VALUES ('s', 'p', 'Actual step', 0, 1)")
        helper.writableDatabase.execSQL("INSERT INTO work_sessions VALUES ('w', 'My work', 'PROJECT_STEP', 's', '', '', 'Resume here', 1, 2, 1, 'Saved')")
        helper.close()
        val db = Room.databaseBuilder(context, LifeOsDatabase::class.java, name).addMigrations(ENTRY_MIGRATION_4_5).build()
        try {
            assertEquals("My goal", db.workspaceDao().projectsSnapshot().single().goal)
            assertTrue(db.workspaceDao().stepById("s")!!.done)
            assertEquals("Resume here", db.workspaceDao().sessions().first().single().resumeNote)
            assertEquals(17, db.academicDao().attendanceByCourse("CS")!!.attendedClasses)
            assertNull(db.workspaceDao().entryReceipt("no-receipt"))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
