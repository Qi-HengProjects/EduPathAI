package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.NoteFolder
import com.example.edupathai.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val folders: List<NoteFolder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class NotesViewModel(
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val list = noteRepository.getFolders()
                _uiState.update { it.copy(folders = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            val created = noteRepository.createFolder(name, colorHex)
            if (created != null) {
                _uiState.update { it.copy(folders = listOf(created) + it.folders) }
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            noteRepository.deleteFolder(folderId)
            _uiState.update { it.copy(folders = it.folders.filter { f -> f.id != folderId }) }
        }
    }
}