package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.SubjectFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val isLoading: Boolean = false,
    val folders: List<SubjectFolder> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class NotesViewModel(
    private val repository: NoteRepository = NoteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        // Automatically fetch folders when ViewModel is first created
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fetchedFolders = repository.getFolders()
                _uiState.update {
                    it.copy(isLoading = false, folders = fetchedFolders)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.addFolder(name)
                // Refresh list after adding
                loadFolders()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}