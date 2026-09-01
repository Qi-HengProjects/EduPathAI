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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.ProfileModel
import com.example.edupathai.data.SupabaseProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userId: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadProfile() {
        coroutineScope.launch {
            isLoading = true
            try {
                val currentUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: userId
                val fetched = withContext(Dispatchers.IO) {
                    try {
                        SupabaseProvider.client.from("profiles").select {
                            filter { eq("id", currentUserId) }
                        }.decodeList<ProfileModel>().firstOrNull()
                    } catch (_: Exception) {
                        null
                    }
                }
                profile = fetched ?: ProfileModel(
                    id = currentUserId,
                    username = SupabaseProvider.client.auth.currentUserOrNull()?.email?.substringBefore("@") ?: "Student",
                    email = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "student@university.edu"
                )
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        loadProfile()
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F19)),
                title = {
                    Column {
                        Text(
                            text = "Student Dashboard",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Overview & Academic Suite",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadProfile() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF94A3B8))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF94A3B8))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Pill Badge Header
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EduPath AI • Academic Workspace",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // User Identity Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                .border(1.5.dp, Color(0xFF3B82F6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.username ?: "Student",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Navigation Action Cards (Notebooks & Timeline are now fully functional)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardItem(
                        icon = Icons.Default.MenuBook,
                        title = "Notebooks",
                        subtitle = "Active Subjects",
                        accent = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToNotes
                    )
                    StatCardItem(
                        icon = Icons.Default.CalendarMonth,
                        title = "Timeline",
                        subtitle = "Daily Schedule",
                        accent = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSchedule
                    )
                }
            }
        }
    }
}

@Composable
fun StatCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        modifier = modifier
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            }
        }
    }
}