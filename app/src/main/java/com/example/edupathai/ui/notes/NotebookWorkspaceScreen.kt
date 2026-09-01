package com.example.edupathai.ui.notes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.AiIslandMode
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

    BackHandler {
        viewModel.saveCurrentNote()
        onNavigateBack()
    }

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
                    Column {
                        Text(
                            text = subjectName.ifBlank { "Subject Workspace" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${uiState.notes.size} note entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveCurrentNote()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewNote() }) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = "New Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.saveCurrentNote() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.currentNote?.id != null) {
                        IconButton(onClick = { viewModel.deleteCurrentNote() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.notes.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.notes, key = { it.id ?: it.title }) { note ->
                        val isSelected = uiState.currentNote?.id == note.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectNote(note) },
                            label = {
                                Text(
                                    text = note.title.ifBlank { "Untitled" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { viewModel.generateSimplifiedNotes() },
                            label = { Text("Simplify") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (uiState.aiMode == AiIslandMode.SIMPLIFY) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { viewModel.generateFlashcards() },
                            label = { Text("Cards") },
                            leadingIcon = { Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (uiState.aiMode == AiIslandMode.FLASHCARDS) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { viewModel.generateMindmap() },
                            label = { Text("Mindmap") },
                            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (uiState.aiMode == AiIslandMode.MINDMAP) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { viewModel.generateQuiz() },
                            label = { Text("Quiz") },
                            leadingIcon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (uiState.aiMode == AiIslandMode.QUIZ) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.aiMode != AiIslandMode.NONE) {
                    item {
                        AiWorkspaceIsland(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.noteTitle,
                        onValueChange = { newTitle ->
                            viewModel.updateTitle(newTitle)
                        },
                        placeholder = { Text("Note Title...") },
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.noteContent,
                        onValueChange = { newContent ->
                            viewModel.updateContent(newContent)
                        },
                        placeholder = { Text("Write your notes, lecture summaries, or formulas here...") },
                        minLines = 14,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun AiWorkspaceIsland(
    uiState: NoteDetailUiState,
    viewModel: NoteDetailViewModel
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (uiState.isAiProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI is processing notes...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                when (uiState.aiMode) {
                    AiIslandMode.SIMPLIFY -> {
                        SimplifyView(
                            text = uiState.simplifiedText ?: "No summary available.",
                            onRegenerate = { viewModel.generateSimplifiedNotes(forceRegenerate = true) },
                            onAppendToNote = { viewModel.appendSummaryToNoteContent() },
                            onSchedule = { viewModel.scheduleNoteTask("Review Simplified") },
                            onDismiss = { viewModel.dismissAiIsland() }
                        )
                    }
                    AiIslandMode.FLASHCARDS -> {
                        FlashcardsView(
                            flashcards = uiState.flashcards,
                            onRegenerate = { viewModel.generateFlashcards(forceRegenerate = true) },
                            onSchedule = { viewModel.scheduleNoteTask("Practice Flashcards") },
                            onDismiss = { viewModel.dismissAiIsland() }
                        )
                    }
                    AiIslandMode.MINDMAP -> {
                        uiState.mindmapData?.let { mindmap ->
                            VisualMindmapCard(
                                mindmapData = mindmap,
                                onSchedule = { viewModel.scheduleNoteTask("Review Mindmap") },
                                onDelete = { viewModel.dismissAiIsland() }
                            )
                        }
                    }
                    AiIslandMode.QUIZ -> {
                        InteractiveQuizCard(
                            questions = uiState.quizQuestions,
                            currentIndex = uiState.currentQuizIndex,
                            selectedAnswer = uiState.selectedQuizAnswer,
                            isSubmitted = uiState.isAnswerSubmitted,
                            score = uiState.quizScore,
                            isFinished = uiState.isQuizFinished,
                            onSelectOption = { answer ->
                                viewModel.selectQuizAnswer(answer)
                                viewModel.submitQuizAnswer()
                            },
                            onNext = { viewModel.nextQuizQuestion() },
                            onReset = { viewModel.resetQuiz() },
                            onSchedule = { viewModel.scheduleNoteTask("Practice Quiz") },
                            onDismiss = { viewModel.dismissAiIsland() }
                        )
                    }
                    AiIslandMode.NONE -> {}
                }
            }
        }
    }
}

@Composable
fun SimplifyView(
    text: String,
    onRegenerate: () -> Unit,
    onAppendToNote: () -> Unit,
    onSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(16.dp))
                }
                FilledTonalButton(onClick = onAppendToNote, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Note", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(onClick = onSchedule, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun InteractiveQuizCard(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedAnswer: String?,
    isSubmitted: Boolean,
    score: Int,
    isFinished: Boolean,
    onSelectOption: (String) -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    onSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    if (questions.isEmpty()) {
        Text("No quiz questions generated. Write more notes and tap Quiz again.")
        return
    }

    if (isFinished) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Quiz Completed!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your Score: $score / ${questions.size}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (score >= (questions.size + 1) / 2) Color(0xFF10B981) else Color(0xFFEF4444)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake")
                }
                Button(
                    onClick = onSchedule,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule")
                }
            }
        }
        return
    }

    val currentQ = questions.getOrNull(currentIndex) ?: return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = currentQ.question,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            currentQ.options.forEach { option ->
                val isSelected = selectedAnswer == option
                val isCorrect = option.trim().equals(currentQ.correctAnswer.trim(), ignoreCase = true)

                val backgroundColor = when {
                    !isSubmitted -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    isCorrect -> Color(0xFF064E3B)
                    isSelected && !isCorrect -> Color(0xFF450A0A)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    !isSubmitted -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    isCorrect -> Color(0xFF10B981)
                    isSelected && !isCorrect -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSubmitted) { onSelectOption(option) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSubmitted) {
                            if (isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Incorrect",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isSubmitted) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Explanation:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentQ.explanation.ifBlank { "Correct answer: ${currentQ.correctAnswer}" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex + 1 < questions.size) "Next Question" else "View Score Results")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FlashcardsView(
    flashcards: List<Flashcard>,
    onRegenerate: () -> Unit,
    onSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Flashcards (${flashcards.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(16.dp))
                }
                FilledTonalButton(onClick = onSchedule, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            flashcards.forEachIndexed { index, card ->
                var revealed by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { revealed = !revealed }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Q${index + 1}: ${card.question}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (revealed) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "A: ${card.answer}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "Tap to flip answer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
        Color(0xFF10B981) to Color(0xFF064E3B),
        Color(0xFFA855F7) to Color(0xFF3B0764),
        Color(0xFFF59E0B) to Color(0xFF451A03),
        Color(0xFF06B6D4) to Color(0xFF164E63),
        Color(0xFFEC4899) to Color(0xFF500724)
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
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