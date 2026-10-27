package com.example.data.repository

import android.content.Context
import com.example.data.local.DatabaseProvider
import androidx.room.withTransaction
import com.example.data.local.LifeOsDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LifeOsRepository(context: Context) {

    private val db: LifeOsDatabase = DatabaseProvider.get(context)

    private val taskDao = db.taskDao()
    private val habitDao = db.habitDao()
    private val academicDao = db.academicDao()
    private val fitnessDao = db.fitnessDao()
    private val dailyResetDao = db.dailyResetDao()

    private val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
    private val attendanceWrites = Mutex()

    fun getThemeMode(): AppThemeMode = AppThemeMode.entries.firstOrNull {
        it.name == prefs.getString("appearance_mode", "")
    } ?: AppThemeMode.NIGHT_GRAPHITE

    fun saveThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("appearance_mode", mode.name).apply()
    }

    fun getBodyWeightGoal(): Float = prefs.getFloat("body_weight_goal", 66.5f)

    fun saveBodyWeightGoal(weight: Float) {
        require(weight.isFinite() && weight > 0f)
        prefs.edit().putFloat("body_weight_goal", weight).apply()
    }

    suspend fun correctAttendance(courseCode: String, attended: Int, total: Int, target: Int) {
        require(attended >= 0 && total >= attended && target in 1..100)
        attendanceWrites.withLock {
            val record = academicDao.getAllAttendance().first().find { it.courseCode == courseCode } ?: return@withLock
            academicDao.updateAttendance(record.copy(attendedClasses = attended, totalClasses = total, targetPercentage = target))
        }
    }

    // V2 never inserts example records or deletes look-alike records at startup.
    // Preserve older stored data; its provenance cannot be inferred safely.

    // Tasks
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()
    val topThreeTasks: Flow<List<TaskItem>> = taskDao.getTopThreeTasks()
    fun getTasksByQuadrant(quadrant: Quadrant): Flow<List<TaskItem>> = taskDao.getTasksByQuadrant(quadrant)
    suspend fun getPendingTasks(): List<TaskItem> = taskDao.getPendingTasks().first()
    suspend fun insertTask(task: TaskItem): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskItem) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskItem) = db.withTransaction {
        taskDao.deleteTask(task)
        db.workspaceDao().sourcePlanState("TASK", task.id.toString(), "DROPPED")
    }
    suspend fun clearTasks() = taskDao.clearAllTasks()
    suspend fun setTaskStatus(id: Long, status: TaskStatus) = db.withTransaction {
        taskDao.updateStatus(id, status)
        db.workspaceDao().sourcePlanState("TASK", id.toString(), when (status) {
            TaskStatus.PENDING -> "PLANNED"
            TaskStatus.COMPLETED -> "DONE"
            TaskStatus.DROPPED -> "DROPPED"
            else -> "DEFERRED"
        })
    }
    suspend fun toggleTopThree(id: Long, isTopThree: Boolean) = taskDao.updateTopThree(id, isTopThree)

    // Habits
    val allHabits: Flow<List<HabitItem>> = habitDao.getAllHabits()
    suspend fun insertHabit(habit: HabitItem): Long = habitDao.insertHabit(habit)
    suspend fun toggleHabit(id: Long, currentCompleted: Boolean) = habitDao.setCompletedToday(id, !currentCompleted)
    suspend fun deleteHabit(habit: HabitItem) = habitDao.deleteHabit(habit)
    suspend fun clearHabits() = habitDao.clearAllHabits()

    // Academic Timetable & Attendance
    val allTimetable: Flow<List<TimetableEntry>> = academicDao.getAllTimetable()
    fun getTimetableForDay(day: Int): Flow<List<TimetableEntry>> = academicDao.getTimetableForDay(day)
    suspend fun insertTimetableEntry(entry: TimetableEntry): Long = academicDao.insertTimetableEntry(entry)
    suspend fun deleteTimetableEntry(entry: TimetableEntry) = academicDao.deleteTimetableEntry(entry)

    val allAssignments: Flow<List<AssignmentItem>> = academicDao.getAllAssignments()
    suspend fun insertAssignment(assignment: AssignmentItem) = academicDao.insertAssignment(assignment)
    suspend fun updateAssignment(assignment: AssignmentItem) = db.withTransaction {
        academicDao.updateAssignment(assignment)
        db.workspaceDao().sourcePlanState("ASSIGNMENT", assignment.id.toString(), if (assignment.isSubmitted) "DONE" else "PLANNED")
    }
    suspend fun deleteAssignment(assignment: AssignmentItem) = db.withTransaction {
        academicDao.deleteAssignment(assignment)
        db.workspaceDao().sourcePlanState("ASSIGNMENT", assignment.id.toString(), "DROPPED")
    }
    suspend fun toggleAssignmentSubmitted(assignment: AssignmentItem) {
        updateAssignment(assignment.copy(isSubmitted = !assignment.isSubmitted))
    }

    val allAttendance: Flow<List<AttendanceRecord>> = academicDao.getAllAttendance()
    suspend fun logAttendance(courseCode: String, attended: Boolean) {
        attendanceWrites.withLock {
            val records = academicDao.getAllAttendance().first()
            val record = records.find { it.courseCode == courseCode }
            if (record != null) {
                val newAttended = if (attended) record.attendedClasses + 1 else record.attendedClasses
                academicDao.updateAttendance(record.copy(attendedClasses = newAttended, totalClasses = record.totalClasses + 1))
            } else {
                academicDao.insertAttendance(
                    AttendanceRecord(
                        courseCode = courseCode,
                        attendedClasses = if (attended) 1 else 0,
                        totalClasses = 1,
                        targetPercentage = 80
                    )
                )
            }
        }
    }

    // Fitness
    val workoutSessions: Flow<List<WorkoutSession>> = fitnessDao.getWorkoutSessions()
    fun getSetsForSplit(split: String): Flow<List<ExerciseSetLog>> = fitnessDao.getSetsForSplit(split)
    suspend fun insertWorkoutSet(set: ExerciseSetLog) = fitnessDao.insertSet(set)
    suspend fun updateWorkoutSet(set: ExerciseSetLog) = fitnessDao.updateSet(set)

    val weightLogs: Flow<List<BodyWeightLog>> = fitnessDao.getWeightLogs()
    suspend fun insertWeightLog(weight: Float, note: String = "") {
        fitnessDao.insertWeightLog(BodyWeightLog(weightKg = weight, note = note))
    }

    val waterLogs: Flow<List<WaterLog>> = fitnessDao.getWaterLogs()
    suspend fun addWater(amountMl: Int) {
        fitnessDao.insertWaterLog(WaterLog(amountMl = amountMl))
    }
    suspend fun clearWater() = fitnessDao.clearWaterLogs()

    // Nutrition / Macros Persistence
    fun getSavedMacros(): MacroNutrients {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString("macro_saved_date", "")
        val targetCalories = prefs.getInt("target_calories", 2150)
        val targetProtein = prefs.getInt("target_protein", 140)
        val targetCarbs = prefs.getInt("target_carbs", 235)
        val targetFats = prefs.getInt("target_fats", 65)

        if (savedDate != today) {
            // New day: start today's current intake at 0, retain target goals
            return MacroNutrients(
                currentCalories = 0,
                targetCalories = targetCalories,
                currentProteinGrams = 0,
                targetProteinGrams = targetProtein,
                currentCarbsGrams = 0,
                targetCarbsGrams = targetCarbs,
                currentFatsGrams = 0,
                targetFatsGrams = targetFats
            )
        }
        return MacroNutrients(
            currentCalories = prefs.getInt("current_calories", 0),
            targetCalories = targetCalories,
            currentProteinGrams = prefs.getInt("current_protein", 0),
            targetProteinGrams = targetProtein,
            currentCarbsGrams = prefs.getInt("current_carbs", 0),
            targetCarbsGrams = targetCarbs,
            currentFatsGrams = prefs.getInt("current_fats", 0),
            targetFatsGrams = targetFats
        )
    }

    fun saveMacros(macros: MacroNutrients) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putString("macro_saved_date", today)
            .putInt("current_calories", macros.currentCalories)
            .putInt("target_calories", macros.targetCalories)
            .putInt("current_protein", macros.currentProteinGrams)
            .putInt("target_protein", macros.targetProteinGrams)
            .putInt("current_carbs", macros.currentCarbsGrams)
            .putInt("target_carbs", macros.targetCarbsGrams)
            .putInt("current_fats", macros.currentFatsGrams)
            .putInt("target_fats", macros.targetFatsGrams)
            .apply()
    }

    fun clearMacros() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putString("macro_saved_date", today)
            .putInt("current_calories", 0)
            .putInt("current_protein", 0)
            .putInt("current_carbs", 0)
            .putInt("current_fats", 0)
            .apply()
    }

    // Target Exam Configuration
    fun getTargetExam(): ExamTarget {
        val title = prefs.getString("target_exam_title", "") ?: ""
        val dateMillis = prefs.getLong("target_exam_date_millis", 0L)
        return ExamTarget(title = title, dateMillis = dateMillis)
    }

    fun saveTargetExam(title: String, dateMillis: Long) {
        prefs.edit()
            .putString("target_exam_title", title)
            .putLong("target_exam_date_millis", dateMillis)
            .apply()
    }

    fun clearTargetExam() {
        prefs.edit()
            .remove("target_exam_title")
            .remove("target_exam_date_millis")
            .apply()
    }

    // Daily Reset
    val allDailyResets: Flow<List<DailyResetRecord>> = dailyResetDao.getAllResets()
    suspend fun saveDailyReset(reset: DailyResetRecord): Long = dailyResetDao.insertReset(reset)

    suspend fun resetAllMockData() {
        prefs.edit().putBoolean("user_clean_slate", true).apply()
        taskDao.clearAllTasks()
        habitDao.clearAllHabits()
        academicDao.clearAttendance()
        academicDao.clearAssignments()
        fitnessDao.clearAllSets()
        fitnessDao.clearWeightLogs()
        fitnessDao.clearWaterLogs()
        clearMacros()
        resetAttendanceData()
    }

    suspend fun resetAttendanceData() {
        academicDao.clearAttendance()
        val timetable = academicDao.getAllTimetable().first()
        val courses = timetable.map { it.courseCode }.distinct().ifEmpty {
            listOf("MAT", "ATP", "ATP LAB", "CHY", "BEE", "BEC", "EG", "LS", "CHY LAB/EE-EC W/S")
        }
        courses.forEach { code ->
            academicDao.insertAttendance(
                AttendanceRecord(
                    courseCode = code,
                    attendedClasses = 0,
                    totalClasses = 0,
                    targetPercentage = 80
                )
            )
        }
    }

    suspend fun importTimetable(entries: List<TimetableEntry>) {
        academicDao.clearTimetable()
        entries.forEach { academicDao.insertTimetableEntry(it) }
    }

    suspend fun importWorkoutSets(sets: List<ExerciseSetLog>) {
        fitnessDao.clearAllSets()
        sets.forEach { fitnessDao.insertSet(it) }
    }

    private suspend fun seedDefaultDataIfEmpty() {
        if (prefs.getBoolean("user_clean_slate", false)) {
            return
        }
        val habits = habitDao.getAllHabits().first()
        if (habits.isEmpty()) {
            habitDao.insertHabit(HabitItem(title = "1 Hour Workout (PPL Winter Arc)", currentStreak = 14, isCompletedToday = true, category = "Fitness"))
            habitDao.insertHabit(HabitItem(title = "2 Hour Deep Study (S1 Prep)", currentStreak = 9, isCompletedToday = false, category = "Academics"))
            habitDao.insertHabit(HabitItem(title = "8 Hour Restorative Sleep", currentStreak = 7, isCompletedToday = true, category = "Recovery"))
        }

        val tasks = taskDao.getAllTasks().first()
        if (tasks.isEmpty()) {
            taskDao.insertTask(TaskItem(title = "Algorithmic Thinking Python Lab 4 review", quadrant = Quadrant.Q1_DO, category = "College", dueTime = "Today", isTopThree = true))
            taskDao.insertTask(TaskItem(title = "Winter Arc Push Day: Flat DB & Incline DB Press", quadrant = Quadrant.Q1_DO, category = "Gym", dueTime = "5:30 PM", isTopThree = true))
            taskDao.insertTask(TaskItem(title = "Prepare for S1 Midterm Exam (Target Oct 1)", quadrant = Quadrant.Q2_SCHEDULE, category = "College", dueTime = "Oct 1", isTopThree = true))
            taskDao.insertTask(TaskItem(title = "Solve Engineering Graphics CAD projection sheet", quadrant = Quadrant.Q2_SCHEDULE, category = "College", dueTime = "Friday", isTopThree = false))
            taskDao.insertTask(TaskItem(title = "Track daily macros: 2,150 kcal & 140g protein", quadrant = Quadrant.Q2_SCHEDULE, category = "Health", dueTime = "Daily", isTopThree = false))
        }

        val timetable = academicDao.getAllTimetable().first()
        if (timetable.isEmpty()) {
            // Monday
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "BEE", courseName = "Introduction to Electrical and Electronics Engineering", dayOfWeek = Calendar.MONDAY, startHour = 8, startMinute = 0, endHour = 8, endMinute = 45, room = "A213", professor = "Dr. Linss T Alex (LTA)", colorHex = 0xFF6366F1))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "CHY", courseName = "Chemistry for Information & Electrical Science", dayOfWeek = Calendar.MONDAY, startHour = 9, startMinute = 10, endHour = 9, endMinute = 55, room = "A213", professor = "Dr. Jude Martin Mendez (JMM)", colorHex = 0xFF06B6D4))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "MAT", courseName = "Mathematics for Information Science - I", dayOfWeek = Calendar.MONDAY, startHour = 10, startMinute = 0, endHour = 10, endMinute = 45, room = "A213", professor = "Prof. Shereeta D'Cotha (SDC)", colorHex = 0xFF8B5CF6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ATP LAB", courseName = "Algorithmic Thinking with Python - Lab", dayOfWeek = Calendar.MONDAY, startHour = 10, startMinute = 50, endHour = 12, endMinute = 40, room = "Python Lab / A213", professor = "Prof. Sharija P M (SPM, NF)", colorHex = 0xFF10B981))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "BEC", courseName = "Introduction to Electrical and Electronics Engineering", dayOfWeek = Calendar.MONDAY, startHour = 12, startMinute = 45, endHour = 13, endMinute = 30, room = "A213", professor = "Prof. SKV", colorHex = 0xFFF59E0B))

            // Tuesday
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "BEC", courseName = "Introduction to Electrical and Electronics Engineering", dayOfWeek = Calendar.TUESDAY, startHour = 8, startMinute = 0, endHour = 8, endMinute = 45, room = "A213", professor = "Prof. SKV", colorHex = 0xFFF59E0B))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "LS", courseName = "Life Skills and Professional Communication", dayOfWeek = Calendar.TUESDAY, startHour = 9, startMinute = 10, endHour = 9, endMinute = 55, room = "A213", professor = "Prof. DMR", colorHex = 0xFFEC4899))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "CHY", courseName = "Chemistry for Information & Electrical Science", dayOfWeek = Calendar.TUESDAY, startHour = 10, startMinute = 0, endHour = 10, endMinute = 45, room = "A213", professor = "Dr. Jude Martin Mendez (JMM)", colorHex = 0xFF06B6D4))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "CHY LAB/EE-EC W/S", courseName = "Chemistry Lab / EE-EC Workshop", dayOfWeek = Calendar.TUESDAY, startHour = 10, startMinute = 50, endHour = 12, endMinute = 40, room = "Chem Lab / Workshop", professor = "Faculty J/L", colorHex = 0xFF3B82F6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "MAT", courseName = "Mathematics for Information Science - I", dayOfWeek = Calendar.TUESDAY, startHour = 12, startMinute = 45, endHour = 13, endMinute = 30, room = "A213", professor = "Prof. Shereeta D'Cotha (SDC)", colorHex = 0xFF8B5CF6))

            // Wednesday
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "MAT", courseName = "Mathematics for Information Science - I", dayOfWeek = Calendar.WEDNESDAY, startHour = 8, startMinute = 0, endHour = 8, endMinute = 45, room = "A213", professor = "Prof. Shereeta D'Cotha (SDC)", colorHex = 0xFF8B5CF6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "BEE", courseName = "Introduction to Electrical and Electronics Engineering", dayOfWeek = Calendar.WEDNESDAY, startHour = 9, startMinute = 10, endHour = 9, endMinute = 55, room = "A213", professor = "Dr. Linss T Alex (LTA)", colorHex = 0xFF6366F1))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ATP", courseName = "Algorithmic Thinking with Python", dayOfWeek = Calendar.WEDNESDAY, startHour = 10, startMinute = 0, endHour = 10, endMinute = 45, room = "A213", professor = "Prof. Sharija P M (SPM)", colorHex = 0xFF10B981))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "CHY LAB/EE-EC W/S", courseName = "Chemistry Lab / EE-EC Workshop", dayOfWeek = Calendar.WEDNESDAY, startHour = 10, startMinute = 50, endHour = 12, endMinute = 40, room = "Chem Lab / Workshop", professor = "Faculty J/L", colorHex = 0xFF3B82F6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ATP", courseName = "Algorithmic Thinking with Python", dayOfWeek = Calendar.WEDNESDAY, startHour = 12, startMinute = 45, endHour = 13, endMinute = 30, room = "A213", professor = "Prof. Sharija P M (SPM)", colorHex = 0xFF10B981))

            // Thursday
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ATP", courseName = "Algorithmic Thinking with Python", dayOfWeek = Calendar.THURSDAY, startHour = 8, startMinute = 0, endHour = 8, endMinute = 45, room = "A213", professor = "Prof. Sharija P M (SPM)", colorHex = 0xFF10B981))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "CHY", courseName = "Chemistry for Information & Electrical Science", dayOfWeek = Calendar.THURSDAY, startHour = 9, startMinute = 10, endHour = 9, endMinute = 55, room = "A213", professor = "Dr. Jude Martin Mendez (JMM)", colorHex = 0xFF06B6D4))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "EG", courseName = "Engineering Graphics and CAD", dayOfWeek = Calendar.THURSDAY, startHour = 10, startMinute = 0, endHour = 11, endMinute = 35, room = "CAD Lab / A213", professor = "Prof. Feeba George (FEG)", colorHex = 0xFF8B5CF6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "BEC", courseName = "Introduction to Electrical and Electronics Engineering", dayOfWeek = Calendar.THURSDAY, startHour = 11, startMinute = 55, endHour = 12, endMinute = 40, room = "A213", professor = "Prof. SKV", colorHex = 0xFFF59E0B))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "BEE", courseName = "Introduction to Electrical and Electronics Engineering", dayOfWeek = Calendar.THURSDAY, startHour = 12, startMinute = 45, endHour = 13, endMinute = 30, room = "A213", professor = "Dr. Linss T Alex (LTA)", colorHex = 0xFF6366F1))

            // Friday
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "LS-P", courseName = "Life Skills & Communication - Practical", dayOfWeek = Calendar.FRIDAY, startHour = 8, startMinute = 0, endHour = 8, endMinute = 45, room = "Language Lab", professor = "Prof. DMR", colorHex = 0xFFEC4899))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "EG", courseName = "Engineering Graphics and CAD", dayOfWeek = Calendar.FRIDAY, startHour = 9, startMinute = 10, endHour = 10, endMinute = 45, room = "CAD Lab / A213", professor = "Prof. Feeba George (FEG)", colorHex = 0xFF8B5CF6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "MAT", courseName = "Mathematics for Information Science - I", dayOfWeek = Calendar.FRIDAY, startHour = 10, startMinute = 50, endHour = 11, endMinute = 35, room = "A213", professor = "Prof. Shereeta D'Cotha (SDC)", colorHex = 0xFF8B5CF6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ATP", courseName = "Algorithmic Thinking with Python", dayOfWeek = Calendar.FRIDAY, startHour = 11, startMinute = 55, endHour = 12, endMinute = 40, room = "A213", professor = "Prof. Sharija P M (SPM)", colorHex = 0xFF10B981))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "CHY", courseName = "Chemistry for Information & Electrical Science", dayOfWeek = Calendar.FRIDAY, startHour = 12, startMinute = 45, endHour = 13, endMinute = 30, room = "A213", professor = "Dr. Jude Martin Mendez (JMM)", colorHex = 0xFF06B6D4))

            // Saturday
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "LS", courseName = "Life Skills and Professional Communication", dayOfWeek = Calendar.SATURDAY, startHour = 9, startMinute = 10, endHour = 10, endMinute = 45, room = "A213", professor = "Prof. DMR", colorHex = 0xFFEC4899))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ACTIVITY", courseName = "Student Activity Hour", dayOfWeek = Calendar.SATURDAY, startHour = 10, startMinute = 50, endHour = 11, endMinute = 35, room = "A213", professor = "Student Affairs", colorHex = 0xFF14B8A6))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "ATP", courseName = "Algorithmic Thinking with Python", dayOfWeek = Calendar.SATURDAY, startHour = 11, startMinute = 55, endHour = 12, endMinute = 40, room = "A213", professor = "Prof. Sharija P M (SPM)", colorHex = 0xFF10B981))
            academicDao.insertTimetableEntry(TimetableEntry(courseCode = "EG", courseName = "Engineering Graphics and CAD", dayOfWeek = Calendar.SATURDAY, startHour = 12, startMinute = 45, endHour = 13, endMinute = 30, room = "CAD Lab / A213", professor = "Prof. Feeba George (FEG)", colorHex = 0xFF8B5CF6))
        }

        val attendance = academicDao.getAllAttendance().first()
        if (attendance.isEmpty()) {
            val timetable = academicDao.getAllTimetable().first()
            val courses = timetable.map { it.courseCode }.distinct().ifEmpty {
                listOf("MAT", "ATP", "ATP LAB", "CHY", "BEE", "BEC", "EG", "LS", "CHY LAB/EE-EC W/S")
            }
            courses.forEach { code ->
                academicDao.insertAttendance(
                    AttendanceRecord(
                        courseCode = code,
                        attendedClasses = 0,
                        totalClasses = 0,
                        targetPercentage = 80
                    )
                )
            }
        }

        val assignments = academicDao.getAllAssignments().first()
        if (assignments.isEmpty()) {
            val examCal = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 1, 9, 0, 0)
            }
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 3600 * 1000L
            academicDao.insertAssignment(AssignmentItem(courseCode = "CSE S1", title = "S1 First Exam / Midterms (Target >85%)", description = "Semester 1 formal midterm assessment. Aiming for 85%+ score.", dueDateMillis = examCal.timeInMillis, weightPercentage = 35, category = "Exam"))
            academicDao.insertAssignment(AssignmentItem(courseCode = "ATP", title = "Control Flow & Recursion Programming Set", description = "Python recursion and algorithmic logic exercises.", dueDateMillis = now + dayMillis * 4, weightPercentage = 10, category = "Assignment"))
            academicDao.insertAssignment(AssignmentItem(courseCode = "MAT", title = "Calculus & Matrix Rank Problem Set", description = "Linear algebra problem sheet 3.", dueDateMillis = now + dayMillis * 7, weightPercentage = 10, category = "Assignment"))
            academicDao.insertAssignment(AssignmentItem(courseCode = "EG", title = "Orthographic & Isometric CAD Projections", description = "Engineering Graphics lab sheet submissions.", dueDateMillis = now + dayMillis * 11, weightPercentage = 15, category = "Project"))
        }

        val weightLogs = fitnessDao.getWeightLogs().first()
        if (weightLogs.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 3600 * 1000L
            fitnessDao.insertWeightLog(BodyWeightLog(weightKg = 70.4f, note = "Starting weigh-in before Winter Arc recomp", timestamp = now - dayMillis * 9))
            fitnessDao.insertWeightLog(BodyWeightLog(weightKg = 70.1f, note = "Consistent ~2,150 kcal maintenance & 140g protein", timestamp = now - dayMillis * 6))
            fitnessDao.insertWeightLog(BodyWeightLog(weightKg = 69.8f, note = "Waistline tapering, strength holds steady", timestamp = now - dayMillis * 3))
            fitnessDao.insertWeightLog(BodyWeightLog(weightKg = 69.5f, note = "Fasted weigh-in • on track for 66–67kg goal", timestamp = now))
        }

        val pushSets = fitnessDao.getSetsForSplit("Push").first()
        if (pushSets.isEmpty()) {
            // Core Push Exercises: Flat Dumbbell Press, Incline Dumbbell Press, Dumbbell Lateral Raises, Cable Rope Triceps Pushdowns
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Flat Dumbbell Press", splitCategory = "Push", setNumber = 1, reps = 10, weightKg = 24f, isCompleted = true))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Flat Dumbbell Press", splitCategory = "Push", setNumber = 2, reps = 10, weightKg = 26f, isCompleted = true))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Flat Dumbbell Press", splitCategory = "Push", setNumber = 3, reps = 8, weightKg = 28f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Incline Dumbbell Press", splitCategory = "Push", setNumber = 1, reps = 10, weightKg = 22f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Incline Dumbbell Press", splitCategory = "Push", setNumber = 2, reps = 8, weightKg = 24f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Incline Dumbbell Press", splitCategory = "Push", setNumber = 3, reps = 8, weightKg = 24f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Dumbbell Lateral Raises", splitCategory = "Push", setNumber = 1, reps = 15, weightKg = 10f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Dumbbell Lateral Raises", splitCategory = "Push", setNumber = 2, reps = 15, weightKg = 10f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Dumbbell Lateral Raises", splitCategory = "Push", setNumber = 3, reps = 12, weightKg = 12f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Cable Rope Triceps Pushdowns", splitCategory = "Push", setNumber = 1, reps = 12, weightKg = 22.5f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Cable Rope Triceps Pushdowns", splitCategory = "Push", setNumber = 2, reps = 12, weightKg = 25f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Cable Rope Triceps Pushdowns", splitCategory = "Push", setNumber = 3, reps = 10, weightKg = 27.5f, isCompleted = false))
        }

        val pullSets = fitnessDao.getSetsForSplit("Pull").first()
        if (pullSets.isEmpty()) {
            // Core Pull Exercises: Lat Pulldowns, Chest-Supported Dumbbell Rows, Seated Cable Rows, Cable Face Pulls, Incline Dumbbell Curls
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Lat Pulldowns", splitCategory = "Pull", setNumber = 1, reps = 10, weightKg = 55f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Lat Pulldowns", splitCategory = "Pull", setNumber = 2, reps = 10, weightKg = 60f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Lat Pulldowns", splitCategory = "Pull", setNumber = 3, reps = 8, weightKg = 65f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Chest-Supported Dumbbell Rows", splitCategory = "Pull", setNumber = 1, reps = 10, weightKg = 22f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Chest-Supported Dumbbell Rows", splitCategory = "Pull", setNumber = 2, reps = 10, weightKg = 24f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Chest-Supported Dumbbell Rows", splitCategory = "Pull", setNumber = 3, reps = 8, weightKg = 26f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Seated Cable Rows", splitCategory = "Pull", setNumber = 1, reps = 12, weightKg = 50f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Seated Cable Rows", splitCategory = "Pull", setNumber = 2, reps = 10, weightKg = 55f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Cable Face Pulls", splitCategory = "Pull", setNumber = 1, reps = 15, weightKg = 20f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Cable Face Pulls", splitCategory = "Pull", setNumber = 2, reps = 15, weightKg = 22.5f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Incline Dumbbell Curls", splitCategory = "Pull", setNumber = 1, reps = 12, weightKg = 12f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Incline Dumbbell Curls", splitCategory = "Pull", setNumber = 2, reps = 10, weightKg = 14f, isCompleted = false))
        }

        val legSets = fitnessDao.getSetsForSplit("Legs").first()
        if (legSets.isEmpty()) {
            // Core Legs Exercises: Barbell Squats (or Leg Press), Romanian Deadlifts (RDLs), Bulgarian Split Squats, Standing Calf Raises
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Barbell Squats", splitCategory = "Legs", setNumber = 1, reps = 8, weightKg = 80f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Barbell Squats", splitCategory = "Legs", setNumber = 2, reps = 8, weightKg = 85f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Barbell Squats", splitCategory = "Legs", setNumber = 3, reps = 6, weightKg = 90f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Romanian Deadlifts (RDLs)", splitCategory = "Legs", setNumber = 1, reps = 10, weightKg = 75f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Romanian Deadlifts (RDLs)", splitCategory = "Legs", setNumber = 2, reps = 8, weightKg = 80f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Bulgarian Split Squats", splitCategory = "Legs", setNumber = 1, reps = 10, weightKg = 16f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Bulgarian Split Squats", splitCategory = "Legs", setNumber = 2, reps = 10, weightKg = 18f, isCompleted = false))

            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Standing Calf Raises", splitCategory = "Legs", setNumber = 1, reps = 15, weightKg = 60f, isCompleted = false))
            fitnessDao.insertSet(ExerciseSetLog(exerciseName = "Standing Calf Raises", splitCategory = "Legs", setNumber = 2, reps = 15, weightKg = 65f, isCompleted = false))
        }
    }
}
