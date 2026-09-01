package com.example.edupathai.ui.chatbox

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.NoteFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var messageToSaveText by rememberSaveable { mutableStateOf("") }
    var noteTitleInput by rememberSaveable { mutableStateOf("") }
    var selectedFolderId by rememberSaveable { mutableStateOf("") }
    var isCreatingNewFolder by rememberSaveable { mutableStateOf(false) }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var newFolderColorHex by rememberSaveable { mutableStateOf("#3B82F6") }

    LaunchedEffect(Unit) {
        viewModel.loadFolders()
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.currentSessionTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(Icons.Default.AddComment, contentDescription = "New Chat", tint = Color(0xFF60A5FA))
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
        ) {
            // Transcript Message List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "EduPath AI Assistant",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask questions, generate flashcards, request summaries, or build study timelines.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages) { msg ->
                            ChatMessageBubble(
                                message = msg,
                                onSaveToNotes = {
                                    messageToSaveText = msg.message.ifBlank { msg.content }
                                    noteTitleInput = "AI Note - ${uiState.currentSessionTitle}"
                                    if (uiState.availableFolders.isNotEmpty()) {
                                        selectedFolderId = uiState.availableFolders.first().id
                                    }
                                    showSaveDialog = true
                                },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(msg.message.ifBlank { msg.content }))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (uiState.isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF3B82F6),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Thinking...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { viewModel.stopThinking() }) {
                                        Text("Stop", color = Color(0xFFEF4444), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = Color(0xFF131C2E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoiceInputButton(
                        onResult = { text: String ->
                            val current = uiState.userInput
                            viewModel.updateUserInput(if (current.isBlank()) text else "$current $text")
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = uiState.userInput,
                        onValueChange = { viewModel.updateUserInput(it) },
                        placeholder = { Text("Ask anything...", color = Color(0xFF64748B), fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF0B0F19),
                            unfocusedContainerColor = Color(0xFF0B0F19),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = uiState.userInput.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (uiState.userInput.isNotBlank() && !uiState.isLoading) Color(0xFF3B82F6) else Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Save Note Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                containerColor = Color(0xFF131C2E),
                title = { Text("Save to Notebook", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = noteTitleInput,
                            onValueChange = { noteTitleInput = it },
                            label = { Text("Note Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isCreatingNewFolder,
                                onClick = { isCreatingNewFolder = false },
                                label = { Text("Existing Folder", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = isCreatingNewFolder,
                                onClick = { isCreatingNewFolder = true },
                                label = { Text("New Folder", fontSize = 12.sp) }
                            )
                        }

                        if (isCreatingNewFolder) {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text("Folder Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444").forEach { hex ->
                                    val isSelected = newFolderColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { newFolderColorHex = hex }
                                    )
                                }
                            }
                        } else {
                            if (uiState.availableFolders.isEmpty()) {
                                Text("No folders available. Please create a new folder.", color = Color(0xFFEF4444), fontSize = 12.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    uiState.availableFolders.forEach { folder: NoteFolder ->
                                        val isSelected = selectedFolderId == folder.id
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF0B0F19),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedFolderId = folder.id }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            try {
                                                                Color(android.graphics.Color.parseColor(folder.colorHex))
                                                            } catch (_: Exception) {
                                                                Color(0xFF3B82F6)
                                                            }
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(folder.name, color = Color.White, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isCreatingNewFolder) {
                                if (newFolderName.isNotBlank()) {
                                    viewModel.createFolderAndSaveNote(
                                        messageText = messageToSaveText,
                                        folderName = newFolderName.trim(),
                                        colorHex = newFolderColorHex,
                                        noteTitle = noteTitleInput.trim()
                                    )
                                    showSaveDialog = false
                                }
                            } else {
                                if (selectedFolderId.isNotBlank()) {
                                    viewModel.saveMessageToNote(
                                        messageText = messageToSaveText,
                                        folderId = selectedFolderId,
                                        noteTitle = noteTitleInput.trim()
                                    )
                                    showSaveDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Save Note", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onSaveToNotes: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "user"
    val textContent = message.message.ifBlank { message.content }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            color = if (isUser) Color(0xFF2563EB) else Color(0xFF131C2E),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = textContent,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        if (!isUser && textContent.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(onClick = onSaveToNotes, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save Note",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}