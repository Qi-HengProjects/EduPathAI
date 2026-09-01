package com.example.edupathai.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
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
fun SettingsScreen(
    userId: String,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var profile by remember { mutableStateOf<ProfileModel?>(null) }
    var isProfileLoading by remember { mutableStateOf(true) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var editUsername by rememberSaveable { mutableStateOf("") }
    var editBio by rememberSaveable { mutableStateOf("") }

    fun loadProfile() {
        coroutineScope.launch {
            isProfileLoading = true
            val currentUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: userId
            val fetched = withContext(Dispatchers.IO) {
                try {
                    SupabaseProvider.client.from("profiles").select {
                        filter { eq("id", currentUserId) }
                    }.decodeSingleOrNull<ProfileModel>()
                } catch (_: Exception) {
                    null
                }
            }
            val user = fetched ?: ProfileModel(
                id = currentUserId,
                username = SupabaseProvider.client.auth.currentUserOrNull()?.email?.substringBefore("@") ?: "Student",
                email = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "student@university.edu"
            )
            profile = user
            editUsername = user.username
            editBio = user.bio ?: ""
            isProfileLoading = false
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
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
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
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF3B82F6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF38BDF8))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = profile?.username ?: "Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = profile?.email ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }

                        IconButton(
                            onClick = {
                                if (profile != null) {
                                    editUsername = profile?.username ?: ""
                                    editBio = profile?.bio ?: ""
                                    showEditProfileDialog = true
                                }
                            },
                            enabled = !isProfileLoading
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = if (isProfileLoading) Color(0xFF64748B) else Color(0xFF38BDF8))
                        }
                    }
                }
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Info,
                    title = "About EduPath AI",
                    value = "Version 1.0.0",
                    onClick = { Toast.makeText(context, "EduPath AI • Powered by Gemini AI", Toast.LENGTH_SHORT).show() }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { onLoggedOut() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                containerColor = Color(0xFF131C2E),
                title = { Text("Edit Student Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it },
                            label = { Text("Username") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio / Academic Focus") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val currentUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: userId
                                    val safeEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email
                                        ?: profile?.email
                                        ?: ""

                                    val updated = ProfileModel(
                                        id = currentUserId,
                                        username = editUsername.trim().ifBlank { profile?.username ?: "Student" },
                                        email = safeEmail,
                                        bio = editBio.trim()
                                    )
                                    withContext(Dispatchers.IO) {
                                        SupabaseProvider.client.from("profiles").upsert(updated)
                                    }
                                    profile = updated
                                    showEditProfileDialog = false
                                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
            }
        }
    }
}