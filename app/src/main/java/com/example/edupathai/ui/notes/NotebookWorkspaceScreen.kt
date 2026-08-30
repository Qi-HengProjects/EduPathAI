package com.example.edupathai.ui.notes

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.MindmapData

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotebookWorkspaceScreen(
    subjectName: String,
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Sync notification messages
    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subjectName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveCurrentNote() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val currentNote = uiState.currentNote ?: return@Scaffold

            // Note Title Input
            OutlinedTextField(
                value = currentNote.title,
                onValueChange = { viewModel.updateCurrentNoteContent(it, currentNote.contentMarkdown) },
                label = { Text("Note Title") },
                placeholder = { Text("e.g., Chapter 4: Cellular Respiration") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Raw Note Content Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📝 Raw Note Content",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentNote.contentMarkdown,
                        onValueChange = { viewModel.updateCurrentNoteContent(currentNote.title, it) },
                        placeholder = { Text("Write or paste study materials, lecture summaries, or textbook sections here...") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // AI Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Assistive AI Tools",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiActionButton(
                        label = "Simplify",
                        icon = Icons.Default.Lightbulb,
                        isLoading = uiState.isAiProcessing && uiState.aiMode == AiIslandMode.SIMPLIFY,
                        onClick = { viewModel.simplifyNote() },
                        modifier = Modifier.weight(1f)
                    )
                    AiActionButton(
                        label = "Flashcards",
                        icon = Icons.Default.Psychology,
                        isLoading = uiState.isAiProcessing && uiState.aiMode == AiIslandMode.FLASHCARDS,
                        onClick = { viewModel.generateFlashcards() },
                        modifier = Modifier.weight(1f)
                    )
                    AiActionButton(
                        label = "Mindmap",
                        icon = Icons.Default.Schema,
                        isLoading = uiState.isAiProcessing && uiState.aiMode == AiIslandMode.MINDMAP,
                        onClick = { viewModel.generateMindmap() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 1. Simplified Jargon Island
            uiState.simplifiedText?.let { text ->
                AiOutputIsland(
                    title = "💡 Simplified Explanation",
                    content = text,
                    containerColor = Color(0xFF1E293B),
                    accentColor = Color(0xFF38BDF8),
                    onSchedule = { viewModel.scheduleNoteTask("Review Simplified") },
                    onDelete = { viewModel.dismissAiIsland() }
                )
            }

            // 2. Flashcards Island
            if (uiState.flashcards.isNotEmpty()) {
                AiOutputIsland(
                    title = "❓ Active Recall Flashcards",
                    content = uiState.flashcards.joinToString("\n\n") { "Q: ${it.question}\nA: ${it.answer}" },
                    containerColor = Color(0xFF2E1065),
                    accentColor = Color(0xFFA78BFA),
                    onSchedule = { viewModel.scheduleNoteTask("Practice Flashcards") },
                    onDelete = { viewModel.dismissAiIsland() }
                )
            }

            // 3. Visual Connected Mindmap Diagram
            uiState.mindmapData?.let { data ->
                VisualMindmapCard(
                    mindmapData = data,
                    onSchedule = { viewModel.scheduleNoteTask("Study Mindmap") },
                    onDelete = { viewModel.dismissAiIsland() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AiActionButton(
    label: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun AiOutputIsland(
    title: String,
    content: String,
    containerColor: Color,
    accentColor: Color,
    onSchedule: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSchedule,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(content))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = accentColor, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = accentColor.copy(alpha = 0.25f)
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualMindmapCard(
    mindmapData: MindmapData,
    onSchedule: () -> Unit,
    onDelete: () -> Unit
) {
    val branchColors = listOf(
        Color(0xFF10B981) to Color(0xFF064E3B), // Emerald
        Color(0xFFA855F7) to Color(0xFF3B0764), // Purple
        Color(0xFFF59E0B) to Color(0xFF451A03), // Amber
        Color(0xFF06B6D4) to Color(0xFF164E63), // Cyan
        Color(0xFFEC4899) to Color(0xFF500724)  // Pink
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schema,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visual Mindmap Diagram",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSchedule,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Mindmap Frame
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                // 1. Central Root Node
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF2563EB),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "🎯 ${mindmapData.rootTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 2. Trunk Connector Line Drop from Root
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 4.dp.toPx(),
                            center = Offset(centerX, size.height)
                        )
                    }
                }

                // 3. Branches with Integrated Structural Spine Lines
                Column(modifier = Modifier.fillMaxWidth()) {
                    mindmapData.branches.forEachIndexed { index, branch ->
                        val (accent, bg) = branchColors[index % branchColors.size]
                        val isFirst = index == 0
                        val isLast = index == mindmapData.branches.size - 1

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left Spine & Branch Line Canvas
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight()
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val spineX = 10.dp.toPx()
                                    val branchY = 22.dp.toPx()
                                    val endX = size.width
                                    val stroke = 2.5.dp.toPx()

                                    if (!isFirst) {
                                        drawLine(
                                            color = Color(0xFF38BDF8),
                                            start = Offset(spineX, 0f),
                                            end = Offset(spineX, branchY),
                                            strokeWidth = stroke
                                        )
                                    }

                                    if (!isLast) {
                                        drawLine(
                                            color = Color(0xFF38BDF8),
                                            start = Offset(spineX, branchY),
                                            end = Offset(spineX, size.height),
                                            strokeWidth = stroke
                                        )
                                    }

                                    drawLine(
                                        color = accent,
                                        start = Offset(spineX, branchY),
                                        end = Offset(endX, branchY),
                                        strokeWidth = stroke
                                    )

                                    drawCircle(
                                        color = accent,
                                        radius = 4.dp.toPx(),
                                        center = Offset(spineX, branchY)
                                    )

                                    drawCircle(
                                        color = accent,
                                        radius = 3.dp.toPx(),
                                        center = Offset(endX, branchY)
                                    )
                                }
                            }

                            // Branch Content Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = bg.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = if (isLast) 0.dp else 12.dp)
                                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(accent)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = branch.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                    }

                                    if (branch.subItems.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            branch.subItems.forEach { subItem ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.5.dp,
                                                        accent.copy(alpha = 0.35f)
                                                    )
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            text = "•",
                                                            color = accent,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = subItem,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFCBD5E1)
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
                }
            }
        }
    }
}
