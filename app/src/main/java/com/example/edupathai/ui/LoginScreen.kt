package com.example.edupathai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.data.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Mandatory Profile Setup State
    var showBioDialog by remember { mutableStateOf(false) }
    var newUserId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isSavingProfile by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val client = SupabaseProvider.client

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both email and password."
                        return@Button
                    }
                    if (isSignUp && password.length < 6) {
                        errorMessage = "Password must be at least 6 characters."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            if (isSignUp) {
                                // 1. Attempt Sign Up
                                client.auth.signUpWith(Email) {
                                    this.email = email.trim()
                                    this.password = password.trim()
                                }

                                val currentUser = client.auth.currentUserOrNull()

                                // 2. Detect duplicate account
                                if (currentUser == null || currentUser.identities.isNullOrEmpty()) {
                                    isLoading = false
                                    errorMessage = "An account with this email already exists. Please log in."
                                    try { client.auth.signOut() } catch (_: Exception) {}
                                    return@launch
                                }

                                // 3. Require user to set up profile
                                newUserId = currentUser.id
                                isLoading = false
                                showBioDialog = true
                            } else {
                                // Normal Login
                                client.auth.signInWith(Email) {
                                    this.email = email.trim()
                                    this.password = password.trim()
                                }
                                isLoading = false
                                onLoginSuccess()
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            val msg = e.message ?: "Authentication failed"
                            errorMessage = when {
                                msg.contains("already registered", ignoreCase = true) ||
                                        msg.contains("already exists", ignoreCase = true) ||
                                        msg.contains("duplicate key", ignoreCase = true) -> {
                                    "This email is already registered. Please log in."
                                }
                                msg.contains("Invalid login credentials", ignoreCase = true) -> {
                                    "Invalid email or password. Please try again."
                                }
                                else -> msg
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isSignUp) "Sign Up" else "Log In")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    isSignUp = !isSignUp
                    errorMessage = null
                }
            ) {
                Text(
                    if (isSignUp) "Already have an account? Log In"
                    else "Don't have an account? Sign Up"
                )
            }
        }

        // --- Mandatory Profile Setup Dialog (No Skip Option) ---
        if (showBioDialog) {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss without completing input */ },
                title = {
                    Text(
                        text = "Complete Your Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Please enter your name and study goals to set up your AI learning profile:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Display Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            isError = username.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio / Focus Goals *") },
                            placeholder = { Text("e.g., CS Student preparing for exams, visual learner") },
                            minLines = 3,
                            maxLines = 4,
                            isError = bio.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (username.isBlank() || bio.isBlank()) {
                            Text(
                                text = "Both fields are required to continue.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (username.isNotBlank() && bio.isNotBlank()) {
                                isSavingProfile = true
                                coroutineScope.launch {
                                    try {
                                        val profile = UserProfile(
                                            id = newUserId,
                                            email = email.trim(),
                                            username = username.trim(),
                                            bio = bio.trim()
                                        )
                                        client.from("profiles").upsert(profile)
                                        showBioDialog = false
                                        onLoginSuccess()
                                    } catch (_: Exception) {
                                        showBioDialog = false
                                        onLoginSuccess()
                                    } finally {
                                        isSavingProfile = false
                                    }
                                }
                            }
                        },
                        enabled = username.isNotBlank() && bio.isNotBlank() && !isSavingProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSavingProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Complete Setup")
                        }
                    }
                }
            )
        }
    }
}