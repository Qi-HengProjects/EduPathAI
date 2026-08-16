package com.example.edupathai.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

enum class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    NOTEBOOKS("notes_directory", "Notebooks", Icons.Default.Folder),
    TIMELINE("daily_timeline", "Timeline", Icons.Default.CalendarToday)
}

@Composable
fun AdaptiveAppScaffold(
    currentRoute: String?,
    onNavigateTo: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp.dp >= 600.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Locks the entire screen background to dark theme
    ) {
        if (isWideScreen) {
            // Wide / Landscape: Dark Seamless Left Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 96.dp)
                        .padding(horizontal = 4.dp),
                    containerColor = MaterialTheme.colorScheme.background, // Matches background exactly
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AppDestination.values().forEach { destination ->
                        val isSelected = currentRoute == destination.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onNavigateTo(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.title) },
                            label = { Text(destination.title) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content(PaddingValues(0.dp))
                }
            }
        } else {
            // Phone Portrait: Bottom Navigation Bar
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppDestination.values().forEach { destination ->
                            val isSelected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { onNavigateTo(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = destination.title) },
                                label = { Text(destination.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    }
}