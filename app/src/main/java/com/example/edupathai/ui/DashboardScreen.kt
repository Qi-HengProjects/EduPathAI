package com.example.edupathai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.ProfileModel
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.ui.schedule.ScheduleViewModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    userId: String,
    scheduleViewModel: ScheduleViewModel? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileModel?>(null) }
    var folderCount by remember { mutableStateOf(0) }
    val noteRepository = remember { NoteRepository() }

    LaunchedEffect(userId) {
        coroutineScope.launch {
            val currentAuth = try {
                SupabaseProvider.client.auth.currentUserOrNull()
            } catch (_: Exception) {
                null
            }
            val activeId = currentAuth?.id ?: userId

            val fetchedProfile = withContext(Dispatchers.IO) {
                try {
                    SupabaseProvider.client.from("profiles").select {
                        filter { eq("id", activeId) }
                    }.decodeSingleOrNull<ProfileModel>()
                } catch (_: Exception) {
                    null
                }
            }

            val fallbackUsername = currentAuth?.email?.substringBefore("@") ?: "Student"

            profile = fetchedProfile ?: ProfileModel(
                id = activeId,
                username = fallbackUsername,
                email = currentAuth?.email ?: "student@edupath.ai"
            )

            val folders = noteRepository.getFolders()
            folderCount = folders.size
        }
    }

    val displayUsername = profile?.username?.ifBlank { null }
        ?: try {
            SupabaseProvider.client.auth.currentUserOrNull()?.email?.substringBefore("@")
        } catch (_: Exception) {
            null
        }
        ?: "Student"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .statusBarsPadding()
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
                        text = "Hello, $displayUsername 👋",
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "$folderCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Subject Notebooks", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Daily Schedule", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        // Quick Action Cards
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .clickable { onNavigateToNotes() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "AI Study Assistant", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Generate summaries, flashcards & mindmaps", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSchedule() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Smart Study Schedule", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Plan daily goals & check overdue tasks", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
                }
            }
        }
    }
}