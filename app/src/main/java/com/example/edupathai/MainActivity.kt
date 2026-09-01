package com.example.edupathai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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

    var isCheckingAuth by rememberSaveable { mutableStateOf(true) }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var currentUserId by rememberSaveable { mutableStateOf("") }

    val scheduleViewModel: ScheduleViewModel = viewModel()

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showChatHistory by rememberSaveable { mutableStateOf(false) }

    var activeFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFolderName by rememberSaveable { mutableStateOf("") }
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSessionTitle by rememberSaveable { mutableStateOf<String?>(null) }

    // Check Auth State on launch
    LaunchedEffect(Unit) {
        try {
            var session = SupabaseProvider.client.auth.currentSessionOrNull()
            val rememberMe = prefs.getBoolean("remember_me", false)
            val savedEmail = prefs.getString("saved_email", "") ?: ""
            val savedPassword = prefs.getString("saved_password", "") ?: ""

            // Auto-login only if remember_me is enabled with valid saved credentials
            if (session == null && rememberMe && savedEmail.isNotBlank() && savedPassword.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    try {
                        SupabaseProvider.client.auth.signInWith(Email) {
                            this.email = savedEmail.trim()
                            this.password = savedPassword
                        }
                    } catch (_: Exception) {}
                }
                session = SupabaseProvider.client.auth.currentSessionOrNull()
            }

            if (session != null) {
                val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: SupabaseProvider.getLocalUserId()
                currentUserId = uid
                isLoggedIn = true
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
                val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: SupabaseProvider.getLocalUserId()
                currentUserId = uid
                isLoggedIn = true
            }
        )
        return
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            userId = currentUserId,
            onBack = { showSettings = false },
            onLoggedOut = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            SupabaseProvider.client.auth.signOut()
                        } catch (_: Exception) {}
                    }
                    // Explicitly wipe remembered credentials
                    prefs.edit().apply {
                        putBoolean("remember_me", false)
                        remove("saved_email")
                        remove("saved_password")
                        apply()
                    }
                    currentUserId = ""
                    isLoggedIn = false
                    showSettings = false
                }
            }
        )
        return
    }

    if (showChatHistory) {
        BackHandler { showChatHistory = false }
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

    if (currentDestination != AppDestination.DASHBOARD && activeFolderId == null) {
        BackHandler {
            currentDestination = AppDestination.DASHBOARD
        }
    }

    AdaptiveAppScaffold(
        currentDestination = currentDestination,
        onNavigate = { currentDestination = it }
    ) {
        when (currentDestination) {
            AppDestination.DASHBOARD -> {
                DashboardScreen(
                    userId = currentUserId,
                    scheduleViewModel = scheduleViewModel,
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
                DailyTimelineScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                )
            }
        }
    }
}