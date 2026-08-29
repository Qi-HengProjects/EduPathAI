package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatHistoryUiState(
    val isLoading: Boolean = false,
    val sessions: List<ChatSession> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class ChatHistoryViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHistoryUiState())
    val uiState: StateFlow<ChatHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    /** Read: fetches sessions, filtered by the current search query (title search). */
    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fetchedSessions = repository.getSessions(_uiState.value.searchQuery)
                _uiState.update { it.copy(isLoading = false, sessions = fetchedSessions, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadSessions()
    }

    /** Update: rename a session/thread. */
    fun renameSession(id: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            try {
                repository.renameSession(id, newTitle.trim())
                loadSessions()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /** Update: star / pin a session so it stays pinned to the top. */
    fun togglePin(session: ChatSession) {
        val id = session.id ?: return
        viewModelScope.launch {
            try {
                repository.toggleSessionPin(id, !session.isPinned)
                loadSessions()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /** Delete: remove an individual chat thread entirely. */
    fun deleteSession(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteSession(id)
                loadSessions()
                _uiState.update { it.copy(snackbarMessage = "Chat deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
