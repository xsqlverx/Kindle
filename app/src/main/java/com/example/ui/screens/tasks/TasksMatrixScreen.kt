package com.example.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Quadrant
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.ui.components.*
import com.example.ui.screens.today.TaskGlassRow
import com.example.ui.theme.*
import com.example.viewmodel.LifeOsViewModel

enum class TaskFilterOption(val label: String) {
    ALL("All Tasks"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    Q1_DO("Q1 Do First"),
    Q2_SCHEDULE("Q2 Schedule"),
    Q3_DELEGATE("Q3 Delegate"),
    Q4_ELIMINATE("Q4 Drop")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksMatrixScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier,
    onStartFocus: ((TaskItem) -> Unit)? = null
) {
    val lifeColors = LocalLifeOsColors.current
    val context = LocalContext.current

    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    var selectedFilter by rememberSaveable { mutableStateOf(TaskFilterOption.ALL) }
    var selectedQuadrantFilter by rememberSaveable { mutableStateOf<Quadrant?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var quickTaskTitle by rememberSaveable { mutableStateOf("") }
    var showPriorityMatrix by rememberSaveable { mutableStateOf(false) }
    var showCompletedSection by rememberSaveable { mutableStateOf(true) }

    val pendingTasks = remember(allTasks) { allTasks.filter { it.status == TaskStatus.PENDING } }
    val completedTasks = remember(allTasks) { allTasks.filter { it.status == TaskStatus.COMPLETED } }
    val q1Tasks = remember(allTasks) { allTasks.filter { it.quadrant == Quadrant.Q1_DO && it.status == TaskStatus.PENDING } }
    val q2Tasks = remember(allTasks) { allTasks.filter { it.quadrant == Quadrant.Q2_SCHEDULE && it.status == TaskStatus.PENDING } }
    val q3Tasks = remember(allTasks) { allTasks.filter { it.quadrant == Quadrant.Q3_DELEGATE && it.status == TaskStatus.PENDING } }
    val q4Tasks = remember(allTasks) { allTasks.filter { it.quadrant == Quadrant.Q4_ELIMINATE && it.status == TaskStatus.PENDING } }

    val filteredTasks = remember(allTasks, selectedFilter) {
        when (selectedFilter) {
            TaskFilterOption.ALL -> pendingTasks
            TaskFilterOption.PENDING -> pendingTasks
            TaskFilterOption.COMPLETED -> completedTasks
            TaskFilterOption.Q1_DO -> q1Tasks
            TaskFilterOption.Q2_SCHEDULE -> q2Tasks
            TaskFilterOption.Q3_DELEGATE -> q3Tasks
            TaskFilterOption.Q4_ELIMINATE -> q4Tasks
        }
    }

    val totalCount = pendingTasks.size + completedTasks.size
    val completedCount = completedTasks.size
    val completionPercentage = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage,
        animationSpec = tween(durationMillis = 400),
        label = "taskCompletionProgress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().testTag("tasks_matrix_hero")) {
                WorkspaceIntro("CHOOSE YOUR NEXT MOVE", "Make room for\nwhat matters.", "A few clear priorities. A place for everything else.")
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${pendingTasks.size}", fontSize = 42.sp, fontWeight = FontWeight.Light, color = lifeColors.textPrimary)
                        Text("open · ${completedTasks.size} completed", fontSize = 12.sp, color = lifeColors.textSecondary)
                    }
                    FilledTonalButton(onClick = { showAddTaskDialog = true }, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("new_task_modal_button")) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("New task", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(progress = { animatedProgress }, color = lifeColors.primaryAccent,
                    trackColor = lifeColors.borderGlass, modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showPriorityMatrix = !showPriorityMatrix }
                    .heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your priority map", color = lifeColors.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(if (showPriorityMatrix) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Toggle priority matrix", tint = lifeColors.primaryAccent)
                }
                AnimatedVisibility(showPriorityMatrix) {
                    Column {
                            // 2x2 Matrix Overview Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MatrixTile(
                                    title = "Q1: Do First",
                                    subtitle = "Urgent & Imp",
                                    count = q1Tasks.size,
                                    accentColor = Q1DoColor,
                                    isSelected = selectedQuadrantFilter == Quadrant.Q1_DO,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedQuadrantFilter = if (selectedQuadrantFilter == Quadrant.Q1_DO) null else Quadrant.Q1_DO
                                        selectedFilter = if (selectedQuadrantFilter == Quadrant.Q1_DO) TaskFilterOption.Q1_DO else TaskFilterOption.ALL
                                    }
                                )
                                MatrixTile(
                                    title = "Q2: Schedule",
                                    subtitle = "Important",
                                    count = q2Tasks.size,
                                    accentColor = Q2ScheduleColor,
                                    isSelected = selectedQuadrantFilter == Quadrant.Q2_SCHEDULE,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedQuadrantFilter = if (selectedQuadrantFilter == Quadrant.Q2_SCHEDULE) null else Quadrant.Q2_SCHEDULE
                                        selectedFilter = if (selectedQuadrantFilter == Quadrant.Q2_SCHEDULE) TaskFilterOption.Q2_SCHEDULE else TaskFilterOption.ALL
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MatrixTile(
                                    title = "Q3: Delegate",
                                    subtitle = "Urgent",
                                    count = q3Tasks.size,
                                    accentColor = Q3DelegateColor,
                                    isSelected = selectedQuadrantFilter == Quadrant.Q3_DELEGATE,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedQuadrantFilter = if (selectedQuadrantFilter == Quadrant.Q3_DELEGATE) null else Quadrant.Q3_DELEGATE
                                        selectedFilter = if (selectedQuadrantFilter == Quadrant.Q3_DELEGATE) TaskFilterOption.Q3_DELEGATE else TaskFilterOption.ALL
                                    }
                                )
                                MatrixTile(
                                    title = "Q4: Drop",
                                    subtitle = "Low Priority",
                                    count = q4Tasks.size,
                                    accentColor = Q4DropColor,
                                    isSelected = selectedQuadrantFilter == Quadrant.Q4_ELIMINATE,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedQuadrantFilter = if (selectedQuadrantFilter == Quadrant.Q4_ELIMINATE) null else Quadrant.Q4_ELIMINATE
                                        selectedFilter = if (selectedQuadrantFilter == Quadrant.Q4_ELIMINATE) TaskFilterOption.Q4_ELIMINATE else TaskFilterOption.ALL
                                    }
                                )
                            }
                    }
                }
            }
        }

        // 2. Inline Quick Add Task Bar
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_task_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickTaskTitle,
                        onValueChange = { quickTaskTitle = it },
                        placeholder = { Text("Add a daily task...", fontSize = 13.sp, color = lifeColors.textSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_task_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = lifeColors.borderGlass,
                            focusedBorderColor = lifeColors.primaryAccent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (quickTaskTitle.isNotBlank()) {
                                playAppleHaptic(context)
                                viewModel.addTask(
                                    title = quickTaskTitle.trim(),
                                    quadrant = Quadrant.Q1_DO,
                                    category = "Daily",
                                    dueTime = "Today",
                                    isTopThree = false
                                )
                                quickTaskTitle = ""
                            }
                        },
                        enabled = quickTaskTitle.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("quick_add_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add task", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 3. Filter Pills Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        label = "All (${allTasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.ALL,
                        onClick = {
                            selectedFilter = TaskFilterOption.ALL
                            selectedQuadrantFilter = null
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "Pending (${pendingTasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.PENDING,
                        onClick = {
                            selectedFilter = TaskFilterOption.PENDING
                            selectedQuadrantFilter = null
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "Completed (${completedTasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.COMPLETED,
                        onClick = {
                            selectedFilter = TaskFilterOption.COMPLETED
                            selectedQuadrantFilter = null
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "Q1 Do First (${q1Tasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.Q1_DO,
                        onClick = {
                            selectedFilter = TaskFilterOption.Q1_DO
                            selectedQuadrantFilter = Quadrant.Q1_DO
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "Q2 Schedule (${q2Tasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.Q2_SCHEDULE,
                        onClick = {
                            selectedFilter = TaskFilterOption.Q2_SCHEDULE
                            selectedQuadrantFilter = Quadrant.Q2_SCHEDULE
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "Q3 Delegate (${q3Tasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.Q3_DELEGATE,
                        onClick = {
                            selectedFilter = TaskFilterOption.Q3_DELEGATE
                            selectedQuadrantFilter = Quadrant.Q3_DELEGATE
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "Q4 Drop (${q4Tasks.size})",
                        isSelected = selectedFilter == TaskFilterOption.Q4_ELIMINATE,
                        onClick = {
                            selectedFilter = TaskFilterOption.Q4_ELIMINATE
                            selectedQuadrantFilter = Quadrant.Q4_ELIMINATE
                        }
                    )
                }
            }
        }

        // 4. Filtered Task Items List
        if (filteredTasks.isEmpty() && (selectedFilter != TaskFilterOption.ALL || completedTasks.isEmpty())) {
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
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = lifeColors.primaryAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when (selectedFilter) {
                                    TaskFilterOption.COMPLETED -> "No completed tasks yet"
                                    TaskFilterOption.PENDING -> "No pending tasks remaining"
                                    else -> "Zero tasks in this view"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = lifeColors.textPrimary
                            )
                            Text(
                                text = "Use the quick input above to create a task",
                                fontSize = 12.sp,
                                color = lifeColors.textSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                TaskGlassRow(
                    task = task,
                    onToggle = {
                        playAppleHaptic(context)
                        viewModel.toggleTaskDone(task)
                    },
                    onDelete = {
                        playAppleHaptic(context, strong = true)
                        viewModel.deleteTask(task)
                    },
                    onFocus = if (task.status == TaskStatus.PENDING) onStartFocus?.let { start -> { start(task) } } else null
                )
            }
        }

        // 5. Collapsible Completed Tasks Section (When viewing ALL or PENDING filter)
        if ((selectedFilter == TaskFilterOption.ALL || selectedFilter == TaskFilterOption.PENDING) && completedTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCompletedSection = !showCompletedSection }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RingHabitEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Completed (${completedTasks.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = lifeColors.textPrimary
                        )
                    }
                    Icon(
                        imageVector = if (showCompletedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle completed tasks",
                        tint = lifeColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showCompletedSection) {
                items(completedTasks, key = { "completed_${it.id}" }) { task ->
                    TaskGlassRow(
                        task = task,
                        onToggle = {
                            playAppleHaptic(context)
                            viewModel.toggleTaskDone(task)
                        },
                        onDelete = {
                            playAppleHaptic(context, strong = true)
                            viewModel.deleteTask(task)
                        }
                    )
                }
            }
        }
    }

    // Modal: Add New Task Dialog
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Academics") }
        var categoryDropdownExpanded by remember { mutableStateOf(false) }
        val categoryOptions = listOf("Academics", "Fitness", "Routine", "Projects", "Personal")
        var dueTime by remember { mutableStateOf("Today") }
        var selectedQ by remember { mutableStateOf(Quadrant.Q1_DO) }
        var isTopThree by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add New Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g., Complete OS Lab Assignment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_task_title_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                readOnly = false,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categoryOptions.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dueTime,
                            onValueChange = { dueTime = it },
                            label = { Text("Due Time") },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Text(
                        text = "Eisenhower Quadrant",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = lifeColors.textSecondary
                    )

                    // Quadrant radio chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Quadrant.values().forEach { quad ->
                            val isSel = (selectedQ == quad)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) lifeColors.primaryAccent.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { selectedQ = quad }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedQ = quad },
                                    colors = RadioButtonDefaults.colors(selectedColor = lifeColors.primaryAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = quad.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lifeColors.textPrimary
                                    )
                                    Text(
                                        text = quad.description,
                                        fontSize = 11.sp,
                                        color = lifeColors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTopThree,
                            onCheckedChange = { isTopThree = it },
                            colors = CheckboxDefaults.colors(checkedColor = lifeColors.primaryAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Feature in Today's Top 3 Focus",
                            fontSize = 13.sp,
                            color = lifeColors.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(
                                title = title.trim(),
                                quadrant = selectedQ,
                                category = category.trim(),
                                dueTime = dueTime.trim(),
                                isTopThree = isTopThree
                            )
                        }
                        showAddTaskDialog = false
                    },
                    modifier = Modifier.testTag("modal_create_task_button")
                ) {
                    Text("Create Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MatrixTile(
    title: String,
    subtitle: String,
    count: Int,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.22f)
                else lifeColors.surfaceGlassHighlight
            )
            .border(
                1.dp,
                if (isSelected) accentColor else lifeColors.borderGlass.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = "$count",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (count > 0) accentColor else lifeColors.textTertiary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = lifeColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = lifeColors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val lifeColors = LocalLifeOsColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) lifeColors.primaryAccent else lifeColors.surfaceGlass)
            .border(
                1.dp,
                if (isSelected) lifeColors.primaryAccent else lifeColors.borderGlass,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else lifeColors.textPrimary
        )
    }
}

