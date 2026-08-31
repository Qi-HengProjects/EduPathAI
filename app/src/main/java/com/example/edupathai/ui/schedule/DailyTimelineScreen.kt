package com.example.edupathai.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.ScheduleTask
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTimelineScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }

    val calendarListState = rememberLazyListState()

    // Generate all days in the currently viewed month (1..28/30/31)
    val daysInMonth = remember(currentYearMonth) {
        val count = currentYearMonth.lengthOfMonth()
        (1..count).map { currentYearMonth.atDay(it) }
    }

    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy") }
    val monthHeaderFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }

    // Auto-scroll calendar strip to the active selected date
    LaunchedEffect(currentYearMonth, selectedDate) {
        val targetIndex = (selectedDate.dayOfMonth - 1).coerceAtLeast(0)
        if (selectedDate.month == currentYearMonth.month && selectedDate.year == currentYearMonth.year) {
            calendarListState.animateScrollToItem(targetIndex)
        }
    }

    // Refresh schedule data on launch
    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text(
                            text = "Schedule & Timeline",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = selectedDate.format(fullDateFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTasks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Schedule")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Header with Navigation Controls
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentYearMonth.format(monthHeaderFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // "Today" shortcut button if user browses away from current month
                            if (currentYearMonth != YearMonth.from(today) || selectedDate != today) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable {
                                        currentYearMonth = YearMonth.from(today)
                                        selectedDate = today
                                    }
                                ) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Previous & Next Month Navigation
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val prev = currentYearMonth.minusMonths(1)
                                    currentYearMonth = prev
                                    selectedDate = prev.atDay(1)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val next = currentYearMonth.plusMonths(1)
                                    currentYearMonth = next
                                    selectedDate = next.atDay(1)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Monthly Day Strip (1..31)
                    LazyRow(
                        state = calendarListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(daysInMonth, key = { it.toString() }) { date ->
                            val isSelected = date == selectedDate
                            val isCurrentToday = date == today
                            val dayOfWeekStr = date.format(DateTimeFormatter.ofPattern("EEE")).uppercase()
                            val dayNumStr = date.dayOfMonth.toString()

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(72.dp)
                                    .clickable { selectedDate = date }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayOfWeekStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = dayNumStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Dot indicator for today's date
                                    if (isCurrentToday && !isSelected) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Timeline Tasks Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (uiState.isLoading && uiState.tasks.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.tasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No Tasks for This Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ask AI in the Chat tab to plan a study block or tap 'Schedule' under any note.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.loadTasks() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Timeline")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        // Study Progress Card
                        item {
                            val completedCount = uiState.tasks.count { it.isCompleted }
                            val progress = if (uiState.tasks.isNotEmpty()) completedCount.toFloat() / uiState.tasks.size else 0f

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Study Progress",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "$completedCount / ${uiState.tasks.size} Done",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }

                        // Task List
                        items(uiState.tasks, key = { it.id ?: (it.title + it.startTime) }) { task ->
                            TimelineTaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { task.id?.let { viewModel.deleteTask(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTaskCard(
    task: ScheduleTask,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (task.isCompleted) Color.Transparent else tagColor.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onToggleComplete() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tagColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⏰ ${task.startTime} - ${task.endTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (task.energyLevel.isNotBlank()) {
                            val energyBadge = when (task.energyLevel.lowercase()) {
                                "high" -> "🔥 High"
                                "low" -> "🌱 Low"
                                else -> "⚡ Med"
                            }
                            Text(
                                text = energyBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}