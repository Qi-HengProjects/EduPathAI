package com.example.edupathai.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edupathai.data.SubjectFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesDirectoryScreen(
    viewModel: NotesViewModel = viewModel(),
    onFolderClick: (folderId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subject Notebooks") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                uiState.folders.isEmpty() -> {
                    Text(
                        text = "No subjects yet. Tap '+' to create your first folder!",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.folders) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { folder.id?.let { onFolderClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for creating a new subject folder
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Subject Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Subject Name (e.g. Biology)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            newFolderName = ""
                            showDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FolderCard(
    folder: SubjectFolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.subjectName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${folder.noteCount} notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}