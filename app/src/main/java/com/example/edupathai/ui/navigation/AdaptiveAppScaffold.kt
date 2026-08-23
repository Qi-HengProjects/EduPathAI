package com.example.edupathai.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AdaptiveAppScaffold(
    currentRoute: String?,
    onNavigateTo: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val navItems = listOf(
        NavigationTabItem(
            route = "dashboard",
            label = "Dashboard",
            icon = Icons.Default.Person
        ),
        NavigationTabItem(
            route = "notes_directory",
            label = "Notes",
            icon = Icons.Default.MenuBook
        ),
        NavigationTabItem(
            route = "chat_home",
            label = "AI Chat",
            icon = Icons.Default.Chat
        ),
        NavigationTabItem(
            route = "daily_timeline",
            label = "Schedule",
            icon = Icons.Default.DateRange
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    val selected = currentRoute?.startsWith(item.route) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigateTo(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}