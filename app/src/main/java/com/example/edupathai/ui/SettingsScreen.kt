package com.example.edupathai.ui

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

    var bio by remember { mutableStateOf("") }
    var focusMode by remember { mutableStateOf(false) }
    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        try {
            val p = SupabaseProvider.db.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserProfile>()
            bio = p.bio
            focusMode = p.focusModeEnabled
            voiceSpeed = p.aiVoiceSpeed
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Accessibility") },
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // UPDATE: Edit Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Edit Bio") },
                    modifier = Modifier.fillMaxWidth()
                )

                // UPDATE: Toggle Focus Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Focus Mode", fontWeight = FontWeight.SemiBold)
                        Text("Minimizes UI animations and distractions", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = focusMode,
                        onCheckedChange = { focusMode = it }
                    )
                }

                // UPDATE: AI Voice Speed
                Column {
                    Text("AI Voice Speed: ${"%.1f".format(voiceSpeed)}x", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = voiceSpeed,
                        onValueChange = { voiceSpeed = it },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                SupabaseProvider.db.from("profiles").update({
                                    set("bio", bio)
                                    set("focus_mode_enabled", focusMode)
                                    set("ai_voice_speed", voiceSpeed)
                                }) {
                                    filter { eq("id", userId) }
                                }
                                Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            SupabaseProvider.auth.signOut()
                            onLoggedOut()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log Out")
                }

                // DELETE: Delete account profile
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                SupabaseProvider.db.from("profiles").delete {
                                    filter { eq("id", userId) }
                                }
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