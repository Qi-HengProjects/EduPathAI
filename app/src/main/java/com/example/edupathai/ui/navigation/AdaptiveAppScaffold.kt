package com.example.edupathai.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    DASHBOARD("Dashboard", Icons.Default.Person, "dashboard"),
    NOTES("Notes", Icons.AutoMirrored.Filled.MenuBook, "notes_directory"),
    CHAT("AI Chat", Icons.AutoMirrored.Filled.Chat, "chat_home"),
    SCHEDULE("Schedule", Icons.Default.CalendarMonth, "daily_timeline"),
    SETTINGS("Settings", Icons.Default.Settings, "settings");

    companion object {
        fun fromRoute(route: String?): AppDestination {
            return entries.find { route?.startsWith(it.route) == true } ?: DASHBOARD
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveAppScaffold(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isKeyboardOpen = WindowInsets.isImeVisible
    val layoutDirection = LocalLayoutDirection.current

    if (isLandscape) {
        // LANDSCAPE MODE: Left Navigation Rail
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.weight(1f))
                AppDestination.entries.forEach { destination ->
                    NavigationRailItem(
                        selected = currentDestination == destination,
                        onClick = { onNavigate(destination) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    } else {
        // PORTRAIT MODE: 4-Item Bottom Navigation Bar
        Scaffold(
            bottomBar = {
                if (!isKeyboardOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination == destination,
                                onClick = { onNavigate(destination) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (isKeyboardOpen) 0.dp else innerPadding.calculateBottomPadding(),
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection)
                    )
            ) {
                content()
            }
        }
    }
}