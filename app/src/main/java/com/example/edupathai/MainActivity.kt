package com.example.edupathai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edupathai.data.SupabaseProvider
import com.example.edupathai.ui.DashboardScreen
import com.example.edupathai.ui.LoginScreen
import com.example.edupathai.ui.SettingsScreen
import com.example.edupathai.ui.chatbox.ChatHistoryScreen
import com.example.edupathai.ui.chatbox.ChatScreen
import com.example.edupathai.ui.chatbox.ChatViewModel
import com.example.edupathai.ui.navigation.AdaptiveAppScaffold
import com.example.edupathai.ui.navigation.AppDestination
import com.example.edupathai.ui.notes.NoteDetailViewModel
import com.example.edupathai.ui.notes.NotebookWorkspaceScreen
import com.example.edupathai.ui.notes.NotesDirectoryScreen
import com.example.edupathai.ui.schedule.DailyTimelineScreen
import com.example.edupathai.ui.schedule.ScheduleViewModel
import com.example.edupathai.ui.theme.EduPathAITheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduPathAITheme {
                val currentSession = SupabaseProvider.auth.currentUserOrNull()
                var currentUserId by remember { mutableStateOf(currentSession?.id ?: "") }

                if (currentUserId.isEmpty()) {
                    // Screen 1: Login & Sign Up Page
                    LoginScreen(
                        onLoginSuccess = {
                            currentUserId = SupabaseProvider.auth.currentUserOrNull()?.id ?: ""
                        }
                    )
                } else {
                    // Main App Navigation with Adaptive Scaffold
                    EduPathNavHost(
                        userId = currentUserId,
                        onLoggedOut = {
                            currentUserId = ""
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EduPathNavHost(
    userId: String,
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AdaptiveAppScaffold(
        currentDestination = AppDestination.fromRoute(currentRoute),
        onNavigate = { destination ->
            navController.navigate(destination.route) {
                popUpTo("dashboard") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.fillMaxSize()
        ) {
            // Target Screen 8: Personal Dashboard
            composable("dashboard") {
                DashboardScreen(
                    userId = userId,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // Target Screen 9: Settings Panel & Accessibility
            composable("settings") {
                SettingsScreen(
                    userId = userId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onLoggedOut = onLoggedOut
                )
            }

            // Module 1: Notes & Timeline (Existing)
            composable("notes_directory") {
                NotesDirectoryScreen(
                    onFolderClick = { folderId, subjectName ->
                        val encodedName = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
                        navController.navigate("notebook_workspace/$folderId?subjectName=$encodedName")
                    },
                    onNavigateToTimeline = {
                        navController.navigate("daily_timeline")
                    }
                )
            }

            composable(
                route = "notebook_workspace/{folderId}?subjectName={subjectName}",
                arguments = listOf(
                    navArgument("folderId") { type = NavType.StringType },
                    navArgument("subjectName") {
                        type = NavType.StringType
                        defaultValue = "Subject Notebook"
                    }
                )
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
                val rawSubjectName = backStackEntry.arguments?.getString("subjectName") ?: "Subject Notebook"
                val subjectName = URLDecoder.decode(rawSubjectName, StandardCharsets.UTF_8.toString())

                val workspaceViewModel = viewModel<NoteDetailViewModel>(
                    key = folderId,
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return NoteDetailViewModel(folderId = folderId) as T
                        }
                    }
                )

                NotebookWorkspaceScreen(
                    subjectName = subjectName,
                    viewModel = workspaceViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("daily_timeline") {
                val scheduleViewModel = viewModel<ScheduleViewModel>()
                DailyTimelineScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Module 2: AI Chatbot & Conversation History Engine (Existing)
            composable("chat_home") {
                val chatViewModel = viewModel<ChatViewModel>(
                    key = "chat_home",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(initialSessionId = null) as T
                        }
                    }
                )

                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToHistory = { navController.navigate("chat_history") }
                )
            }

            composable("chat_history") {
                ChatHistoryScreen(
                    onSessionClick = { sessionId, title ->
                        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                        navController.navigate("chat_session/$sessionId?title=$encodedTitle")
                    },
                    onStartNewChat = {
                        navController.navigate("chat_home") {
                            popUpTo("chat_home") { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "chat_session/{sessionId}?title={title}",
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = "Chat"
                    }
                )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val rawTitle = backStackEntry.arguments?.getString("title") ?: "Chat"
                val sessionTitle = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

                val sessionViewModel = viewModel<ChatViewModel>(
                    key = sessionId,
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(
                                initialSessionId = sessionId,
                                initialSessionTitle = sessionTitle
                            ) as T
                        }
                    }
                )

                ChatScreen(
                    viewModel = sessionViewModel,
                    onNavigateToHistory = { navController.navigate("chat_history") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}