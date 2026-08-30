package com.example.edupathai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.edupathai.ui.notes.NotesViewModel
import com.example.edupathai.ui.schedule.DailyTimelineScreen
import com.example.edupathai.ui.schedule.ScheduleViewModel
import com.example.edupathai.ui.theme.EduPathAITheme
import io.github.jan.supabase.gotrue.auth
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EduPathAITheme {
                var isAuthenticated by rememberSaveable {
                    mutableStateOf(SupabaseProvider.client.auth.currentSessionOrNull() != null)
                }

                if (!isAuthenticated) {
                    LoginScreen(
                        onLoginSuccess = { isAuthenticated = true }
                    )
                } else {
                    val currentUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: ""
                    EduPathNavHost(
                        userId = currentUserId,
                        onLogout = { isAuthenticated = false }
                    )
                }
            }
        }
    }
}

@Composable
fun EduPathNavHost(
    userId: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = AppDestination.fromRoute(currentRoute)

    AdaptiveAppScaffold(
        currentDestination = currentDestination,
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
            composable("dashboard") {
                DashboardScreen(
                    userId = userId,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            composable("notes_directory") {
                val notesViewModel: NotesViewModel = viewModel()
                NotesDirectoryScreen(
                    viewModel = notesViewModel,
                    onFolderClick = { folderId, name ->
                        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                        navController.navigate("notebook_workspace/$folderId?name=$encodedName")
                    }
                )
            }

            composable(
                route = "notebook_workspace/{folderId}?name={name}",
                arguments = listOf(
                    navArgument("folderId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "Notebook" }
                )
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                val rawName = backStackEntry.arguments?.getString("name") ?: "Notebook"
                val name = URLDecoder.decode(rawName, StandardCharsets.UTF_8.toString())

                val noteDetailViewModel: NoteDetailViewModel = viewModel(
                    key = folderId,
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return NoteDetailViewModel(folderId = folderId) as T
                        }
                    }
                )

                NotebookWorkspaceScreen(
                    subjectName = name,
                    viewModel = noteDetailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("chat_home") {
                val chatViewModel: ChatViewModel = viewModel()
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToHistory = { navController.navigate("chat_history") },
                    onNavigateBack = { navController.popBackStack() }
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
                    navArgument("title") { type = NavType.StringType; defaultValue = "Chat" }
                )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                val rawTitle = backStackEntry.arguments?.getString("title") ?: "Chat"
                val title = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

                val chatViewModel: ChatViewModel = viewModel(
                    key = sessionId,
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ChatViewModel(
                                initialSessionId = sessionId,
                                initialSessionTitle = title
                            ) as T
                        }
                    }
                )

                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToHistory = { navController.navigate("chat_history") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("daily_timeline") {
                val scheduleViewModel: ScheduleViewModel = viewModel()
                DailyTimelineScreen(
                    viewModel = scheduleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onLoggedOut = onLogout
                )
            }
        }
    }
}
