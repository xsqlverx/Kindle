package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.LifeOsRepository
import com.example.ui.components.LifeOsTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ResetTimerState {
    READY,
    RUNNING,
    PAUSED,
    COMPLETED,
    RESET
}

data class CurrentLectureStatus(
    val isOngoing: Boolean,
    val entry: TimetableEntry?,
    val message: String,
    val timeRemainingOrUntil: String
)

class LifeOsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LifeOsRepository(application)

    // Appearance
    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        repository.saveThemeMode(mode)
    }

    // Navigation
    private val _currentTab = MutableStateFlow(LifeOsTab.TODAY)
    val currentTab: StateFlow<LifeOsTab> = _currentTab.asStateFlow()

    fun setTab(tab: LifeOsTab) {
        _currentTab.value = tab
    }

    // Tasks
    val allTasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topThreeTasks: StateFlow<List<TaskItem>> = repository.topThreeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTaskDone(task: TaskItem) {
        viewModelScope.launch {
            val nextStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
            repository.setTaskStatus(task.id, nextStatus)
        }
    }

    fun addTask(title: String, quadrant: Quadrant, category: String, dueTime: String, isTopThree: Boolean) {
        viewModelScope.launch {
            repository.insertTask(
                TaskItem(
                    title = title,
                    quadrant = quadrant,
                    category = category,
                    dueTime = dueTime,
                    isTopThree = isTopThree
                )
            )
        }
    }

    fun setTaskTriage(taskId: Long, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.setTaskStatus(taskId, newStatus)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Habits
    val allHabits: StateFlow<List<HabitItem>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleHabit(habit: HabitItem) {
        viewModelScope.launch {
            repository.toggleHabit(habit.id, habit.isCompletedToday)
        }
    }

    fun addHabit(title: String, category: String) {
        viewModelScope.launch {
            repository.insertHabit(HabitItem(title = title, category = category))
        }
    }

    // Academics Timetable
    val allTimetable: StateFlow<List<TimetableEntry>> = repository.allTimetable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
        if (it in Calendar.MONDAY..Calendar.SATURDAY) it else Calendar.MONDAY
    })
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    // Target Exam Configuration & Countdown
    private val _targetExam = MutableStateFlow(repository.getTargetExam())
    val targetExam: StateFlow<ExamTarget> = _targetExam.asStateFlow()

    val examCountdownDays: StateFlow<Int> = _targetExam.map { exam ->
        exam.getDaysLeft()?.coerceAtLeast(0) ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setTargetExam(title: String, dateMillis: Long) {
        repository.saveTargetExam(title, dateMillis)
        _targetExam.value = ExamTarget(title = title, dateMillis = dateMillis)
    }

    fun clearTargetExam() {
        repository.clearTargetExam()
        _targetExam.value = ExamTarget(title = "", dateMillis = 0L)
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun addTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.insertTimetableEntry(entry)
        }
    }

    fun deleteTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(entry)
        }
    }

    val dayTimetable: StateFlow<List<TimetableEntry>> = combine(allTimetable, _selectedDay) { list, day ->
        list.filter { it.dayOfWeek == day }.sortedBy { it.startHour * 60 + it.startMinute }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Class Detection
    private val lectureClock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L)
        }
    }
    val currentLectureStatus: StateFlow<CurrentLectureStatus> = combine(allTimetable, lectureClock) { list, now ->
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val todayClasses = list.filter { it.dayOfWeek == currentDay }.sortedBy { it.startHour * 60 + it.startMinute }

        val ongoing = todayClasses.find {
            val start = it.startHour * 60 + it.startMinute
            val end = it.endHour * 60 + it.endMinute
            currentMinutes in start until end
        }

        if (ongoing != null) {
            val end = ongoing.endHour * 60 + ongoing.endMinute
            val remaining = end - currentMinutes
            CurrentLectureStatus(
                isOngoing = true,
                entry = ongoing,
                message = "In Class: ${ongoing.courseCode} - ${ongoing.courseName}",
                timeRemainingOrUntil = "$remaining min remaining (${ongoing.room})"
            )
        } else {
            val next = todayClasses.find {
                val start = it.startHour * 60 + it.startMinute
                start > currentMinutes
            }

            if (next != null) {
                val start = next.startHour * 60 + next.startMinute
                val until = start - currentMinutes
                CurrentLectureStatus(
                    isOngoing = false,
                    entry = next,
                    message = "Up Next: ${next.courseCode} at ${String.format("%02d:%02d", next.startHour, next.startMinute)}",
                    timeRemainingOrUntil = "Starts in $until min • ${next.room}"
                )
            } else {
                CurrentLectureStatus(
                    isOngoing = false,
                    entry = null,
                    message = "Campus Schedule Clear",
                    timeRemainingOrUntil = "No further classes scheduled for today"
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentLectureStatus(false, null, "College", "Checking your saved timetable"))

    // Assignments / Deadlines CRUD
    val assignments: StateFlow<List<AssignmentItem>> = repository.allAssignments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAssignment(assignment: AssignmentItem) {
        viewModelScope.launch {
            repository.toggleAssignmentSubmitted(assignment)
        }
    }

    fun addAssignment(
        courseCode: String,
        title: String,
        description: String = "",
        dueDateMillis: Long,
        weightPercentage: Int = 10,
        category: String = "Project"
    ) {
        viewModelScope.launch {
            repository.insertAssignment(
                AssignmentItem(
                    courseCode = courseCode,
                    title = title,
                    description = description,
                    dueDateMillis = dueDateMillis,
                    weightPercentage = weightPercentage,
                    category = category
                )
            )
        }
    }

    fun updateAssignment(assignment: AssignmentItem) {
        viewModelScope.launch {
            repository.updateAssignment(assignment)
        }
    }

    fun deleteAssignment(assignment: AssignmentItem) {
        viewModelScope.launch {
            repository.deleteAssignment(assignment)
        }
    }

    // Attendance
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected lecture for 60% detail slide-up sheet
    private val _selectedLectureForDetail = MutableStateFlow<TimetableEntry?>(null)
    val selectedLectureForDetail: StateFlow<TimetableEntry?> = _selectedLectureForDetail.asStateFlow()

    fun openLectureDetail(entry: TimetableEntry) {
        _selectedLectureForDetail.value = entry
    }

    fun closeLectureDetail() {
        _selectedLectureForDetail.value = null
    }

    fun recordAttendance(courseCode: String, attended: Boolean) {
        viewModelScope.launch {
            repository.logAttendance(courseCode, attended)
        }
    }

    fun correctAttendance(courseCode: String, attended: Int, total: Int, target: Int) {
        if (attended < 0 || total < attended || target !in 1..100) return
        viewModelScope.launch { repository.correctAttendance(courseCode, attended, total, target) }
    }

    // Fitness: Splits, Exercises, Sets
    private val _selectedSplit = MutableStateFlow("Push")
    val selectedSplit: StateFlow<String> = _selectedSplit.asStateFlow()

    fun setSelectedSplit(split: String) {
        _selectedSplit.value = split
    }

    val currentSplitSets: StateFlow<List<ExerciseSetLog>> = _selectedSplit.flatMapLatest { split ->
        repository.getSetsForSplit(split)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSetCompleted(set: ExerciseSetLog) {
        viewModelScope.launch {
            repository.updateWorkoutSet(set.copy(isCompleted = !set.isCompleted))
        }
    }

    fun addExerciseSet(exerciseName: String, reps: Int, weight: Float) {
        viewModelScope.launch {
            val sets = currentSplitSets.value.filter { it.exerciseName == exerciseName }
            val nextSetNum = (sets.maxOfOrNull { it.setNumber } ?: 0) + 1
            repository.insertWorkoutSet(
                ExerciseSetLog(
                    exerciseName = exerciseName,
                    splitCategory = _selectedSplit.value,
                    setNumber = nextSetNum,
                    reps = reps,
                    weightKg = weight
                )
            )
        }
    }

    // Rest Timer
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()
    private var timerJob: Job? = null

    fun startRestTimer(seconds: Int = 90) {
        timerJob?.cancel()
        _restTimerSeconds.value = seconds
        timerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.value -= 1
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _restTimerSeconds.value = 0
    }

    // Body Weight & Water
    private val _bodyWeightGoal = MutableStateFlow(repository.getBodyWeightGoal())
    val bodyWeightGoal = _bodyWeightGoal.asStateFlow()

    fun setBodyWeightGoal(weight: Float) {
        if (!weight.isFinite() || weight <= 0f) return
        _bodyWeightGoal.value = weight
        repository.saveBodyWeightGoal(weight)
    }

    val weightLogs: StateFlow<List<BodyWeightLog>> = repository.weightLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logBodyWeight(weight: Float, note: String = "") {
        viewModelScope.launch {
            repository.insertWeightLog(weight, note)
        }
    }

    val waterLogs: StateFlow<List<WaterLog>> = repository.waterLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWaterTodayMl: StateFlow<Int> = waterLogs.map { list ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        list.filter { it.timestamp >= startOfDay }.sumOf { it.amountMl }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addWaterMl(amount: Int) {
        viewModelScope.launch {
            repository.addWater(amount)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repository.clearWater()
        }
    }

    fun clearWater() {
        viewModelScope.launch {
            repository.clearWater()
        }
    }

    // Macros
    private val _macros = MutableStateFlow(repository.getSavedMacros())
    val macros: StateFlow<MacroNutrients> = _macros.asStateFlow()

    fun logQuickMeal(calories: Int, protein: Int, carbs: Int, fats: Int) {
        val updated = _macros.value.copy(
            currentCalories = _macros.value.currentCalories + calories,
            currentProteinGrams = _macros.value.currentProteinGrams + protein,
            currentCarbsGrams = _macros.value.currentCarbsGrams + carbs,
            currentFatsGrams = _macros.value.currentFatsGrams + fats
        )
        _macros.value = updated
        repository.saveMacros(updated)
    }

    fun clearMacros() {
        val updated = _macros.value.copy(
            currentCalories = 0,
            currentProteinGrams = 0,
            currentCarbsGrams = 0,
            currentFatsGrams = 0
        )
        _macros.value = updated
        repository.saveMacros(updated)
    }

    fun resetMacrosToDefault() {
        val updated = MacroNutrients(
            currentCalories = 0,
            currentProteinGrams = 0,
            currentCarbsGrams = 0,
            currentFatsGrams = 0,
            targetCalories = 2150,
            targetProteinGrams = 140,
            targetCarbsGrams = 235,
            targetFatsGrams = 65
        )
        _macros.value = updated
        repository.saveMacros(updated)
    }

    // Activity Rings Progress
    val habitRingProgress: StateFlow<Float> = allHabits.map { list ->
        if (list.isEmpty()) 0f
        else list.count { it.isCompletedToday }.toFloat() / list.size.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val waterRingProgress: StateFlow<Float> = totalWaterTodayMl.map { ml ->
        (ml.toFloat() / 3000f).coerceAtMost(1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val workoutRingProgress: StateFlow<Float> = currentSplitSets.map { sets ->
        if (sets.isEmpty()) 0.5f
        else sets.count { it.isCompleted }.toFloat() / sets.size.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)

    // Daily Reset Flow
    val allDailyResets: StateFlow<List<DailyResetRecord>> = repository.allDailyResets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayResetRecord: StateFlow<DailyResetRecord?> = allDailyResets.map { list ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        list.find { it.dateString == dateStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 60-Second Reset Timer State Machine
    private val _resetTimerState = MutableStateFlow(ResetTimerState.READY)
    val resetTimerState: StateFlow<ResetTimerState> = _resetTimerState.asStateFlow()

    private val _resetTimerSeconds = MutableStateFlow(60)
    val resetTimerSeconds: StateFlow<Int> = _resetTimerSeconds.asStateFlow()

    private var resetTimerJob: Job? = null

    fun start60sTimer() {
        resetTimerJob?.cancel()
        _resetTimerState.value = ResetTimerState.RUNNING
        resetTimerJob = viewModelScope.launch {
            while (_resetTimerSeconds.value > 0 && _resetTimerState.value == ResetTimerState.RUNNING) {
                delay(1000)
                if (_resetTimerState.value == ResetTimerState.RUNNING) {
                    _resetTimerSeconds.value -= 1
                    if (_resetTimerSeconds.value <= 0) {
                        _resetTimerState.value = ResetTimerState.COMPLETED
                    }
                }
            }
        }
    }

    fun pause60sTimer() {
        if (_resetTimerState.value == ResetTimerState.RUNNING) {
            _resetTimerState.value = ResetTimerState.PAUSED
            resetTimerJob?.cancel()
        }
    }

    fun stop60sTimer() {
        resetTimerJob?.cancel()
        _resetTimerState.value = ResetTimerState.COMPLETED
    }

    fun resume60sTimer() {
        if (_resetTimerState.value == ResetTimerState.PAUSED) {
            start60sTimer()
        }
    }

    fun reset60sTimer() {
        resetTimerJob?.cancel()
        _resetTimerSeconds.value = 60
        _resetTimerState.value = ResetTimerState.RESET
    }

    fun setTimerReady() {
        resetTimerJob?.cancel()
        _resetTimerSeconds.value = 60
        _resetTimerState.value = ResetTimerState.READY
    }

    fun submitDailyReset(
        mood: MoodType,
        brainDump: String,
        triagedCount: Int,
        gearPacked: Boolean
    ) {
        stop60sTimer()
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.saveDailyReset(
                DailyResetRecord(
                    dateString = dateStr,
                    mood = mood,
                    brainDump = brainDump,
                    triagedTasksCount = triagedCount,
                    gearPacked = gearPacked
                )
            )
        }
    }

    // Settings & Configuration
    fun resetAllDataToDefault() {
        viewModelScope.launch {
            repository.resetAllMockData()
            resetMacrosToDefault()
        }
    }

    fun resetAttendanceData() {
        viewModelScope.launch {
            repository.resetAttendanceData()
        }
    }

    fun clearTrackingData() {
        viewModelScope.launch {
            repository.clearTasks()
            repository.clearHabits()
            repository.clearWater()
            clearMacros()
        }
    }

    fun clearTasks() {
        viewModelScope.launch {
            repository.clearTasks()
        }
    }

    fun clearHabits() {
        viewModelScope.launch {
            repository.clearHabits()
        }
    }

    fun importTimetableJson(jsonStr: String): Pair<Boolean, String> {
        return try {
            val array = org.json.JSONArray(jsonStr.trim())
            val entries = mutableListOf<TimetableEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entries.add(
                    TimetableEntry(
                        courseCode = obj.optString("courseCode", "COURSE"),
                        courseName = obj.optString("courseName", "Lecture"),
                        dayOfWeek = obj.optInt("dayOfWeek", Calendar.MONDAY),
                        startHour = obj.optInt("startHour", 9),
                        startMinute = obj.optInt("startMinute", 0),
                        endHour = obj.optInt("endHour", 10),
                        endMinute = obj.optInt("endMinute", 0),
                        room = obj.optString("room", "TBD"),
                        professor = obj.optString("professor", "")
                    )
                )
            }
            if (entries.isEmpty()) {
                Pair(false, "JSON array was empty")
            } else {
                viewModelScope.launch {
                    repository.importTimetable(entries)
                }
                Pair(true, "Successfully imported ${entries.size} classes")
            }
        } catch (e: Exception) {
            Pair(false, "Parse error: ${e.localizedMessage ?: "Invalid JSON"}")
        }
    }

    fun importWorkoutJson(jsonStr: String): Pair<Boolean, String> {
        return try {
            val array = org.json.JSONArray(jsonStr.trim())
            val sets = mutableListOf<ExerciseSetLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                sets.add(
                    ExerciseSetLog(
                        exerciseName = obj.optString("exerciseName", "Exercise"),
                        splitCategory = obj.optString("splitCategory", "Push"),
                        setNumber = obj.optInt("setNumber", i + 1),
                        reps = obj.optInt("reps", 10),
                        weightKg = obj.optDouble("weightKg", 20.0).toFloat()
                    )
                )
            }
            if (sets.isEmpty()) {
                Pair(false, "JSON array was empty")
            } else {
                viewModelScope.launch {
                    repository.importWorkoutSets(sets)
                }
                Pair(true, "Successfully imported ${sets.size} exercise sets")
            }
        } catch (e: Exception) {
            Pair(false, "Parse error: ${e.localizedMessage ?: "Invalid JSON"}")
        }
    }
}
