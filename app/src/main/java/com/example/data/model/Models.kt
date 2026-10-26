package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Quadrant(val label: String, val description: String) {
    Q1_DO("Do First", "Urgent & Important"),
    Q2_SCHEDULE("Schedule", "Important, Not Urgent"),
    Q3_DELEGATE("Delegate", "Urgent, Not Important"),
    Q4_ELIMINATE("Drop", "Neither Urgent nor Important")
}

enum class TaskStatus {
    PENDING,
    COMPLETED,
    MOVED_TOMORROW,
    MOVED_LATER,
    DROPPED
}

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val quadrant: Quadrant = Quadrant.Q1_DO,
    val status: TaskStatus = TaskStatus.PENDING,
    val category: String = "General",
    val dueTime: String = "",
    val isTopThree: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val iconName: String = "check",
    val targetDaysPerWeek: Int = 7,
    val currentStreak: Int = 0,
    val isCompletedToday: Boolean = false,
    val category: String = "General"
)

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val courseName: String,
    val dayOfWeek: Int, // 1 = Monday ... 5 = Friday
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val room: String,
    val professor: String = "",
    val colorHex: Long = 0xFF6366F1
)

@Entity(tableName = "assignments")
data class AssignmentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long,
    val isSubmitted: Boolean = false,
    val weightPercentage: Int = 10,
    val category: String = "Project"
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0,
    val targetPercentage: Int = 80
)

@Entity(tableName = "fitness_logs")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitName: String, // Push, Pull, Legs, Upper, Lower, Cardio
    val dateMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val completedExercisesCount: Int = 0,
    val totalExercisesCount: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "exercise_sets")
data class ExerciseSetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val splitCategory: String, // Push, Pull, etc.
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_weight_logs")
data class BodyWeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Float,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MoodType(val label: String, val iconKey: String) {
    DRAINING("Draining", "draining"),
    OKAY("Okay", "okay"),
    GOOD("Good", "good")
}

@Entity(tableName = "daily_resets")
data class DailyResetRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val mood: MoodType = MoodType.GOOD,
    val brainDump: String = "",
    val triagedTasksCount: Int = 0,
    val gearPacked: Boolean = false,
    val completedAt: Long = System.currentTimeMillis()
)

enum class AppThemeMode(val title: String) {
    AUTO_TIME("System Auto (Time)"),
    LIGHT_DAY("Day Crisp"),
    SUNSET_AFTERNOON("Sunset Golden"),
    NIGHT_GRAPHITE("Night Graphite")
}

data class MacroNutrients(
    val currentCalories: Int = 0,
    val targetCalories: Int = 2150,
    val currentProteinGrams: Int = 0,
    val targetProteinGrams: Int = 140,
    val currentCarbsGrams: Int = 0,
    val targetCarbsGrams: Int = 235,
    val currentFatsGrams: Int = 0,
    val targetFatsGrams: Int = 65
)

data class ExamTarget(
    val title: String = "Upcoming Exam",
    val dateMillis: Long = 0L
) {
    fun getDaysLeft(): Int? {
        if (dateMillis <= 0L) return null
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val diff = dateMillis - now
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    fun getFormattedDate(): String {
        if (dateMillis <= 0L) return "Not set"
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(dateMillis))
    }
}

