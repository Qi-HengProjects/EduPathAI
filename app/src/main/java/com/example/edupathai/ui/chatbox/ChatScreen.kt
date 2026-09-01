package com.example.edupathai.ui.chatbox

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.util.Locale

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

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        textToSpeech = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.updateUserInput(spokenText)
            }
        }
    }

    LaunchedEffect(initialSessionId) {
        if (!initialSessionId.isNullOrBlank() && initialSessionId != uiState.currentSessionId) {
            viewModel.selectSession(initialSessionId, initialSessionTitle ?: "AI Study Assistant")
        }
    }

    var showSaveNoteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteContent by rememberSaveable { mutableStateOf("") }
    var selectedNoteTitle by rememberSaveable { mutableStateOf("") }

    var showScheduleDialog by rememberSaveable { mutableStateOf(false) }
    var suggestedTaskTitle by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131C2E)),
                title = {
                    Column {
                        Text(
                            text = uiState.currentSessionTitle.ifBlank { "AI Study Assistant" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Ask questions, voice chat, study plans",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(Icons.Default.AddComment, contentDescription = "New Session", tint = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF94A3B8))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0B0F19))
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "How can I help your studies today?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Type or use voice input to explain concepts, plan studies, or review.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                items(uiState.messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        onSpeakText = { text ->
                            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID_${System.currentTimeMillis()}")
                        },
                        onSaveToNote = { content ->
                            selectedNoteContent = content
                            selectedNoteTitle = "AI Note: " + content.take(24).replace("\n", " ").trim() + "..."
                            viewModel.loadFolders()
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                            Text(
                                text = "AI is thinking...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.stopThinking() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Thinking", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Surface(
                color = Color(0xFF131C2E),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.userInput,
                        onValueChange = { viewModel.updateUserInput(it) },
                        placeholder = { Text("Ask a question or request a study plan...", color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your study query...")
                                        }
                                        speechLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Voice input not supported on this device", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    if (uiState.isLoading) {
                        IconButton(
                            onClick = { viewModel.stopThinking() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(color = Color(0xFFEF4444), shape = CircleShape)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = uiState.userInput.isNotBlank(),
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = if (uiState.userInput.isNotBlank()) Color(0xFF3B82F6) else Color(0xFF1E293B),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (uiState.userInput.isNotBlank()) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveNoteDialog) {
        SaveAiToNotesDialog(
            initialTitle = selectedNoteTitle,
            initialContent = selectedNoteContent,
            availableFolders = uiState.availableFolders,
            isSaving = uiState.isLoading,
            onDismiss = { showSaveNoteDialog = false },
            onSave = { folderId, title, content ->
                viewModel.saveMessageToNote(
                    messageText = content,
                    folderId = folderId,
                    noteTitle = title
                )
                showSaveNoteDialog = false
            },
            onCreateFolderAndSave = { folderName, title, content ->
                viewModel.createFolderAndSaveNote(
                    messageText = content,
                    folderName = folderName,
                    colorHex = "#3B82F6",
                    noteTitle = title
                )
                showSaveNoteDialog = false
            }
        )
    }

    if (showScheduleDialog) {
        ConfirmSchedulePlanDialog(
            initialTitle = suggestedTaskTitle,
            isSaving = uiState.isLoading,
            onDismiss = { showScheduleDialog = false },
            onConfirm = { title, start, end, energy, _, colorHex ->
                viewModel.scheduleStudySession(
                    title = title,
                    startTime = start,
                    endTime = end,
                    energyLevel = energy,
                    colorHex = colorHex
                )
                showScheduleDialog = false
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onSpeakText: (String) -> Unit,
    onSaveToNote: (String) -> Unit,
    onSchedulePlan: (String) -> Unit
) {
    val isUser = message.sender.equals("user", ignoreCase = true)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val messageText = message.message.ifBlank { message.content }

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
            color = if (isUser) Color(0xFF3B82F6) else Color(0xFF131C2E),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)) else null,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = messageText,
                    color = Color.White,
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
                    onClick = { onSpeakText(messageText) },
                    label = { Text("Listen", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA855F7)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Read aloud",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFA855F7)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )

                AssistChip(
                    onClick = { onSchedulePlan(messageText) },
                    label = { Text("Schedule", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF38BDF8)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )

                AssistChip(
                    onClick = { onSaveToNote(messageText) },
                    label = { Text("Save Note", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF10B981)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )

                AssistChip(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(messageText))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("Copy", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF94A3B8)
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
        containerColor = Color(0xFF131C2E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Schedule Task", fontWeight = FontWeight.Bold, color = Color.White)
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
                    color = Color(0xFF94A3B8)
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

                Text("Energy Required", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
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

                Text("Task Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
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

                Text("Color Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    taskColors.forEach { hex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Color(0xFF3B82F6)
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
                enabled = !isSaving && taskTitle.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Confirm & Add", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel", color = Color(0xFF94A3B8))
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
        containerColor = Color(0xFF131C2E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Notebook", fontWeight = FontWeight.Bold, color = Color.White)
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
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF131C2E))
                        ) {
                            availableFolders.forEach { folder ->
                                val fId = folder.id
                                DropdownMenuItem(
                                    text = { Text(folder.name, color = Color.White) },
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
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Create New Folder", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8))
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
                            Text("Choose Existing Folder", style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8))
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
                        ),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}