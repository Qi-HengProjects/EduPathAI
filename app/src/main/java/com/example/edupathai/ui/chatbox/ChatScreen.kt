package com.example.edupathai.ui.chatbox

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.NoteFolder
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    initialSessionId: String? = null,
    initialSessionTitle: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(initialSessionId) {
        if (!initialSessionId.isNullOrBlank() && initialSessionId != uiState.currentSessionId) {
            viewModel.loadSession(initialSessionId, initialSessionTitle ?: "AI Study Assistant")
        }
    }

    var showSaveNoteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteContent by rememberSaveable { mutableStateOf("") }
    var selectedNoteTitle by rememberSaveable { mutableStateOf("") }

    var showScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var suggestedTaskTitle by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.actionFeedbackMessage) {
        uiState.actionFeedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedbackMessage()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                Column {
                    Text(
                        text = uiState.currentSessionTitle.ifBlank { "AI Study Assistant" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        text = "Ask questions, generate study notes & plans",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.startNewSession() }) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "New Session",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "How can I help your studies today?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ask to explain concepts, solve problems, or plan study blocks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(uiState.messages) { message ->
                ChatMessageBubble(
                    message = message,
                    onSaveToNote = { content ->
                        selectedNoteContent = content
                        selectedNoteTitle = "AI Note: " + content.take(24).replace("\n", " ").trim() + "..."
                        viewModel.loadAvailableFolders()
                        showSaveNoteDialog = true
                    },
                    onSchedulePlan = { content ->
                        suggestedTaskTitle = "Study: " + content.take(30).replace("\n", " ").trim()
                        showScheduleDialog = true
                    }
                )
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = "AI is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    placeholder = { Text("Ask a question or request a study plan...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSaveNoteDialog) {
        SaveAiToNotesDialog(
            initialTitle = selectedNoteTitle,
            initialContent = selectedNoteContent,
            availableFolders = uiState.availableFolders,
            isSaving = uiState.isSavingNote,
            onDismiss = { showSaveNoteDialog = false },
            onSave = { folderId, title, content ->
                viewModel.saveAiResponseToNotes(
                    folderId = folderId,
                    noteTitle = title,
                    noteContent = content,
                    onSuccess = { showSaveNoteDialog = false }
                )
            },
            onCreateFolderAndSave = { folderName, title, content ->
                viewModel.createFolderAndSaveNote(
                    folderName = folderName,
                    noteTitle = title,
                    noteContent = content,
                    onSuccess = { showSaveNoteDialog = false }
                )
            }
        )
    }

    if (showScheduleDialog) {
        ConfirmSchedulePlanDialog(
            initialTitle = suggestedTaskTitle,
            isSaving = uiState.isSchedulingTask,
            onDismiss = { showScheduleDialog = false },
            onConfirm = { title, start, end, energy, type, colorHex ->
                viewModel.scheduleTimelineTask(
                    title = title,
                    startTime = start,
                    endTime = end,
                    energyLevel = energy,
                    taskType = type,
                    colorHex = colorHex,
                    onSuccess = { showScheduleDialog = false }
                )
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onSaveToNote: (String) -> Unit,
    onSchedulePlan: (String) -> Unit
) {
    val isUser = message.sender.equals("user", ignoreCase = true)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!isUser) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onSchedulePlan(message.text) },
                    label = { Text("Schedule", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )

                AssistChip(
                    onClick = { onSaveToNote(message.text) },
                    label = { Text("Save Note", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )

                AssistChip(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("Copy", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSchedulePlanDialog(
    initialTitle: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, start: String, end: String, energy: String, type: String, colorHex: String) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = LocalTime.now()
    val defaultStart = now.plusMinutes(15).format(timeFormatter)
    val defaultEnd = now.plusMinutes(75).format(timeFormatter)

    var taskTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var startTime by rememberSaveable { mutableStateOf(defaultStart) }
    var endTime by rememberSaveable { mutableStateOf(defaultEnd) }
    var selectedEnergy by rememberSaveable { mutableStateOf("medium") }
    var selectedType by rememberSaveable { mutableStateOf("study") }
    var selectedColor by rememberSaveable { mutableStateOf("#3B82F6") }

    val energyOptions = listOf("high" to "🔥 High Energy", "medium" to "⚡ Medium", "low" to "🌱 Low Energy")
    val typeOptions = listOf("study" to "Study", "revision" to "Revision", "assignment" to "Assignment", "quiz" to "Quiz")
    val taskColors = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Schedule Task", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Review and customize the timeline details before creating:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task / Goal Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("14:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("15:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Energy Required", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(energyOptions) { (key, label) ->
                        FilterChip(
                            selected = selectedEnergy == key,
                            onClick = { selectedEnergy = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Task Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(typeOptions) { (key, label) ->
                        FilterChip(
                            selected = selectedType == key,
                            onClick = { selectedType = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Color Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    taskColors.forEach { hex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .clickable { selectedColor = hex }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(taskTitle, startTime, endTime, selectedEnergy, selectedType, selectedColor)
                },
                enabled = !isSaving && taskTitle.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Confirm & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveAiToNotesDialog(
    initialTitle: String,
    initialContent: String,
    availableFolders: List<NoteFolder>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (folderId: String, title: String, content: String) -> Unit,
    onCreateFolderAndSave: (folderName: String, title: String, content: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var content by rememberSaveable { mutableStateOf(initialContent) }
    var selectedFolderId by rememberSaveable {
        mutableStateOf(availableFolders.firstOrNull()?.id ?: "")
    }
    var isCreatingNewFolder by rememberSaveable {
        mutableStateOf(availableFolders.isEmpty())
    }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Notebook", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isCreatingNewFolder && availableFolders.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        val currentFolderName = availableFolders.find { it.id == selectedFolderId }?.name ?: "Select Subject Folder"
                        OutlinedTextField(
                            value = currentFolderName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject Notebook Folder") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            availableFolders.forEach { folder ->
                                val fId = folder.id
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        selectedFolderId = fId
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { isCreatingNewFolder = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Create New Folder", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("New Subject Folder Name *") },
                        placeholder = { Text("e.g., Computer Science, Math") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (availableFolders.isNotEmpty()) {
                        TextButton(
                            onClick = { isCreatingNewFolder = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Choose Existing Folder", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCreatingNewFolder) {
                        onCreateFolderAndSave(newFolderName, title, content)
                    } else {
                        onSave(selectedFolderId, title, content)
                    }
                },
                enabled = !isSaving && title.isNotBlank() && (
                        (!isCreatingNewFolder && selectedFolderId.isNotBlank()) ||
                                (isCreatingNewFolder && newFolderName.isNotBlank())
                        )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Save Note")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}