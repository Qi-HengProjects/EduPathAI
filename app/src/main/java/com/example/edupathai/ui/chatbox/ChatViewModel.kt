package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.BuildConfig
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.GeminiService
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentSessionId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val initialSessionId: String? = null,
    private val initialSessionTitle: String? = null,
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            currentSessionId = initialSessionId
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

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
            try {
                val fetchedMessages = repository.fetchMessages(sessionId)
                _uiState.update {
                    it.copy(isLoading = false, currentSessionId = sessionId, messages = fetchedMessages)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
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

            // Call Gemini AI
            val botResponseText = try {
                withContext(Dispatchers.IO) {
                    if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                        "API Key missing. Please check your local.properties configuration."
                    } else {
                        val response = generativeModel.generateContent(
                            """
                            You are an assistive study tutor.
                            Answer the student's question clearly, directly, and concisely.
                            
                            STRICT RULES:
                            - Do NOT use markdown symbols. Never use asterisks (** or *), hashtags (#), or backticks.
                            - Use standard clean bullet points (• ) if listing items.
                            
                            Student: $trimmed
                            """.trimIndent()
                        )
                        GeminiService.sanitizeText(response.text ?: "Sorry, I couldn't generate a response.")
                    }
                }
            } catch (e: Exception) {
                "Error connecting to AI: ${e.localizedMessage ?: "Unknown error"}"
            }

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
