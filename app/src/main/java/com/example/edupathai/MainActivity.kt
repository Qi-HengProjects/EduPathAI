package com.example.edupathai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EduPathAITheme {
                val coroutineScope = rememberCoroutineScope()

                var isCheckingAuth by remember { mutableStateOf(true) }
                var isAuthenticated by remember { mutableStateOf(false) }

                var currentDestination by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showChatHistory by rememberSaveable { mutableStateOf(false) }
                var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedSessionTitle by rememberSaveable { mutableStateOf<String?>(null) }

                var activeFolderId by rememberSaveable { mutableStateOf<String?>(null) }
                var activeFolderName by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    SupabaseProvider.client.auth.sessionStatus.collect { status ->
                        when (status) {
                            is SessionStatus.Authenticated -> {
                                isAuthenticated = true
                                isCheckingAuth = false
                            }
                            is SessionStatus.NotAuthenticated -> {
                                isAuthenticated = false
                                isCheckingAuth = false
                            }
                            else -> {
                                isCheckingAuth = true
                            }
                        }
                    }
                }

                if (isCheckingAuth) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "EduPath AI",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                } else if (!isAuthenticated) {
                    LoginScreen(
                        onLoginSuccess = { isAuthenticated = true }
                    )
                } else {
                    val currentUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: ""

                    if (showSettings) {
                        SettingsScreen(
                            userId = currentUserId,
                            onBack = { showSettings = false },
                            onLoggedOut = {
                                coroutineScope.launch {
                                    try {
                                        SupabaseProvider.client.auth.signOut()
                                    } catch (_: Exception) {}
                                    isAuthenticated = false
                                    showSettings = false
                                }
                            }
                        )
                    } else if (activeFolderId != null) {
                        val noteDetailViewModel: NoteDetailViewModel = viewModel(
                            key = activeFolderId,
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    @Suppress("UNCHECKED_CAST")
                                    return NoteDetailViewModel(folderId = activeFolderId!!) as T
                                }
                            }
                        )

                        NotebookWorkspaceScreen(
                            subjectName = activeFolderName,
                            viewModel = noteDetailViewModel,
                            onNavigateBack = { activeFolderId = null }
                        )
                    } else if (showChatHistory) {
                        val chatHistoryViewModel: ChatHistoryViewModel = viewModel()
                        ChatHistoryScreen(
                            viewModel = chatHistoryViewModel,
                            onSessionClick = { sId: String, sTitle: String ->
                                selectedSessionId = sId
                                selectedSessionTitle = sTitle
                                showChatHistory = false
                            },
                            onStartNewChat = {
                                selectedSessionId = null
                                selectedSessionTitle = null
                                showChatHistory = false
                            },
                            onNavigateBack = { showChatHistory = false }
                        )
                    } else {
                        AdaptiveAppScaffold(
                            currentDestination = currentDestination,
                            onNavigate = { currentDestination = it }
                        ) {
                            when (currentDestination) {
                                AppDestination.DASHBOARD -> {
                                    DashboardScreen(
                                        userId = currentUserId,
                                        onNavigateToSettings = { showSettings = true }
                                    )
                                }
                                AppDestination.NOTES -> {
                                    val notesViewModel: NotesViewModel = viewModel()
                                    NotesDirectoryScreen(
                                        viewModel = notesViewModel,
                                        onFolderClick = { id, name ->
                                            activeFolderId = id
                                            activeFolderName = name
                                        }
                                    )
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
                                else -> {
                                    DashboardScreen(
                                        userId = currentUserId,
                                        onNavigateToSettings = { showSettings = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}