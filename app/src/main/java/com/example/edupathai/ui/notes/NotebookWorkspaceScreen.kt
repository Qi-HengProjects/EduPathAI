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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.AiPromptType
import com.example.edupathai.data.MindmapBranch
import com.example.edupathai.data.MindmapData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookWorkspaceScreen(
    subjectName: String,
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveNote() }) {
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
            // Note Title Input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Note Title") },
                placeholder = { Text("e.g., Chapter 4: Cellular Respiration") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Raw Note Content
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
                        value = uiState.content,
                        onValueChange = { viewModel.updateContent(it) },
                        placeholder = { Text("Write or paste study materials, lecture summaries, or textbook sections here...") },
                        minLines = 6,
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
                        isLoading = uiState.isAiProcessing && uiState.activeAiAction == AiPromptType.SIMPLIFY_JARGON,
                        onClick = { viewModel.runAiAction(AiPromptType.SIMPLIFY_JARGON) },
                        modifier = Modifier.weight(1f)
                    )
                    AiActionButton(
                        label = "Quiz",
                        icon = Icons.Default.Psychology,
                        isLoading = uiState.isAiProcessing && uiState.activeAiAction == AiPromptType.GENERATE_QUIZ,
                        onClick = { viewModel.runAiAction(AiPromptType.GENERATE_QUIZ) },
                        modifier = Modifier.weight(1f)
                    )
                    AiActionButton(
                        label = "Mindmap",
                        icon = Icons.Default.Schema,
                        isLoading = uiState.isAiProcessing && uiState.activeAiAction == AiPromptType.MINDMAP,
                        onClick = { viewModel.runAiAction(AiPromptType.MINDMAP) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 1. Simplified Jargon Island
            uiState.simplifiedJargon?.let { text ->
                AiOutputIsland(
                    title = "💡 Simplified Explanation",
                    content = text,
                    containerColor = Color(0xFF1E293B),
                    accentColor = Color(0xFF38BDF8),
                    onSchedule = { viewModel.scheduleReviewTask(AiPromptType.SIMPLIFY_JARGON) },
                    onDelete = { viewModel.clearAiIsland(AiPromptType.SIMPLIFY_JARGON) }
                )
            }

            // 2. Active Recall Quiz Island
            uiState.activeRecallQuiz?.let { text ->
                AiOutputIsland(
                    title = "❓ Active Recall Flashcards",
                    content = text,
                    containerColor = Color(0xFF2E1065),
                    accentColor = Color(0xFFA78BFA),
                    onSchedule = { viewModel.scheduleReviewTask(AiPromptType.GENERATE_QUIZ) },
                    onDelete = { viewModel.clearAiIsland(AiPromptType.GENERATE_QUIZ) }
                )
            }

            // 3. Visual Graphical Mindmap Diagram
            uiState.mindmapData?.let { data ->
                VisualMindmapCard(
                    mindmapData = data,
                    onSchedule = { viewModel.scheduleReviewTask(AiPromptType.MINDMAP) },
                    onDelete = { viewModel.clearAiIsland(AiPromptType.MINDMAP) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------------------
// GRAPHICAL VISUAL MINDMAP DIAGRAM COMPONENT
// ----------------------------------------------------------------
@Composable
fun VisualMindmapCard(
    mindmapData: MindmapData,
    onSchedule: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onSchedule,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graphical Mind Map Node Canvas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Central Root Node Bubble
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF3B82F6),
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "🎯 ${mindmapData.rootTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // 2. Central Connector Stem
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(Color(0xFF64748B))
                )

                // 3. Child Branch Nodes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val branchColors = listOf(
                        Color(0xFF10B981) to Color(0xFF064E3B),
                        Color(0xFFA855F7) to Color(0xFF3B0764),
                        Color(0xFFF59E0B) to Color(0xFF451A03),
                        Color(0xFF06B6D4) to Color(0xFF164E63)
                    )

                    mindmapData.branches.forEachIndexed { index, branch ->
                        val (accent, bg) = branchColors[index % branchColors.size]

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bg.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        branch.subItems.forEach { subItem ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF0F172A).copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    text = subItem,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFCBD5E1),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

// ----------------------------------------------------------------
// REUSABLE AI ISLANDS & BUTTONS
// ----------------------------------------------------------------
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
                    color = accentColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onSchedule,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
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
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
            )
        }
    }
}