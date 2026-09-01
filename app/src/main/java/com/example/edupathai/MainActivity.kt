package com.example.edupathai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch

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

    var isLoggedIn by rememberSaveable { mutableStateOf(true) }
    val currentUserId = remember { SupabaseProvider.getLocalUserId() }

    // Shared ScheduleViewModel so Dashboard and Schedule Timeline are always 100% synchronized
    val scheduleViewModel: ScheduleViewModel = viewModel()

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showChatHistory by rememberSaveable { mutableStateOf(false) }

    var activeFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFolderName by rememberSaveable { mutableStateOf("") }
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSessionTitle by rememberSaveable { mutableStateOf<String?>(null) }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = {
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
                    try {
                        SupabaseProvider.client.auth.signOut()
                    } catch (_: Exception) {}
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