package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.BuildConfig
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.GeminiService
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentSessionId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val initialSessionId: String? = null,
    private val initialSessionTitle: String? = null,
    private val repository: ChatRepository = ChatRepository(),
    private val geminiService: GeminiService = GeminiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            currentSessionId = initialSessionId
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        if (initialSessionId != null) {
            loadMessages(initialSessionId)
        } else {
            startNewSession()
        }
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val messages = repository.fetchMessages(sessionId)
            _uiState.update { it.copy(messages = messages, currentSessionId = sessionId, isLoading = false) }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val session = repository.createSession()
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    currentSessionId = session?.id,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    fun startNewChat() = startNewSession()

    fun updateInput(text: String) {
        // This ViewModel doesn't seem to hold input state in UIState, but let's add it if needed
        // For now, satisfy the caller
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val sessionId = _uiState.value.currentSessionId

        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = "user",
            content = trimmed
        )

        // Optimistically add user message to UI
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            // Persist user message to Supabase
            repository.saveMessage(userMessage)

            // Call Gemini AI via Service
            val botResponseText = geminiService.sendChatMessage(
                history = _uiState.value.messages.dropLast(1), // Don't include the message we just added optimistically
                userPrompt = trimmed
            )

            val botMessage = ChatMessage(
                sessionId = sessionId,
                role = "model",
                content = botResponseText
            )

            // Persist model message to Supabase
            repository.saveMessage(botMessage)

            // Update UI with AI response
            _uiState.update {
                it.copy(
                    messages = it.messages + botMessage,
                    isLoading = false
                )
            }
        }
    }
}