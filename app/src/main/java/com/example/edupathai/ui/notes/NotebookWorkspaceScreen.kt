package com.example.edupathai.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.AiPromptType
import com.example.edupathai.data.NoteBookEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookWorkspaceScreen(
    subjectName: String,
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(subjectName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiState.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePreviewMode() }) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview"
                        )
                    }
                    IconButton(onClick = { viewModel.saveNote() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Note")
                    }
                    IconButton(onClick = { showDrawer = true }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open Notes List")
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
        ) {
            // AI Action Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isAiProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }

                AiPromptType.values().forEach { action ->
                    SuggestionChip(
                        onClick = { viewModel.runAiAction(action) },
                        label = { Text(action.title) },
                        icon = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Note Title Input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateDraft(it, uiState.content) },
                label = { Text("Note Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Editor / Markdown Viewer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isPreviewMode) {
                    MarkdownViewer(
                        markdown = uiState.content,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(14.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = uiState.content,
                        onValueChange = { viewModel.updateDraft(uiState.title, it) },
                        label = { Text("Content (Markdown supported)") },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showDrawer) {
            ModalBottomSheet(onDismissRequest = { showDrawer = false }) {
                NotebookDrawerContent(
                    notes = uiState.notes,
                    activeNoteId = uiState.currentNoteId,
                    onSelect = {
                        viewModel.selectNote(it)
                        showDrawer = false
                    },
                    onNewNote = {
                        viewModel.createNewNote()
                        showDrawer = false
                    },
                    onDeleteCurrent = {
                        viewModel.deleteNote()
                        showDrawer = false
                    }
                )
            }
        }
    }
}

@Composable
fun MarkdownViewer(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        if (markdown.isBlank()) {
            Text("No content to display. Switch to edit mode to start writing.", color = MaterialTheme.colorScheme.outline)
        } else {
            markdown.lines().forEach { line ->
                when {
                    line.startsWith("### ") -> Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    line.startsWith("## ") -> Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    line.startsWith("---") -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    line.startsWith("- ") || line.startsWith("• ") -> Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
                    )
                    else -> Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotebookDrawerContent(
    notes: List<NoteBookEntry>,
    activeNoteId: String?,
    onSelect: (NoteBookEntry) -> Unit,
    onNewNote: () -> Unit,
    onDeleteCurrent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subject Notes", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onNewNote) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Note")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(notes) { note ->
                val isSelected = note.id == activeNoteId
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(note) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = note.title.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = note.contentMarkdown.take(60).replace("\n", " ") + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (activeNoteId != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDeleteCurrent,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Active Note")
            }
        }
    }
}