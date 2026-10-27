package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun taskById(id: Long): TaskItem?
    @Query("SELECT * FROM tasks WHERE status != 'DROPPED'")
    suspend fun tasksSnapshot(): List<TaskItem>
    @Query("SELECT * FROM tasks WHERE status != 'DROPPED' ORDER BY isTopThree DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isTopThree = 1 AND status = 'PENDING' LIMIT 3")
    fun getTopThreeTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE quadrant = :quadrant AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getTasksByQuadrant(quadrant: Quadrant): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING'")
    fun getPendingTasks(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus)

    @Query("UPDATE tasks SET isTopThree = :isTopThree WHERE id = :id")
    suspend fun updateTopThree(id: Long, isTopThree: Boolean)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitItem): Long

    @Update
    suspend fun updateHabit(habit: HabitItem)

    @Delete
    suspend fun deleteHabit(habit: HabitItem)

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()

    @Query("UPDATE habits SET isCompletedToday = :completed WHERE id = :id")
    suspend fun setCompletedToday(id: Long, completed: Boolean)
}

@Dao
interface AcademicDao {
    @Query("SELECT * FROM attendance_records WHERE courseCode = :courseCode ORDER BY id LIMIT 1")
    suspend fun attendanceByCourse(courseCode: String): AttendanceRecord?
    @Query("SELECT * FROM attendance_events ORDER BY startMillis DESC")
    fun attendanceEvents(): Flow<List<AttendanceEvent>>
    @Query("SELECT * FROM attendance_events WHERE `key` = :key")
    suspend fun attendanceEvent(key: String): AttendanceEvent?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttendanceEvent(event: AttendanceEvent)
    @Query("UPDATE attendance_events SET includedInBaseline = 1 WHERE courseCode = :courseCode")
    suspend fun absorbAttendanceEvents(courseCode: String)
    @Query("DELETE FROM attendance_events")
    suspend fun clearAttendanceEvents()
    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun assignmentById(id: Long): AssignmentItem?
    @Query("SELECT * FROM assignments ORDER BY dueDateMillis")
    suspend fun assignmentsSnapshot(): List<AssignmentItem>
    @Query("SELECT * FROM timetable_entries")
    suspend fun timetableSnapshot(): List<TimetableEntry>
    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllTimetable(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek ORDER BY startHour ASC, startMinute ASC")
    fun getTimetableForDay(dayOfWeek: Int): Flow<List<TimetableEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntry(entry: TimetableEntry): Long

    @Delete
    suspend fun deleteTimetableEntry(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries")
    suspend fun clearTimetable()

    @Query("SELECT * FROM assignments ORDER BY dueDateMillis ASC")
    fun getAllAssignments(): Flow<List<AssignmentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentItem): Long

    @Update
    suspend fun updateAssignment(assignment: AssignmentItem)

    @Delete
    suspend fun deleteAssignment(assignment: AssignmentItem)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignmentById(id: Long)

    @Query("DELETE FROM assignments")
    suspend fun clearAssignments()

    @Query("SELECT * FROM attendance_records ORDER BY courseCode ASC, id ASC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAttendance()
}

@Dao
interface FitnessDao {
    @Query("SELECT * FROM fitness_logs ORDER BY dateMillis DESC")
    fun getWorkoutSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSession)

    @Query("SELECT * FROM exercise_sets WHERE splitCategory = :split ORDER BY setNumber ASC")
    fun getSetsForSplit(split: String): Flow<List<ExerciseSetLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetLog): Long

    @Update
    suspend fun updateSet(set: ExerciseSetLog)

    @Query("DELETE FROM exercise_sets WHERE splitCategory = :split")
    suspend fun clearSetsForSplit(split: String)

    @Query("DELETE FROM exercise_sets")
    suspend fun clearAllSets()

    @Query("SELECT * FROM body_weight_logs ORDER BY timestamp DESC")
    fun getWeightLogs(): Flow<List<BodyWeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: BodyWeightLog): Long

    @Query("DELETE FROM body_weight_logs")
    suspend fun clearWeightLogs()

    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getWaterLogs(): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog): Long

    @Query("DELETE FROM water_logs")
    suspend fun clearWaterLogs()
}

@Dao
interface DailyResetDao {
    @Query("SELECT * FROM daily_resets ORDER BY completedAt DESC")
    fun getAllResets(): Flow<List<DailyResetRecord>>

    @Query("SELECT * FROM daily_resets WHERE dateString = :dateString LIMIT 1")
    suspend fun getResetForDate(dateString: String): DailyResetRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReset(reset: DailyResetRecord): Long
}

@Database(
    entities = [
        TaskItem::class,
        HabitItem::class,
        TimetableEntry::class,
        AssignmentItem::class,
        AttendanceRecord::class, AttendanceEvent::class,
        WorkoutSession::class,
        ExerciseSetLog::class,
        BodyWeightLog::class,
        WaterLog::class,
        DailyResetRecord::class,
        DayBlock::class, WorkSessionRecord::class, WorkProject::class, ProjectStep::class,
        CaptureNote::class, ExamTopic::class, CodingAttempt::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun academicDao(): AcademicDao
    abstract fun fitnessDao(): FitnessDao
    abstract fun dailyResetDao(): DailyResetDao
}
