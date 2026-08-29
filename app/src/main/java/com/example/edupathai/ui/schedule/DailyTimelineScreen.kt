package com.example.edupathai.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.ScheduleTask
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimelineScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (uiState.activeTabIndex) {
                                0 -> "All Tasks"
                                1 -> "Daily Timeline"
                                else -> "Monthly Calendar"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )

                // 3 TABS: All Tasks (Left) | Daily Timeline | Monthly Calendar
                TabRow(
                    selectedTabIndex = uiState.activeTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = uiState.activeTabIndex == 0,
                        onClick = { viewModel.setTab(0) },
                        text = { Text("All Tasks", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = uiState.activeTabIndex == 1,
                        onClick = { viewModel.setTab(1) },
                        text = { Text("Daily", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = uiState.activeTabIndex == 2,
                        onClick = { viewModel.setTab(2) },
                        text = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.activeTabIndex) {
                0 -> {
                    // TAB 0: ALL TASKS (Shows every task in Supabase)
                    AllTasksContent(
                        tasks = uiState.tasks,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
                1 -> {
                    // TAB 1: DAILY TIMELINE (Filtered strictly for today's date)
                    val today = LocalDate.now()
                    val todayTasks = uiState.tasks.filter { task ->
                        val taskDate = extractTaskDate(task)
                        taskDate == null || taskDate == today
                    }

                    DailyTimelineContent(
                        today = today,
                        tasks = todayTasks,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
                2 -> {
                    // TAB 2: MONTHLY CALENDAR (Filtered strictly for the selected date)
                    val calendarTasks = uiState.tasks.filter { task ->
                        extractTaskDate(task) == uiState.selectedDate
                    }

                    MonthlyCalendarContent(
                        selectedDate = uiState.selectedDate,
                        tasks = calendarTasks,
                        onDateSelect = { viewModel.setSelectedDate(it) },
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, start, end, energy ->
                viewModel.addTask(
                    ScheduleTask(
                        title = title,
                        startTime = "${LocalDate.now()}T$start:00",
                        endTime = "${LocalDate.now()}T$end:00",
                        energyLevel = energy
                    )
                )
                showAddTaskDialog = false
            }
        )
    }
}

// ----------------------------------------------------------------
// TAB 0: ALL TASKS VIEW
// ----------------------------------------------------------------
@Composable
fun AllTasksContent(
    tasks: List<ScheduleTask>,
    onToggleTask: (ScheduleTask) -> Unit,
    onDeleteTask: (ScheduleTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Total Scheduled Tasks (${tasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (tasks.isEmpty()) {
            item {
                EmptyStateCard(message = "No tasks found. Tap '+' to create your first task.")
            }
        } else {
            items(tasks) { task ->
                TaskItemCard(
                    task = task,
                    showDateBadge = true,
                    onToggle = { onToggleTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

// ----------------------------------------------------------------
// TAB 1: DAILY TIMELINE VIEW (Filtered by Today)
// ----------------------------------------------------------------
@Composable
fun DailyTimelineContent(
    today: LocalDate,
    tasks: List<ScheduleTask>,
    onToggleTask: (ScheduleTask) -> Unit,
    onDeleteTask: (ScheduleTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cognitive Load Summary for today
        item {
            val high = tasks.count { it.energyLevel == "high" }
            val med = tasks.count { it.energyLevel == "medium" }
            val low = tasks.count { it.energyLevel == "low" }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    EnergyBadge(label = "High Load", count = high, color = Color(0xFFEF4444))
                    EnergyBadge(label = "Medium", count = med, color = Color(0xFF8B5CF6))
                    EnergyBadge(label = "Low", count = low, color = Color(0xFF10B981))
                }
            }
        }

        item {
            Text(
                text = "Today's Schedule • ${today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (tasks.isEmpty()) {
            item {
                EmptyStateCard(message = "No study blocks scheduled for today.\nTap '+' or schedule from Notes / AI Chat!")
            }
        } else {
            items(tasks) { task ->
                TaskItemCard(
                    task = task,
                    showDateBadge = false,
                    onToggle = { onToggleTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

// ----------------------------------------------------------------
// TAB 2: MONTHLY CALENDAR VIEW
// ----------------------------------------------------------------
@Composable
fun MonthlyCalendarContent(
    selectedDate: LocalDate,
    tasks: List<ScheduleTask>,
    onDateSelect: (LocalDate) -> Unit,
    onToggleTask: (ScheduleTask) -> Unit,
    onDeleteTask: (ScheduleTask) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar Grid Card
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalCells = firstDayOfWeek + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                for (col in 0..6) {
                                    val dayIndex = row * 7 + col - firstDayOfWeek + 1
                                    if (dayIndex in 1..daysInMonth) {
                                        val date = currentMonth.atDay(dayIndex)
                                        val isSelected = date == selectedDate
                                        val isToday = date == LocalDate.now()

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable { onDateSelect(date) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayIndex",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(36.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Tasks for ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (tasks.isEmpty()) {
            item {
                EmptyStateCard(message = "No tasks scheduled for this day.")
            }
        } else {
            items(tasks) { task ->
                TaskItemCard(
                    task = task,
                    showDateBadge = false,
                    onToggle = { onToggleTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

// ----------------------------------------------------------------
// TASK CARD COMPONENT (Clean Shorter Time Format)
// ----------------------------------------------------------------
@Composable
fun TaskItemCard(
    task: ScheduleTask,
    showDateBadge: Boolean = false,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val shortStart = formatShortTime(task.startTime)
    val shortEnd = formatShortTime(task.endTime)
    val dateText = extractTaskDate(task)?.format(DateTimeFormatter.ofPattern("MMM d"))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                // Clean short time display
                val timeLabel = buildString {
                    if (showDateBadge && dateText != null) append("$dateText • ")
                    if (shortStart.isNotBlank() && shortEnd.isNotBlank()) {
                        append("$shortStart - $shortEnd")
                    } else if (shortStart.isNotBlank()) {
                        append(shortStart)
                    } else {
                        append("Scheduled")
                    }
                    append(" • ${task.energyLevel.replaceFirstChar { it.uppercase() }} energy")
                }

                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------
// HELPER FUNCTIONS (Formatting & Parsing)
// ----------------------------------------------------------------

/**
 * Extracts clean HH:mm (e.g. 10:00) from long strings like "2026-08-16T10:00:00+00:00"
 */
fun formatShortTime(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        if (raw.contains("T")) {
            val timePart = raw.substringAfter("T").substringBefore("+").substringBefore("Z")
            timePart.take(5) // Returns "10:00"
        } else if (raw.length >= 5 && raw.contains(":")) {
            raw.take(5)
        } else {
            raw
        }
    } catch (_: Exception) {
        raw
    }
}

/**
 * Extracts LocalDate from startTime or createdAt
 */
fun extractTaskDate(task: ScheduleTask): LocalDate? {
    val start = task.startTime
    if (start.contains("T") && start.length >= 10) {
        return try { LocalDate.parse(start.substring(0, 10)) } catch (_: Exception) { null }
    }
    if (start.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*"))) {
        return try { LocalDate.parse(start.take(10)) } catch (_: Exception) { null }
    }
    task.createdAt?.let { created ->
        if (created.length >= 10) {
            return try { LocalDate.parse(created.take(10)) } catch (_: Exception) { null }
        }
    }
    return null
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EnergyBadge(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$label: $count", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, start: String, end: String, energy: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("10:00") }
    var energy by remember { mutableStateOf("medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Study Block", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onAdd(title, start, end, energy) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}