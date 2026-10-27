package com.example.ui.screens.gym

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BodyWeightLog
import com.example.data.model.ExerciseSetLog
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.LifeOsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GymScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier,
    onLogTraining: ((String) -> Unit)? = null
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val selectedSplit by viewModel.selectedSplit.collectAsStateWithLifecycle()
    val sets by viewModel.currentSplitSets.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()
    val waterTodayMl by viewModel.totalWaterTodayMl.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val goalWeight by viewModel.bodyWeightGoal.collectAsStateWithLifecycle()

    var showAddSetDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showMealDialog by remember { mutableStateOf(false) }

    val splits = listOf("Push", "Pull", "Legs", "Upper", "Lower", "Cardio")

    val latestWeight = weightLogs.firstOrNull()?.weightKg
    val startingWeight = weightLogs.lastOrNull()?.weightKg
    val weightDelta = if (latestWeight != null && startingWeight != null) latestWeight - startingWeight else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            WorkspaceIntro("YOUR SAVED ROUTINE", "$selectedSplit.\nOne set at a time.",
                if (sets.isEmpty()) "Build your routine below, then keep your attention on the next set." else
                    "${sets.count { it.isCompleted }} of ${sets.size} saved sets checked off.")
            if (sets.isNotEmpty()) {
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    sets.take(16).forEach { set ->
                        Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (set.isCompleted) lifeColors.primaryAccent else lifeColors.surfaceGlassHighlight))
                    }
                }
                val next = sets.firstOrNull { !it.isCompleted }
                Spacer(Modifier.height(13.dp))
                Text(next?.let { "Next up · ${it.exerciseName}" } ?: "Your saved sets are checked off.",
                    color = lifeColors.textSecondary, fontSize = 12.sp)
            }
            if (onLogTraining != null) TextButton(onClick = { onLogTraining(selectedSplit) }) { Text("Log today's training effort") }
        }

        if (restTimer > 0) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth().testTag("gym_rest_timer")) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        WorkspaceLabel("BETWEEN SETS")
                        Spacer(Modifier.height(8.dp))
                        Text(String.format(Locale.getDefault(), "%02d:%02d", restTimer / 60, restTimer % 60),
                            fontSize = 35.sp, fontWeight = FontWeight.Light, color = lifeColors.primaryAccent)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.startRestTimer(restTimer + 30) }) { Text("Add 30 seconds") }
                            TextButton(onClick = { viewModel.stopRestTimer() }) { Text("End rest", color = lifeColors.textSecondary) }
                        }
                    }
                }
            }
        }

        // 4. Workout Splits Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your routine",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = lifeColors.textPrimary
                )
                TextButton(onClick = { showAddSetDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Set", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(splits) { split ->
                    val isSelected = (split == selectedSplit)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) lifeColors.primaryAccent
                                else lifeColors.surfaceGlass
                            )
                            .border(
                                1.dp,
                                if (isSelected) lifeColors.primaryAccent else lifeColors.borderGlass,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                playAppleHaptic(context)
                                viewModel.setSelectedSplit(split)
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = split,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else lifeColors.textPrimary
                        )
                    }
                }
            }
        }

        // 5. Exercise Sets Grouped by Exercise
        if (sets.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = lifeColors.textTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No exercises logged yet for $selectedSplit split",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = lifeColors.textPrimary
                            )
                            Text(
                                text = "Tap 'Add Set' above to build your session",
                                fontSize = 12.sp,
                                color = lifeColors.textSecondary
                            )
                        }
                    }
                }
            }
        } else {
            val groupedByExercise = sets.groupBy { it.exerciseName }
            items(groupedByExercise.entries.toList()) { (exerciseName, exerciseSets) ->
                ExerciseGlassBlock(
                    exerciseName = exerciseName,
                    sets = exerciseSets,
                    onToggleSet = { set ->
                        playAppleHaptic(context)
                        viewModel.toggleSetCompleted(set)
                        if (!set.isCompleted) {
                            // Start Apple-style 90s rest timer automatically when set checked off!
                            viewModel.startRestTimer(90)
                        }
                    }
                )
            }
        }
        item {
            Spacer(Modifier.height(10.dp))
            Text("Support the work.", fontSize = 23.sp, color = lifeColors.textPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text("Nutrition, hydration and your weigh-ins.", fontSize = 12.sp, color = lifeColors.textSecondary)
        }

        // 1. Calorie & Macro Target Glass Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gym_macros_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NUTRITION & MACROS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = lifeColors.primaryAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${macros.currentCalories} / ${macros.targetCalories} kcal",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = lifeColors.textPrimary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (macros.currentCalories > 0) {
                                IconButton(
                                    onClick = {
                                        playAppleHaptic(context)
                                        viewModel.clearMacros()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset calories and macros",
                                        tint = lifeColors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    showMealDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Macro Progress Bars
                    MacroProgressRow(
                        label = "Protein",
                        current = macros.currentProteinGrams,
                        target = macros.targetProteinGrams,
                        unit = "g",
                        color = IrisRadiant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MacroProgressRow(
                        label = "Carbs",
                        current = macros.currentCarbsGrams,
                        target = macros.targetCarbsGrams,
                        unit = "g",
                        color = SunsetAmber
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MacroProgressRow(
                        label = "Fats",
                        current = macros.currentFatsGrams,
                        target = macros.targetFatsGrams,
                        unit = "g",
                        color = RingGymRose
                    )
                }
            }
        }

        // 2. Water Intake Glass Tracker
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row: Icon, text info, and reset button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(RingWaterCyan.copy(alpha = 0.2f))
                                    .border(1.dp, RingWaterCyan.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Hydration",
                                    tint = RingWaterCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "DAILY HYDRATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = RingWaterCyan
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$waterTodayMl / 3,000 ml",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lifeColors.textPrimary
                                    )
                                    Text(
                                        text = "• ${((waterTodayMl.toFloat() / 3000f).coerceIn(0f, 1f) * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RingWaterCyan
                                    )
                                }
                            }
                        }

                        if (waterTodayMl > 0) {
                            IconButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    viewModel.clearWater()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset water intake",
                                    tint = lifeColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Sleek progress bar
                    val waterProgress = (waterTodayMl.toFloat() / 3000f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(RingWaterCyan.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(waterProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(RingWaterCyan)
                        )
                    }

                    // Mobile-responsive quick log buttons (2 balanced buttons spanning row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                playAppleHaptic(context)
                                viewModel.addWaterMl(250)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = RingWaterCyan.copy(alpha = 0.15f),
                                contentColor = RingWaterCyan
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("250 ml", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                playAppleHaptic(context)
                                viewModel.addWaterMl(500)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RingWaterCyan)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("500 ml", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkDeep)
                        }
                    }
                }
            }
        }

        item {
            LiquidGlassCard(Modifier.fillMaxWidth(), onClick = { showWeightDialog = true }) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    WorkspaceLabel("YOUR WEIGH-INS")
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(latestWeight?.let { "$it kg" } ?: "—", fontSize = 27.sp, fontWeight = FontWeight.Light, color = lifeColors.textPrimary)
                            Text(if (latestWeight == null) "No weigh-in yet" else "Latest saved weight", fontSize = 11.sp, color = lifeColors.textTertiary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("$goalWeight kg", fontSize = 27.sp, fontWeight = FontWeight.Light, color = lifeColors.primaryAccent)
                            Text("Current target · tap to edit", fontSize = 11.sp, color = lifeColors.textTertiary)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(if (weightDelta == null) "Log a weigh-in to begin your history." else
                        String.format(Locale.getDefault(), "%+.1f kg since your first saved weigh-in", weightDelta),
                        color = lifeColors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showWeightDialog = true }) { Text("Log weigh-in / edit target", fontSize = 12.sp) }
                }
            }
        }

    }

    // Modal: Log Weigh-in
    if (showWeightDialog) {
        var weightInput by remember { mutableStateOf(latestWeight?.toString().orEmpty()) }
        var goalInput by remember { mutableStateOf(goalWeight.toString()) }
        val parsedWeight = weightInput.toFloatOrNull()
        val parsedGoal = goalInput.toFloatOrNull()
        val validWeight = parsedWeight != null && parsedWeight.isFinite() && parsedWeight > 0f
        val validGoal = parsedGoal != null && parsedGoal.isFinite() && parsedGoal > 0f
        var noteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Log Body Weight", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = weightInput.isNotBlank() && !validWeight,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = goalInput, onValueChange = { goalInput = it },
                        label = { Text("Target weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Note (e.g., Fasted, Post-gym)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = validWeight && validGoal,
                    onClick = {
                        if (validWeight && validGoal) {
                            viewModel.logBodyWeight(requireNotNull(parsedWeight), noteInput)
                            viewModel.setBodyWeightGoal(requireNotNull(parsedGoal))
                            showWeightDialog = false
                        }
                    }
                ) {
                    Text("Save Weigh-in")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Add Exercise Set
    if (showAddSetDialog) {
        var exerciseName by remember { mutableStateOf("") }
        var repsInput by remember { mutableStateOf("10") }
        var weightInput by remember { mutableStateOf("60") }

        val quickExercises = when (selectedSplit) {
            "Push" -> listOf("Flat Dumbbell Press", "Incline Dumbbell Press", "Dumbbell Lateral Raises", "Cable Rope Triceps Pushdowns")
            "Pull" -> listOf("Lat Pulldowns", "Chest-Supported Dumbbell Rows", "Seated Cable Rows", "Cable Face Pulls", "Incline Dumbbell Curls")
            "Legs" -> listOf("Barbell Squats", "Romanian Deadlifts (RDLs)", "Bulgarian Split Squats", "Standing Calf Raises")
            else -> listOf("Bench Press", "Squat", "Deadlift", "Overhead Press")
        }

        AlertDialog(
            onDismissRequest = { showAddSetDialog = false },
            title = { Text("Add $selectedSplit Set", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Select Core Exercise:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = lifeColors.textSecondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickExercises) { ex ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (exerciseName == ex) lifeColors.primaryAccent.copy(alpha = 0.25f) else lifeColors.surfaceGlassHighlight)
                                    .clickable { exerciseName = ex }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = ex,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (exerciseName == ex) lifeColors.primaryAccent else lifeColors.textPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name") },
                        placeholder = { Text("e.g., Incline Dumbbell Press") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = repsInput,
                            onValueChange = { repsInput = it },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exerciseName.isNotBlank()) {
                            viewModel.addExerciseSet(
                                exerciseName = exerciseName.trim(),
                                reps = repsInput.toIntOrNull() ?: 10,
                                weight = weightInput.toFloatOrNull() ?: 0f
                            )
                        }
                        showAddSetDialog = false
                    }
                ) {
                    Text("Add Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Quick Meal Logger
    if (showMealDialog) {
        var calInput by remember { mutableStateOf("550") }
        var proteinInput by remember { mutableStateOf("40") }
        var carbsInput by remember { mutableStateOf("65") }
        var fatsInput by remember { mutableStateOf("15") }

        AlertDialog(
            onDismissRequest = { showMealDialog = false },
            title = { Text("Quick Meal Macros", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = calInput,
                        onValueChange = { calInput = it },
                        label = { Text("Calories (kcal)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proteinInput,
                            onValueChange = { proteinInput = it },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = carbsInput,
                            onValueChange = { carbsInput = it },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = fatsInput,
                            onValueChange = { fatsInput = it },
                            label = { Text("Fats (g)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logQuickMeal(
                            calories = calInput.toIntOrNull() ?: 0,
                            protein = proteinInput.toIntOrNull() ?: 0,
                            carbs = carbsInput.toIntOrNull() ?: 0,
                            fats = fatsInput.toIntOrNull() ?: 0
                        )
                        showMealDialog = false
                    }
                ) {
                    Text("Save Meal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMealDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color
) {
    val lifeColors = LocalLifeOsColors.current
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = lifeColors.textSecondary
            )
            Text(
                text = "$current / $target $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ExerciseGlassBlock(exerciseName: String, sets: List<ExerciseSetLog>, onToggleSet: (ExerciseSetLog) -> Unit) {
    val colors = LocalLifeOsColors.current
    LiquidGlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(exerciseName, color = colors.textPrimary, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            Text("${sets.count { it.isCompleted }} of ${sets.size} sets complete", color = colors.textTertiary, fontSize = 11.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WorkspaceLabel("SET", Modifier.width(38.dp))
                WorkspaceLabel("WEIGHT", Modifier.weight(1f))
                WorkspaceLabel("REPS", Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(8.dp))
            sets.sortedBy { it.setNumber }.forEach { set ->
                HorizontalDivider(color = colors.borderGlass.copy(alpha = 0.6f))
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(set.setNumber.toString(), color = colors.textTertiary, fontSize = 12.sp, modifier = Modifier.width(38.dp))
                    Text("${set.weightKg} kg", color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(set.reps.toString(), color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Checkbox(set.isCompleted, { onToggleSet(set) }, modifier = Modifier.size(48.dp).testTag("exercise_set_${set.id}"),
                        colors = CheckboxDefaults.colors(checkedColor = colors.primaryAccent, checkmarkColor = MaterialTheme.colorScheme.onPrimary))
                }
            }
        }
    }
}
