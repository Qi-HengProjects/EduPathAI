package com.example.edupathai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.ui.DashboardScreen
import com.example.edupathai.ui.LoginScreen
import com.example.edupathai.ui.SettingsScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EduPathAITheme {
                var isAuthenticated by rememberSaveable {
                    mutableStateOf(SupabaseProvider.client.auth.currentSessionOrNull() != null)
                }
                var currentDestination by rememberSaveable { mutableStateOf(AppDestination.DASHBOARD) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var activeFolderId by rememberSaveable { mutableStateOf<String?>(null) }
                var activeFolderName by rememberSaveable { mutableStateOf("") }

                if (!isAuthenticated) {
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
                                isAuthenticated = false
                                showSettings = false
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
                    } else {
                        AdaptiveAppScaffold(
                            currentDestination = currentDestination,
                            onNavigate = { destination ->
                                if (destination == AppDestination.SETTINGS) {
                                    showSettings = true
                                } else {
                                    currentDestination = destination
                                }
                            }
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
                                    ChatScreen(viewModel = chatViewModel)
                                }
                                AppDestination.SCHEDULE -> {
                                    val scheduleViewModel: ScheduleViewModel = viewModel()
                                    DailyTimelineScreen(
                                        viewModel = scheduleViewModel,
                                        onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                                    )
                                }
                                AppDestination.SETTINGS -> {
                                    // Should not be reached because of onNavigate logic,
                                    // but added to make 'when' exhaustive
                                    SettingsScreen(
                                        userId = currentUserId,
                                        onBack = { currentDestination = AppDestination.DASHBOARD },
                                        onLoggedOut = {
                                            isAuthenticated = false
                                        }
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
