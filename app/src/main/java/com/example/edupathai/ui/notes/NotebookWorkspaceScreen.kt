package com.example.edupathai.ui.notes

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.Flashcard
import com.example.edupathai.data.MindmapData
import com.example.edupathai.data.QuizQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookWorkspaceScreen(
    subjectName: String,
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131C2E)),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                title = {
                    Column {
                        Text(subjectName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("${uiState.notes.size} note entries", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewNote() }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New Note", tint = Color(0xFF60A5FA))
                    }
                    IconButton(onClick = { viewModel.saveCurrentNote() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = { viewModel.deleteCurrentNote() }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Note", tint = Color(0xFFEF4444))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0B0F19))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Note Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.createNewNote() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Note", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    uiState.notes.forEach { note ->
                        val isSelected = note.id == uiState.selectedNoteId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF131C2E),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.clickable { note.id?.let { viewModel.selectNote(it) } }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = note.title.ifBlank { "Untitled" },
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. AI Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip(
                        icon = Icons.Default.AutoAwesome,
                        label = "Simplify",
                        isActive = uiState.viewMode == NoteViewMode.SIMPLIFY,
                        onClick = { viewModel.toggleViewMode(NoteViewMode.SIMPLIFY) }
                    )
                    ActionChip(
                        icon = Icons.Default.Style,
                        label = "Cards",
                        isActive = uiState.viewMode == NoteViewMode.FLASHCARDS,
                        onClick = { viewModel.toggleViewMode(NoteViewMode.FLASHCARDS) }
                    )
                    ActionChip(
                        icon = Icons.Default.AccountTree,
                        label = "Mindmap",
                        isActive = uiState.viewMode == NoteViewMode.MINDMAP,
                        onClick = { viewModel.toggleViewMode(NoteViewMode.MINDMAP) }
                    )
                    ActionChip(
                        icon = Icons.Default.Quiz,
                        label = "Quiz",
                        isActive = uiState.viewMode == NoteViewMode.QUIZ,
                        onClick = { viewModel.toggleViewMode(NoteViewMode.QUIZ) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Main Content Area (Dynamic based on ViewMode)
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    when (uiState.viewMode) {
                        NoteViewMode.EDITOR -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = uiState.noteTitle,
                                    onValueChange = { viewModel.updateTitle(it) },
                                    label = { Text("Note Title") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = uiState.noteContent,
                                    onValueChange = { viewModel.updateContent(it) },
                                    placeholder = { Text("Type or paste lecture notes, code snippets, or study material here...") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            }
                        }
                        NoteViewMode.SIMPLIFY -> {
                            ToolContainerCard(
                                title = "Simplified Key Takeaways",
                                icon = Icons.Default.AutoAwesome,
                                onRegenerate = { viewModel.generateSimplify() },
                                onDelete = { viewModel.deleteCurrentTool() },
                                onSchedule = { viewModel.scheduleStudySession("Review: ${uiState.noteTitle}", "09:00", "10:00", "medium", "#3B82F6") }
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Text(
                                            text = uiState.simplifiedContent,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                        NoteViewMode.FLASHCARDS -> {
                            ToolContainerCard(
                                title = "Study Flashcards (${uiState.flashcards.size})",
                                icon = Icons.Default.Style,
                                onRegenerate = { viewModel.generateFlashcards() },
                                onDelete = { viewModel.deleteCurrentTool() },
                                onSchedule = { viewModel.scheduleStudySession("Flashcards: ${uiState.noteTitle}", "14:00", "15:00", "high", "#EF4444") }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(uiState.flashcards) { index, card ->
                                        FlashcardItemView(index = index + 1, card = card)
                                    }
                                }
                            }
                        }
                        NoteViewMode.MINDMAP -> {
                            ToolContainerCard(
                                title = "Visual Mindmap Diagram",
                                icon = Icons.Default.AccountTree,
                                onRegenerate = { viewModel.generateMindmap() },
                                onDelete = { viewModel.deleteCurrentTool() },
                                onSchedule = { viewModel.scheduleStudySession("Mindmap Review: ${uiState.noteTitle}", "19:00", "20:00", "medium", "#3B82F6") }
                            ) {
                                uiState.mindmap?.let { mapData ->
                                    MindmapDiagramView(data = mapData)
                                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No mindmap generated yet.", color = Color(0xFF64748B))
                                }
                            }
                        }
                        NoteViewMode.QUIZ -> {
                            ToolContainerCard(
                                title = "Knowledge Check Quiz",
                                icon = Icons.Default.Quiz,
                                onRegenerate = { viewModel.generateQuiz() },
                                onDelete = { viewModel.deleteCurrentTool() },
                                onSchedule = { viewModel.scheduleStudySession("Quiz: ${uiState.noteTitle}", "20:00", "21:00", "high", "#10B981") }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(uiState.quiz) { index, q ->
                                        QuizQuestionItemView(index = index + 1, question = q)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF3B82F6))
                            Text(
                                text = uiState.loadingMessage,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF131C2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color(0xFF3B82F6) else Color(0xFF1E293B)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isActive) Color(0xFF60A5FA) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = if (isActive) Color.White else Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ToolContainerCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onSchedule: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 15.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onSchedule,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule", modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", fontSize = 11.sp, color = Color.White)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                content()
            }
        }
    }
}

@Composable
fun FlashcardItemView(index: Int, card: Flashcard) {
    var isRevealed by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .clickable { isRevealed = !isRevealed }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Q$index: ${card.question}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (isRevealed) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Answer: ${card.answer}",
                        color = Color(0xFF34D399),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Text("Tap to reveal answer", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MindmapDiagramView(data: MindmapData) {
    val branchColors = listOf(
        Color(0xFF10B981), // Emerald
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF59E0B), // Amber
        Color(0xFF3B82F6), // Blue
        Color(0xFFEC4899)  // Pink
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Root Node Header
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF2563EB),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = data.rootTitle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Branch Nodes
        itemsIndexed(data.branches) { index, branch ->
            val branchColor = branchColors[index % branchColors.size]

            Row(modifier = Modifier.fillMaxWidth()) {
                // Vertical connecting indicator line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(28.dp).padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(branchColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(60.dp)
                            .background(branchColor.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = branchColor.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, branchColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(branchColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = branch.title,
                                fontWeight = FontWeight.Bold,
                                color = branchColor,
                                fontSize = 14.sp
                            )
                        }

                        if (branch.subItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                branch.subItems.forEach { sub ->
                                    Surface(
                                        color = Color(0xFF0B0F19).copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "• $sub",
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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

@Composable
fun QuizQuestionItemView(index: Int, question: QuizQuestion) {
    var selectedOption by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitted by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("$index. ${question.question}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                question.options.forEach { opt ->
                    val isCorrect = opt == question.correctAnswer
                    val isChosen = opt == selectedOption

                    val optBgColor = when {
                        !isSubmitted && isChosen -> Color(0xFF3B82F6).copy(alpha = 0.25f)
                        isSubmitted && isCorrect -> Color(0xFF10B981).copy(alpha = 0.25f)
                        isSubmitted && isChosen && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.25f)
                        else -> Color(0xFF131C2E)
                    }

                    val optBorderColor = when {
                        !isSubmitted && isChosen -> Color(0xFF3B82F6)
                        isSubmitted && isCorrect -> Color(0xFF10B981)
                        isSubmitted && isChosen && !isCorrect -> Color(0xFFEF4444)
                        else -> Color(0xFF1E293B)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = optBgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, optBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitted) { selectedOption = opt }
                    ) {
                        Text(
                            text = opt,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            if (selectedOption != null && !isSubmitted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { isSubmitted = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Check Answer", fontSize = 12.sp)
                }
            }

            if (isSubmitted && question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explanation: ${question.explanation}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
    }
}