package com.example.edupathai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.example.edupathai.ui.notes.NoteDetailViewModel
import com.example.edupathai.ui.notes.NotebookWorkspaceScreen
import com.example.edupathai.ui.notes.NotesDirectoryScreen
import com.example.edupathai.ui.schedule.DailyTimelineScreen
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
                    LoginScreen(
                        onLoginSuccess = { uid ->
                            currentUserId = uid
                        }
                    )
                } else {
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
        currentRoute = currentRoute,
        onNavigateTo = { route ->
            navController.navigate(route) {
                // Pop up to the start destination to avoid stack overflow
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) { paddingValues ->
        val imeVisible = WindowInsets.isImeVisible
        val layoutDirection = LocalLayoutDirection.current
        val navHostPadding = if (imeVisible) {
            androidx.compose.foundation.layout.PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = 0.dp
            )
        } else {
            paddingValues
        }

        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(navHostPadding)
        ) {
            // Tab 1: Personal Dashboard
            composable("dashboard") {
                DashboardScreen(
                    userId = userId,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // Sub-page: Settings Panel
            composable("settings") {
                SettingsScreen(
                    userId = userId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onLoggedOut = onLoggedOut
                )
            }

            // Tab 2: Notes Directory
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

            // Sub-page: Notebook Workspace
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

            // Tab 3: AI Chat Home
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

            // Sub-page: Chat History List
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

            // Sub-page: Individual Chat Session
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

            // Tab 4: Daily Schedule Timeline
            composable("daily_timeline") {
                DailyTimelineScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}