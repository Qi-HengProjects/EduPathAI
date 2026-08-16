package com.example.edupathai.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edupathai.data.ScheduleTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimelineScreen(
    viewModel: ScheduleViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Timeline Planner", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Time Block")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. ADHD Focus Timer Widget
            FocusTimerCard(
                isRunning = uiState.isTimerRunning,
                secondsRemaining = uiState.timerSecondsRemaining,
                activeTaskTitle = uiState.activeTimerTask?.title,
                onStart = { viewModel.startFocusTimer() },
                onPause = { viewModel.pauseFocusTimer() },
                onReset = { viewModel.resetFocusTimer(25) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Timeline Header & Energy Budget Summary
            EnergyBudgetBar(tasks = uiState.tasks)

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Task Timeline List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No study blocks scheduled today.\nTap '+' to create one!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.tasks, key = { it.id ?: it.title }) { task ->
                        TimelineTaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onStartFocus = { viewModel.startFocusTimer(task) },
                            onDelete = { task.id?.let { viewModel.deleteTask(it) } }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, desc, start, end, energy, color ->
                    viewModel.addTask(title, desc, start, end, energy, color)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun FocusTimerCard(
    isRunning: Boolean,
    secondsRemaining: Int,
    activeTaskTitle: String?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = activeTaskTitle ?: "Focus Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = if (isRunning) onPause else onStart) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Timer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Timer")
                }
            }
        }
    }
}

@Composable
fun EnergyBudgetBar(tasks: List<ScheduleTask>) {
    val highEnergyCount = tasks.count { it.energyLevel == "high" && !it.isCompleted }
    val medEnergyCount = tasks.count { it.energyLevel == "medium" && !it.isCompleted }
    val lowEnergyCount = tasks.count { it.energyLevel == "low" && !it.isCompleted }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Cognitive Load Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${highEnergyCount + medEnergyCount + lowEnergyCount} pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnergyBadge(
                    label = "High",
                    count = highEnergyCount,
                    color = Color(0xFFFF7675),
                    modifier = Modifier.weight(1f)
                )
                EnergyBadge(
                    label = "Med",
                    count = medEnergyCount,
                    color = Color(0xFF6C5CE7),
                    modifier = Modifier.weight(1f)
                )
                EnergyBadge(
                    label = "Low",
                    count = lowEnergyCount,
                    color = Color(0xFF00B894),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EnergyBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TimelineTaskCard(
    task: ScheduleTask,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit,
    onDelete: () -> Unit
) {
    val taskColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Color Block Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(taskColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onStartFocus) {
                Icon(Icons.Default.Timer, contentDescription = "Focus", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, start: String, end: String, energy: String, color: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var energyLevel by remember { mutableStateOf("medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Study Block") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title (e.g. Chapter 4 Practice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Energy Level required:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { level ->
                        FilterChip(
                            selected = energyLevel == level,
                            onClick = { energyLevel = level },
                            label = { Text(level.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val now = "2026-08-16T10:00:00Z"
                        val later = "2026-08-16T11:00:00Z"
                        val colorHex = when (energyLevel) {
                            "high" -> "#FF7675"
                            "medium" -> "#6C5CE7"
                            else -> "#00B894"
                        }
                        onConfirm(title.trim(), desc.trim(), now, later, energyLevel, colorHex)
                    }
                }
            ) {
                Text("Add Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}