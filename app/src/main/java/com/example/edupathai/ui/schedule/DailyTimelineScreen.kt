package com.example.edupathai.ui.schedule

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.ScheduleTask
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun isTaskOverdue(task: ScheduleTask, taskDate: LocalDate): Boolean {
    if (task.isCompleted) return false

    val now = LocalDateTime.now()
    val start = parseTimeToLocalTime(task.startTime)
    val end = parseTimeToLocalTime(task.endTime)

    val taskEndDateTime = if (end.isBefore(start) || end == start) {
        LocalDateTime.of(taskDate.plusDays(1), end)
    } else {
        LocalDateTime.of(taskDate, end)
    }

    return now.isAfter(taskEndDateTime)
}

fun parseTimeToLocalTime(timeStr: String): LocalTime {
    val clean = timeStr.trim()
    return try {
        when {
            clean.length == 5 -> LocalTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm"))
            clean.length == 4 && clean.contains(":") -> LocalTime.parse("0$clean", DateTimeFormatter.ofPattern("HH:mm"))
            clean.contains("AM", ignoreCase = true) || clean.contains("PM", ignoreCase = true) -> {
                LocalTime.parse(clean.uppercase(Locale.US), DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
            }
            else -> LocalTime.parse(clean.take(5))
        }
    } catch (_: Exception) {
        LocalTime.of(23, 59)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyTimelineScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var currentMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showCompletedSection by rememberSaveable { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    val tasksForSelectedDate = remember(uiState.tasks, selectedDate) {
        uiState.tasks.filter { task ->
            val taskDateStr = task.createdAt?.take(10)
            taskDateStr == null || taskDateStr.isBlank() || taskDateStr == selectedDate.toString()
        }
    }

    val activeTasks = tasksForSelectedDate.filter { !it.isCompleted }
    val completedTasks = tasksForSelectedDate.filter { it.isCompleted }
    val overdueCount = activeTasks.count { isTaskOverdue(it, selectedDate) }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131C2E)),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                title = {
                    Column {
                        Text("Daily Schedule & Timeline", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Smart calendar & study blocks", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Calendar Card
            item {
                CalendarMonthCard(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onMonthChange = { currentMonth = it },
                    onDateSelect = { selectedDate = it }
                )
            }

            // Clean Summary Header Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedDate == LocalDate.now()) {
                                Surface(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Today",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (overdueCount > 0) {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "$overdueCount Overdue",
                                        color = Color(0xFFF87171),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${completedTasks.size}/${tasksForSelectedDate.size} Completed",
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Tasks Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Active Tasks",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        "${activeTasks.size} pending",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            if (activeTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active tasks for this day.", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                }
            } else {
                items(activeTasks, key = { it.id ?: it.title }) { task ->
                    val isOverdue = isTaskOverdue(task, selectedDate)
                    ScheduleTaskCard(
                        task = task,
                        isOverdue = isOverdue,
                        onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                        onDelete = { task.id?.let { viewModel.deleteTask(it) } }
                    )
                }
            }

            // Completed Section Header
            if (completedTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompletedSection = !showCompletedSection }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showCompletedSection) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF34D399)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Past / Completed Tasks (${completedTasks.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399),
                            fontSize = 14.sp
                        )
                    }
                }

                if (showCompletedSection) {
                    items(completedTasks, key = { it.id ?: it.title }) { task ->
                        ScheduleTaskCard(
                            task = task,
                            isOverdue = false,
                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                            onDelete = { task.id?.let { viewModel.deleteTask(it) } }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }

        // Add Task Dialog
        if (showAddTaskDialog) {
            var title by rememberSaveable { mutableStateOf("") }
            var startTime by rememberSaveable { mutableStateOf("09:00") }
            var endTime by rememberSaveable { mutableStateOf("10:00") }
            var energyLevel by rememberSaveable { mutableStateOf("medium") }
            var taskType by rememberSaveable { mutableStateOf("study") }

            AlertDialog(
                onDismissRequest = { showAddTaskDialog = false },
                containerColor = Color(0xFF131C2E),
                title = { Text("Add Schedule Block", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val t = parseTimeToLocalTime(startTime)
                                    TimePickerDialog(context, { _, hour, minute ->
                                        startTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                                    }, t.hour, t.minute, true).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text("Start: $startTime", color = Color.White, fontSize = 12.sp, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    val t = parseTimeToLocalTime(endTime)
                                    TimePickerDialog(context, { _, hour, minute ->
                                        endTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                                    }, t.hour, t.minute, true).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text("End: $endTime", color = Color.White, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("low", "medium", "high").forEach { level ->
                                FilterChip(
                                    selected = energyLevel == level,
                                    onClick = { energyLevel = level },
                                    label = { Text(level.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.createTask(
                                    title = title.trim(),
                                    startTime = startTime,
                                    endTime = endTime,
                                    energyLevel = energyLevel,
                                    taskType = taskType,
                                    colorHex = if (energyLevel == "high") "#EF4444" else if (energyLevel == "medium") "#3B82F6" else "#10B981",
                                    date = selectedDate
                                )
                                showAddTaskDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Add Task", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTaskDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleTaskCard(
    task: ScheduleTask,
    isOverdue: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when {
        task.isCompleted -> Color(0xFF10B981).copy(alpha = 0.3f)
        isOverdue -> Color(0xFFEF4444).copy(alpha = 0.5f)
        else -> Color(0xFF1E293B)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = if (isOverdue) Color(0xFFEF4444) else Color(0xFF94A3B8)
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) Color(0xFF64748B) else Color.White,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isOverdue) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Overdue",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${task.startTime} - ${task.endTime}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.energyLevel.lowercase(),
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CalendarMonthCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelect: (LocalDate) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Row {
                    IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", tint = Color.White)
                    }
                    IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(day, color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstDayOfMonth = currentMonth.atDay(1)
            val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7
            val daysInMonth = currentMonth.lengthOfMonth()

            val totalSlots = dayOfWeekOffset + daysInMonth
            val rows = (totalSlots + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIndex in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        for (colIndex in 0 until 7) {
                            val slotIndex = rowIndex * 7 + colIndex
                            val dayNumber = slotIndex - dayOfWeekOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNumber)
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> Color(0xFF3B82F6)
                                                isToday -> Color(0xFF1E293B)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelect(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        color = if (isSelected) Color.White else if (isToday) Color(0xFF60A5FA) else Color(0xFFE2E8F0),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(34.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}