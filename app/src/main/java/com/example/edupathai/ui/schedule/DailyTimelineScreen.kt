package com.example.edupathai.ui.schedule

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.ScheduleTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimelineScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var displayedMonth by remember { mutableStateOf(YearMonth.from(uiState.selectedDate)) }
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearNotification()
        }
    }

    val selectedDateString = uiState.selectedDate.toString()
    val tasksForSelectedDate = remember(uiState.tasks, selectedDateString) {
        uiState.tasks.filter { it.effectiveDate == selectedDateString }
    }

    val completedCount = tasksForSelectedDate.count { it.isCompleted }
    val overdueCount = tasksForSelectedDate.count { it.isOverdue }
    val totalCount = tasksForSelectedDate.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    val datesWithTasks = remember(uiState.tasks) {
        uiState.tasks.map { it.effectiveDate }.toSet()
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F19)),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Daily Schedule & Timeline",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Smart calendar & study blocks",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTasks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF94A3B8))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0B0F19)),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Calendar Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month Header Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFF94A3B8))
                                }
                                IconButton(
                                    onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF94A3B8))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weekday Names Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                                Text(
                                    text = day,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Grid
                        val firstDayOfMonth = displayedMonth.atDay(1)
                        val daysInMonth = displayedMonth.lengthOfMonth()
                        val startOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
                        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (week in 0 until totalCells / 7) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (dayOfWeek in 0..6) {
                                        val cellIndex = week * 7 + dayOfWeek
                                        val dayNumber = cellIndex - startOffset + 1

                                        if (dayNumber in 1..daysInMonth) {
                                            val cellDate = displayedMonth.atDay(dayNumber)
                                            val isSelected = cellDate == uiState.selectedDate
                                            val isToday = cellDate == LocalDate.now()
                                            val hasTasks = datesWithTasks.contains(cellDate.toString())

                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> Color(0xFF3B82F6)
                                                            isToday -> Color(0xFF1E293B)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                        color = if (isToday && !isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { viewModel.selectDate(cellDate) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = dayNumber.toString(),
                                                        color = if (isSelected) Color.White else if (isToday) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (hasTasks && !isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF38BDF8))
                                                        )
                                                    }
                                                }
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

            // Daily Progress & Status Card (with Overdue Indicator)
            item {
                val isToday = uiState.selectedDate == LocalDate.now()
                val dayOfWeek = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE"))
                val formattedDate = uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Section: Date & Optional "Today" Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (isToday) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                                    ) {
                                        Text(
                                            text = "Today",
                                            color = Color(0xFF38BDF8),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "$dayOfWeek, $formattedDate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right Section: Completion and Overdue Badges
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (overdueCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "$overdueCount Overdue",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (totalCount > 0 && completedCount == totalCount) Color(0xFF10B981) else Color(0xFF334155)
                                    )
                                ) {
                                    Text(
                                        text = "$completedCount/$totalCount Completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (totalCount > 0 && completedCount == totalCount) Color(0xFF10B981) else Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Task Items Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Tasks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${tasksForSelectedDate.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Filtered Tasks for Selected Date
            if (tasksForSelectedDate.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8).copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Tasks for this Day",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap the + button to schedule study sessions or goals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(tasksForSelectedDate, key = { it.id ?: (it.title + it.startTime) }) { task ->
                    TaskTimelineItemCard(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                        onDelete = { task.id?.let { viewModel.deleteTask(it) } }
                    )
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                selectedDate = uiState.selectedDate,
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { title, start, end, energy, type, colorHex ->
                    viewModel.createTask(
                        title = title,
                        startTime = start,
                        endTime = end,
                        energyLevel = energy,
                        taskType = type,
                        colorHex = colorHex,
                        date = uiState.selectedDate
                    )
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun TaskTimelineItemCard(
    task: ScheduleTask,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val baseColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }

    val borderColor = when {
        task.isCompleted -> Color(0xFF1E293B)
        task.isOverdue -> Color(0xFFEF4444).copy(alpha = 0.6f)
        else -> baseColor.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = if (task.isOverdue) Color(0xFFEF4444) else Color(0xFF64748B),
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        task.isCompleted -> Color(0xFF64748B)
                        task.isOverdue -> Color(0xFFFCA5A5)
                        else -> Color.White
                    },
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Overdue Alert Badge
                    if (task.isOverdue) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "⚠️ Overdue",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Time Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "${task.startTime} - ${task.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Energy Level Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = baseColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = task.energyLevel,
                            style = MaterialTheme.typography.labelSmall,
                            color = baseColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, start: String, end: String, energy: String, type: String, colorHex: String) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = LocalTime.now()
    val defaultStart = now.format(timeFormatter)
    val defaultEnd = now.plusMinutes(60).format(timeFormatter)

    var taskTitle by rememberSaveable { mutableStateOf("") }
    var startTime by rememberSaveable { mutableStateOf(defaultStart) }
    var endTime by rememberSaveable { mutableStateOf(defaultEnd) }
    var selectedEnergy by rememberSaveable { mutableStateOf("Medium") }
    var selectedType by rememberSaveable { mutableStateOf("study") }
    var selectedColor by rememberSaveable { mutableStateOf("#3B82F6") }

    val energyOptions = listOf("High" to "🔥 High", "Medium" to "⚡ Medium", "Low" to "🌱 Low")
    val typeOptions = listOf("study" to "Study", "revision" to "Revision", "assignment" to "Assignment", "quiz" to "Quiz")
    val taskColors = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131C2E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule Study Task", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Scheduled for: ${selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g., Computer Science Revision") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("14:00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("15:00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Energy Required", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(energyOptions) { (key, label) ->
                        FilterChip(
                            selected = selectedEnergy == key,
                            onClick = { selectedEnergy = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Color Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    taskColors.forEach { hex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Color(0xFF3B82F6)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .clickable { selectedColor = hex }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskTitle.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                        onConfirm(taskTitle.trim(), startTime.trim(), endTime.trim(), selectedEnergy, selectedType, selectedColor)
                    }
                },
                enabled = taskTitle.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Add Task", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}