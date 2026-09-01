package com.example.edupathai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edupathai.data.ProfileModel
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.ui.DashboardScreen
import com.example.edupathai.ui.LoginScreen
import com.example.edupathai.ui.SettingsScreen
import com.example.edupathai.ui.chatbox.ChatHistoryScreen
import com.example.edupathai.ui.chatbox.ChatHistoryViewModel
import com.example.edupathai.ui.chatbox.ChatScreen
import com.example.edupathai.ui.chatbox.ChatViewModel
import com.example.edupathai.ui.navigation.AdaptiveAppScaffold
import com.example.edupathai.ui.navigation.AppDestination
import com.example.edupathai.ui.notes.NoteDetailViewModel
import com.example.edupathai.ui.notes.NotebookWorkspaceScreen
import com.example.edupathai.ui.notes.NotesDirectoryScreen
import com.example.edupathai.ui.notes.NotesViewModel
import com.example.edupathai.ui.schedule.DailyTimelineScreen
import com.example.edupathai.ui.schedule.ScheduleViewModel
import com.example.edupathai.ui.theme.EduPathAITheme
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduPathAITheme {
                MainAppNavigator()
            }
        }
    }
}

@Composable
fun MainAppNavigator() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("edupath_auth_prefs", Context.MODE_PRIVATE) }

    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var isCheckingAuth by rememberSaveable { mutableStateOf(true) }
    var needsProfileSetup by rememberSaveable { mutableStateOf(false) }

    var currentUserId by rememberSaveable { mutableStateOf("") }
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showChatHistory by rememberSaveable { mutableStateOf(false) }

    var activeFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFolderName by rememberSaveable { mutableStateOf("") }
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSessionTitle by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun checkProfileAndNavigate(userId: String) {
        currentUserId = userId
        isLoggedIn = true
        val profile = withContext(Dispatchers.IO) {
            try {
                SupabaseProvider.client.from("profiles").select {
                    filter { eq("id", userId) }
                }.decodeList<ProfileModel>().firstOrNull()
            } catch (_: Exception) {
                null
            }
        }
        needsProfileSetup = profile == null || profile.username.isBlank() || profile.username == "Student"
    }

    LaunchedEffect(Unit) {
        try {
            var session = SupabaseProvider.client.auth.currentSessionOrNull()

            // If cached session expired or was cleared, auto-login using saved Remember Me credentials
            if (session == null) {
                val rememberMe = prefs.getBoolean("remember_me", false)
                val savedEmail = prefs.getString("saved_email", "") ?: ""
                val savedPassword = prefs.getString("saved_password", "") ?: ""

                if (rememberMe && savedEmail.isNotBlank() && savedPassword.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        SupabaseProvider.client.auth.signInWith(Email) {
                            this.email = savedEmail.trim()
                            this.password = savedPassword
                        }
                    }
                    session = SupabaseProvider.client.auth.currentSessionOrNull()
                }
            }

            if (session != null) {
                val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: ""
                if (userId.isNotBlank()) {
                    checkProfileAndNavigate(userId)
                } else {
                    isLoggedIn = false
                }
            } else {
                isLoggedIn = false
            }
        } catch (_: Exception) {
            isLoggedIn = false
        } finally {
            isCheckingAuth = false
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
        return
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = {
                coroutineScope.launch {
                    val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: ""
                    if (userId.isNotBlank()) {
                        checkProfileAndNavigate(userId)
                    }
                }
            }
        )
        return
    }

    if (needsProfileSetup) {
        MandatoryProfileSetupScreen(
            userId = currentUserId,
            onProfileSaved = {
                needsProfileSetup = false
            }
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            userId = currentUserId,
            onBack = { showSettings = false },
            onLoggedOut = {
                coroutineScope.launch {
                    try {
                        SupabaseProvider.client.auth.signOut()
                    } catch (_: Exception) {}
                    // Clear saved credentials on manual sign out
                    prefs.edit().apply {
                        putBoolean("remember_me", false)
                        remove("saved_email")
                        remove("saved_password")
                        apply()
                    }
                    isLoggedIn = false
                    showSettings = false
                }
            }
        )
        return
    }

    if (showChatHistory) {
        val historyViewModel: ChatHistoryViewModel = viewModel()
        ChatHistoryScreen(
            viewModel = historyViewModel,
            onNavigateBack = { showChatHistory = false },
            onSessionClick = { sessionId: String, title: String ->
                selectedSessionId = sessionId
                selectedSessionTitle = title
                showChatHistory = false
                currentDestination = AppDestination.CHAT
            },
            onStartNewChat = {
                selectedSessionId = null
                selectedSessionTitle = null
                showChatHistory = false
                currentDestination = AppDestination.CHAT
            }
        )
        return
    }

    AdaptiveAppScaffold(
        currentDestination = currentDestination,
        onNavigate = { currentDestination = it }
    ) {
        when (currentDestination) {
            AppDestination.DASHBOARD -> {
                DashboardScreen(
                    userId = currentUserId,
                    onNavigateToSettings = { showSettings = true },
                    onNavigateToNotes = { currentDestination = AppDestination.NOTES },
                    onNavigateToSchedule = { currentDestination = AppDestination.SCHEDULE }
                )
            }
            AppDestination.NOTES -> {
                val folderId = activeFolderId
                if (folderId != null) {
                    val detailViewModel: NoteDetailViewModel = viewModel(
                        key = folderId,
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return NoteDetailViewModel(folderId = folderId) as T
                            }
                        }
                    )
                    NotebookWorkspaceScreen(
                        subjectName = activeFolderName,
                        viewModel = detailViewModel,
                        onNavigateBack = { activeFolderId = null }
                    )
                } else {
                    val notesViewModel: NotesViewModel = viewModel()
                    NotesDirectoryScreen(
                        viewModel = notesViewModel,
                        onFolderClick = { id, name ->
                            activeFolderId = id
                            activeFolderName = name
                        }
                    )
                }
            }
            AppDestination.CHAT -> {
                val chatViewModel: ChatViewModel = viewModel()
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToHistory = { showChatHistory = true },
                    initialSessionId = selectedSessionId,
                    initialSessionTitle = selectedSessionTitle
                )
            }
            AppDestination.SCHEDULE -> {
                val scheduleViewModel: ScheduleViewModel = viewModel()
                DailyTimelineScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                )
            }
        }
    }
}

@Composable
fun MandatoryProfileSetupScreen(
    userId: String,
    onProfileSaved: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var usernameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome! Set Up Your Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Please enter your username and academic details before continuing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text("Academic Major / Bio") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (usernameInput.isBlank()) return@Button
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val email = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: ""
                                val profile = ProfileModel(
                                    id = userId,
                                    username = usernameInput.trim(),
                                    email = email,
                                    bio = bioInput.trim()
                                )
                                withContext(Dispatchers.IO) {
                                    SupabaseProvider.client.from("profiles").upsert(profile)
                                }
                                onProfileSaved()
                            } catch (_: Exception) {
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && usernameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save & Continue", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}