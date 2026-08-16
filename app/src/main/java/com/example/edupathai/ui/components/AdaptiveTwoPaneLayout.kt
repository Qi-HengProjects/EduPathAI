package com.example.edupathai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveTwoPaneLayout(
    primaryPane: @Composable () -> Unit,
    secondaryPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    breakpoint: Dp = 600.dp,
    primaryPaneWeight: Float = 0.38f,
    secondaryPaneWeight: Float = 0.62f
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isExpanded = screenWidth >= breakpoint

    // Inside AdaptiveTwoPaneLayout.kt
    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(primaryPaneWeight)
                    .fillMaxHeight()
            ) {
                primaryPane()
            }

            Spacer(modifier = Modifier.width(16.dp)) // Clean spacing instead of a divider line

            Box(
                modifier = Modifier
                    .weight(secondaryPaneWeight)
                    .fillMaxHeight()
            ) {
                secondaryPane()
            }
        }
    }
}