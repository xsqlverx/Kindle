package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration. Never wipe existing records to resolve a schema mismatch. */
val WORKSPACE_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS day_blocks (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, startMillis INTEGER NOT NULL, durationMinutes INTEGER NOT NULL, fixed INTEGER NOT NULL, sourceType TEXT NOT NULL, sourceId TEXT NOT NULL, material TEXT NOT NULL, firstStep TEXT NOT NULL, state TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS work_sessions (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, sourceType TEXT NOT NULL, sourceId TEXT NOT NULL, blockId TEXT NOT NULL, material TEXT NOT NULL, resumeNote TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, elapsedSeconds INTEGER NOT NULL, outcome TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS work_projects (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, goal TEXT NOT NULL, blocker TEXT NOT NULL, archived INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS project_steps (id TEXT NOT NULL PRIMARY KEY, projectId TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, done INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS capture_notes (id TEXT NOT NULL PRIMARY KEY, text TEXT NOT NULL, createdAt INTEGER NOT NULL, taskId INTEGER NOT NULL, archived INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS exam_topics (id TEXT NOT NULL PRIMARY KEY, course TEXT NOT NULL, title TEXT NOT NULL, material TEXT NOT NULL, confidence INTEGER NOT NULL, revisionAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS coding_attempts (id TEXT NOT NULL PRIMARY KEY, problem TEXT NOT NULL, link TEXT NOT NULL, difficulty TEXT NOT NULL, outcome TEXT NOT NULL, notes TEXT NOT NULL, retryAt INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

val ATTENDANCE_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN baselineAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS attendance_events (`key` TEXT NOT NULL PRIMARY KEY, courseCode TEXT NOT NULL, date TEXT NOT NULL, startMillis INTEGER NOT NULL, startMinute INTEGER NOT NULL, endMinute INTEGER NOT NULL, status TEXT NOT NULL, updatedAt INTEGER NOT NULL, includedInBaseline INTEGER NOT NULL)")
    }
}

object DatabaseProvider {
    @Volatile private var instance: LifeOsDatabase? = null
    fun get(context: Context): LifeOsDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context.applicationContext, LifeOsDatabase::class.java, "life_os_database_v6")
            .addMigrations(WORKSPACE_MIGRATION_2_3, ATTENDANCE_MIGRATION_3_4).build().also { instance = it }
    }
}
