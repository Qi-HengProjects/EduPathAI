package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_CHAT_TITLE = "AI Study Buddy"

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val sessionId: String? = null,
    val sessionTitle: String = DEFAULT_CHAT_TITLE,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val errorMessage: String? = null
)

class ChatViewModel(
    private val initialSessionId: String? = null,
    initialSessionTitle: String? = null,
    private val repository: ChatRepository = ChatRepository(),
    private val geminiService: GeminiService = GeminiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            sessionId = initialSessionId,
            sessionTitle = initialSessionTitle ?: DEFAULT_CHAT_TITLE
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        if (initialSessionId != null) {
            loadMessages(initialSessionId)
        }
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fetchedMessages = repository.getMessages(sessionId)
                _uiState.update {
                    it.copy(isLoading = false, sessionId = sessionId, messages = fetchedMessages)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /** Voice prompt: transcript from [VoiceInputButton] is sent immediately, like a typed + submitted message. */
    fun onVoiceTranscript(transcript: String) {
        _uiState.update { it.copy(inputText = transcript) }
        sendMessage()
    }

    fun onVoiceError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /** Create: sends the current draft (text or voice-transcribed) as a new prompt. */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isSending) return

        viewModelScope.launch {
            val historyBeforeSend = _uiState.value.messages
            _uiState.update { it.copy(isSending = true, inputText = "") }

            try {
                var sessionId = _uiState.value.sessionId
                if (sessionId == null) {
                    val newSession = repository.createSession(title = text.take(48).ifBlank { DEFAULT_CHAT_TITLE })
                    sessionId = newSession.id
                    _uiState.update { it.copy(sessionId = sessionId, sessionTitle = newSession.title) }
                }
                val activeSessionId = sessionId ?: return@launch

                val userMessage = repository.createMessage(
                    ChatMessage(sessionId = activeSessionId, sender = "user", content = text)
                )
                _uiState.update { it.copy(messages = it.messages + userMessage) }

                val replyText = try {
                    geminiService.sendChatMessage(historyBeforeSend, text)
                } catch (e: Throwable) {
                    "❌ UNEXPECTED ERROR: ${e.localizedMessage ?: e.javaClass.simpleName}"
                }

                val modelMessage = repository.createMessage(
                    ChatMessage(sessionId = activeSessionId, sender = "model", content = replyText)
                )
                repository.touchSession(activeSessionId)

                _uiState.update { it.copy(isSending = false, messages = it.messages + modelMessage) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, errorMessage = e.message) }
            }
        }
    }

    /** Create: resets this screen to a blank conversation; the next send() lazily creates a new session. */
    fun startNewChat() {
        _uiState.update { ChatUiState(sessionTitle = DEFAULT_CHAT_TITLE) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
