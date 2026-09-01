package com.example.edupathai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.SupabaseProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("edupath_auth_prefs", Context.MODE_PRIVATE) }

    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    // Load saved credentials on launch if Remember Me was enabled
    LaunchedEffect(Unit) {
        val savedRememberMe = prefs.getBoolean("remember_me", false)
        rememberMe = savedRememberMe
        if (savedRememberMe) {
            email = prefs.getString("saved_email", "") ?: ""
            password = prefs.getString("saved_password", "") ?: ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(44.dp)
                )

                Text(
                    text = if (isRegistering) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = if (isRegistering) "Register with your student email" else "Sign in to access your dashboard & notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("student@university.edu", color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Remember Me Checkbox Row (Only on Login tab)
                if (!isRegistering) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF3B82F6),
                                uncheckedColor = Color(0xFF64748B),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remember Me",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Button (Login / Register)
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        focusManager.clearFocus()
                        isLoading = true

                        coroutineScope.launch {
                            try {
                                if (isRegistering) {
                                    SupabaseProvider.client.auth.signUpWith(Email) {
                                        this.email = email.trim()
                                        this.password = password
                                    }
                                    Toast.makeText(
                                        context,
                                        "Registration successful! Please check your email inbox to verify your account before logging in.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isRegistering = false
                                } else {
                                    SupabaseProvider.client.auth.signInWith(Email) {
                                        this.email = email.trim()
                                        this.password = password
                                    }

                                    // Save or clear credentials based on Remember Me checkbox
                                    prefs.edit().apply {
                                        putBoolean("remember_me", rememberMe)
                                        if (rememberMe) {
                                            putString("saved_email", email.trim())
                                            putString("saved_password", password)
                                        } else {
                                            remove("saved_email")
                                            remove("saved_password")
                                        }
                                        apply()
                                    }

                                    onLoginSuccess()
                                }
                            } catch (e: Exception) {
                                val errorMsg = e.message ?: "Authentication failed"
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isRegistering) "Create Student Account" else "Sign In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Switch Between Sign In / Sign Up
                TextButton(
                    onClick = {
                        isRegistering = !isRegistering
                    }
                ) {
                    Text(
                        text = if (isRegistering) "Already have an account? Sign In" else "Don't have an account? Register",
                        color = Color(0xFF38BDF8),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}