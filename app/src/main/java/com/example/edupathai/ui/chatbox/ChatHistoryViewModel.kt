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
    val sessions: List<ChatSession> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatHistoryViewModel(
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHistoryUiState())
    val uiState: StateFlow<ChatHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val list = chatRepository.getSessions()
                _uiState.update { it.copy(sessions = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun createNewSession(title: String = "New Conversation") {
        viewModelScope.launch {
            val session = chatRepository.createSession(title = title, isPinned = false)
            if (session != null) {
                _uiState.update { it.copy(sessions = listOf(session) + it.sessions) }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, newTitle.trim())
            _uiState.update { state ->
                val updated = state.sessions.map {
                    if (it.id == sessionId) it.copy(title = newTitle.trim()) else it
                }
                state.copy(sessions = updated)
            }
        }
    }

    fun toggleSessionPin(sessionId: String, currentPinnedState: Boolean) {
        viewModelScope.launch {
            val newPinState = !currentPinnedState
            chatRepository.togglePinSession(sessionId, newPinState)
            _uiState.update { state ->
                val updated = state.sessions.map {
                    if (it.id == sessionId) it.copy(isPinned = newPinState) else it
                }.sortedByDescending { it.isPinned }
                state.copy(sessions = updated)
            }
        }
    }

    fun togglePinSession(sessionId: String, isPinned: Boolean) = toggleSessionPin(sessionId, !isPinned)

    fun togglePin(session: ChatSession) {
        toggleSessionPin(session.id, session.isPinned)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            _uiState.update { state ->
                state.copy(sessions = state.sessions.filter { it.id != sessionId })
            }
        }
    }
}