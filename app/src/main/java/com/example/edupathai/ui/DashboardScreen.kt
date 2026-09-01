package com.example.edupathai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.ProfileModel
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import com.example.edupathai.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    userId: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileModel?>(null) }
    var todayTasks by remember { mutableStateOf<List<ScheduleTask>>(emptyList()) }
    var folderCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val scheduleRepository = remember { ScheduleRepository() }
    val noteRepository = remember { NoteRepository() }

    LaunchedEffect(userId) {
        coroutineScope.launch {
            isLoading = true
            val fetchedProfile = withContext(Dispatchers.IO) {
                try {
                    SupabaseProvider.client.from("profiles").select {
                        filter { eq("id", userId) }
                    }.decodeSingleOrNull<ProfileModel>()
                } catch (_: Exception) {
                    null
                }
            }
            profile = fetchedProfile ?: ProfileModel(id = userId, username = "Student", email = "student@edupath.ai")

            val allTasks = scheduleRepository.getTasks()
            val todayStr = LocalDate.now().toString()
            todayTasks = allTasks.filter { it.effectiveDate == todayStr }

            val folders = noteRepository.getFolders()
            folderCount = folders.size

            isLoading = false
        }
    }

    val completedCount = todayTasks.count { it.isCompleted }
    val overdueCount = todayTasks.count { it.isOverdue }
    val totalCount = todayTasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // User Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${profile?.username ?: "Student"} 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF131C2E), CircleShape)
                        .border(1.dp, Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF94A3B8))
                }
            }
        }

        // Daily Progress Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSchedule() }
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Today's Study Progress", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (overdueCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "$overdueCount Overdue",
                                        color = Color(0xFFEF4444),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "$completedCount/$totalCount Done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalCount > 0 && completedCount == totalCount) Color(0xFF10B981) else Color(0xFF38BDF8)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }

        // Quick Overview Metric Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToNotes() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$folderCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Subject Folders", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToSchedule() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${todayTasks.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Tasks Today", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        // Today's Scheduled Tasks Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Today's Timeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                TextButton(onClick = onNavigateToSchedule) {
                    Text("View Full Schedule", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (todayTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.EventNote, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No tasks scheduled for today", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(todayTasks) { task ->
                val cardBorder = when {
                    task.isCompleted -> Color(0xFF1E293B)
                    task.isOverdue -> Color(0xFFEF4444).copy(alpha = 0.5f)
                    else -> Color(0xFF1E293B)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (task.isCompleted) Color(0xFF10B981) else if (task.isOverdue) Color(0xFFEF4444) else Color(0xFF38BDF8))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) Color(0xFF64748B) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${task.startTime} - ${task.endTime}",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (task.isOverdue) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Overdue",
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}