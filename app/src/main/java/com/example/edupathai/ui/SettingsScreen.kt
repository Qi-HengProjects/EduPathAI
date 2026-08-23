package com.example.edupathai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.data.UserProfile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("edupath_auth_prefs", Context.MODE_PRIVATE) }

    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var focusMode by remember { mutableStateOf(false) }
    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        try {
            val p = SupabaseProvider.db.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserProfile>()
            fullName = p.fullName
            bio = p.bio
            focusMode = p.focusModeEnabled
            voiceSpeed = p.aiVoiceSpeed
            currentEmail = p.email
        } catch (_: Exception) {
            currentEmail = SupabaseProvider.auth.currentUserOrNull()?.email ?: ""
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Bio Input
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Edit Bio") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Focus Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Focus Mode", fontWeight = FontWeight.SemiBold)
                        Text("Minimizes UI animations and distractions", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = focusMode,
                        onCheckedChange = { focusMode = it }
                    )
                }

                // AI Voice Speed Slider
                Column {
                    Text("AI Voice Speed: ${"%.1f".format(voiceSpeed)}x", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = voiceSpeed,
                        onValueChange = { voiceSpeed = it },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                }

                // Save Profile Changes
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val emailToSave = if (currentEmail.isNotBlank()) {
                                    currentEmail
                                } else {
                                    SupabaseProvider.auth.currentUserOrNull()?.email ?: ""
                                }
                                val updatedProfile = UserProfile(
                                    id = userId,
                                    email = emailToSave,
                                    fullName = fullName,
                                    bio = bio,
                                    focusModeEnabled = focusMode,
                                    aiVoiceSpeed = voiceSpeed
                                )
                                SupabaseProvider.db.from("profiles").upsert(updatedProfile)
                                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Switch Account (Clears saved Remember Me inputs & signs out)
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            prefs.edit().clear().apply()
                            SupabaseProvider.auth.signOut()
                            Toast.makeText(context, "Ready to switch account", Toast.LENGTH_SHORT).show()
                            onLoggedOut()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch Account")
                }

                // Sign Out (Keeps Remember Me inputs for fast login next time)
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            SupabaseProvider.auth.signOut()
                            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                            onLoggedOut()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }

                // Delete Account Record
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                SupabaseProvider.db.from("profiles").delete {
                                    filter { eq("id", userId) }
                                }
                                prefs.edit().clear().apply()
                                SupabaseProvider.auth.signOut()
                                Toast.makeText(context, "Account data deleted", Toast.LENGTH_SHORT).show()
                                onLoggedOut()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Account Profile")
                }
            }
        }
    }
}