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
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var profile by remember { mutableStateOf<ProfileModel?>(null) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var editUsername by rememberSaveable { mutableStateOf("") }
    var editBio by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(userId) {
        val fetched = withContext(Dispatchers.IO) {
            try {
                SupabaseProvider.client.from("profiles").select {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<ProfileModel>()
            } catch (_: Exception) {
                null
            }
        }
        val user = fetched ?: ProfileModel(
            id = userId,
            username = "Student",
            email = "student@edupath.ai",
            bio = "Computer Science Student"
        )
        profile = user
        editUsername = user.username
        editBio = user.bio ?: ""
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
                            Text(text = profile?.bio ?: "Study Profile", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }

                        IconButton(
                            onClick = {
                                editUsername = profile?.username ?: "Student"
                                editBio = profile?.bio ?: ""
                                showEditProfileDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF38BDF8))
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
                                val updated = ProfileModel(
                                    id = userId,
                                    username = editUsername.trim().ifBlank { "Student" },
                                    email = profile?.email ?: "student@edupath.ai",
                                    bio = editBio.trim()
                                )
                                withContext(Dispatchers.IO) {
                                    try {
                                        SupabaseProvider.client.from("profiles").upsert(updated)
                                    } catch (_: Exception) {}
                                }
                                profile = updated
                                showEditProfileDialog = false
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
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