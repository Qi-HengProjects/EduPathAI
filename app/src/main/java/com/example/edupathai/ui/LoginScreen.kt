package com.example.edupathai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.data.UserProfile
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (userId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("edupath_auth_prefs", Context.MODE_PRIVATE) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefs.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(prefs.getBoolean("remember_me", false)) }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "EduPath AI Learning Assistant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name / Student Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Text(text = "Remember Me", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (isSignUp && fullName.isBlank())) {
                        Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            if (isSignUp) {
                                SupabaseProvider.auth.signUpWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                                val uid = SupabaseProvider.auth.currentUserOrNull()?.id ?: ""
                                if (uid.isNotEmpty()) {
                                    SupabaseProvider.db.from("profiles").insert(
                                        UserProfile(
                                            id = uid,
                                            email = email,
                                            fullName = fullName
                                        )
                                    )
                                }
                                saveCredentials(prefs, rememberMe, email, password)
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(uid)
                            } else {
                                SupabaseProvider.auth.signInWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                                val uid = SupabaseProvider.auth.currentUserOrNull()?.id ?: ""
                                saveCredentials(prefs, rememberMe, email, password)
                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(uid)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(text = if (isSignUp) "Sign Up" else "Log In", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    text = if (isSignUp) "Already have an account? Log In"
                    else "Don't have an account? Sign Up"
                )
            }
        }
    }
}

private fun saveCredentials(
    prefs: android.content.SharedPreferences,
    remember: Boolean,
    email: String,
    pass: String
) {
    prefs.edit().apply {
        putBoolean("remember_me", remember)
        if (remember) {
            putString("saved_email", email)
            putString("saved_password", pass)
        } else {
            remove("saved_email")
            remove("saved_password")
        }
        apply()
    }
}