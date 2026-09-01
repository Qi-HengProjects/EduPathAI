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
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sessions = chatRepository.getSessions()
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun togglePin(session: ChatSession) {
        val sId = session.id ?: return
        togglePin(sId, session.isPinned)
    }

    fun togglePin(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch {
            chatRepository.togglePinSession(sessionId, !isPinned)
            loadSessions()
        }
    }

    fun renameSession(session: ChatSession, newTitle: String) {
        val sId = session.id ?: return
        renameSession(sId, newTitle)
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, newTitle)
            loadSessions()
        }
    }

    fun deleteSession(session: ChatSession) {
        val sId = session.id ?: return
        deleteSession(sId)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            loadSessions()
        }
    }
}