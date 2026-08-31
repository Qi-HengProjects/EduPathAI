package com.example.edupathai.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class AppDestination(
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Default.Person),
    NOTES("Notes", Icons.Default.MenuBook),
    CHAT("AI Chat", Icons.Default.Chat),
    SCHEDULE("Schedule", Icons.Default.CalendarMonth)
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
    val darkBackground = MaterialTheme.colorScheme.background

    if (isLandscape) {
        // Landscape Mode: Wider navigation rail with solid dark background
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
        ) {
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(96.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.weight(1f))
                AppDestination.values().forEach { destination ->
                    NavigationRailItem(
                        selected = currentDestination == destination,
                        onClick = { onNavigate(destination) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(darkBackground)
            ) {
                content()
            }
        }
    } else {
        // Portrait Mode: Clean 4-item bottom bar
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = darkBackground,
            bottomBar = {
                if (!isKeyboardOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppDestination.values().forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination == destination,
                                onClick = { onNavigate(destination) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label, maxLines = 1) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(darkBackground)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (isKeyboardOpen) 0.dp else innerPadding.calculateBottomPadding()
                    )
            ) {
                content()
            }
        }
    }
}