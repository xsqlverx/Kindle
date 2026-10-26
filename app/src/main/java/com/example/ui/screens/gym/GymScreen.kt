package com.example.ui.screens.gym

import androidx.compose.animation.AnimatedVisibility
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
    modifier: Modifier = Modifier
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val selectedSplit by viewModel.selectedSplit.collectAsStateWithLifecycle()
    val sets by viewModel.currentSplitSets.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()
    val waterTodayMl by viewModel.totalWaterTodayMl.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimerSeconds.collectAsStateWithLifecycle()

    var showAddSetDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showMealDialog by remember { mutableStateOf(false) }

    val splits = listOf("Push", "Pull", "Legs", "Upper", "Lower", "Cardio")

    val latestWeight = weightLogs.firstOrNull()?.weightKg ?: 69.5f
    val earliestWeight = weightLogs.lastOrNull()?.weightKg ?: 70.4f
    val startingWeight = 70.0f
    val goalWeight = 66.5f
    val weightDelta = latestWeight - startingWeight

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // Floating Rest Timer (if running)
        if (restTimer > 0) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    tint = RingGymRose.copy(alpha = 0.22f),
                    borderAlpha = 0.8f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Rest Timer",
                                tint = RingGymRose,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "REST INTERVAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = RingGymRose
                                )
                                Text(
                                    text = "${restTimer / 60}:${String.format("%02d", restTimer % 60)} remaining",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = lifeColors.textPrimary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    viewModel.startRestTimer(restTimer + 30)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+30s", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    playAppleHaptic(context)
                                    viewModel.stopRestTimer()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = lifeColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
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
                        Column {
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
                            Text("500 ml", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 3. Body Weight Glass Tracker Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showWeightDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LEAN RECOMPOSITION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = lifeColors.primaryAccent
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(RingHabitEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Goal: 66–67 kg",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RingHabitEmerald
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$latestWeight kg",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = lifeColors.textPrimary
                            )
                            Text(
                                text = if (weightDelta <= 0) "${String.format("%.1f", weightDelta)} kg" else "+${String.format("%.1f", weightDelta)} kg",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weightDelta <= 0) RingHabitEmerald else RingGymRose
                            )
                        }
                        Text(
                            text = "Started: 70.0 kg • -3.5 kg taper",
                            fontSize = 11.sp,
                            color = lifeColors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(lifeColors.surfaceGlassHighlight)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "+ Weigh-in",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.primaryAccent
                        )
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
                    text = "Workout Routine Hub",
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
                            color = if (isSelected) Color.White else lifeColors.textPrimary
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
    }

    // Modal: Log Weigh-in
    if (showWeightDialog) {
        var weightInput by remember { mutableStateOf("$latestWeight") }
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
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
                    onClick = {
                        val weight = weightInput.toFloatOrNull() ?: latestWeight
                        viewModel.logBodyWeight(weight, noteInput)
                        showWeightDialog = false
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
private fun ExerciseGlassBlock(
    exerciseName: String,
    sets: List<ExerciseSetLog>,
    onToggleSet: (ExerciseSetLog) -> Unit
) {
    val lifeColors = LocalLifeOsColors.current

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = exerciseName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Sets Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "SET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lifeColors.textTertiary, modifier = Modifier.weight(1f))
                Text(text = "WEIGHT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lifeColors.textTertiary, modifier = Modifier.weight(1.5f))
                Text(text = "REPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lifeColors.textTertiary, modifier = Modifier.weight(1.5f))
                Text(text = "DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lifeColors.textTertiary, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${set.setNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.primaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${set.weightKg} kg",
                        fontSize = 13.sp,
                        color = lifeColors.textPrimary,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "${set.reps}",
                        fontSize = 13.sp,
                        color = lifeColors.textPrimary,
                        modifier = Modifier.weight(1.5f)
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (set.isCompleted) RingGymRose else Color.Transparent
                                )
                                .border(
                                    1.2.dp,
                                    if (set.isCompleted) RingGymRose else lifeColors.textTertiary,
                                    CircleShape
                                )
                                .clickable { onToggleSet(set) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (set.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed set",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
