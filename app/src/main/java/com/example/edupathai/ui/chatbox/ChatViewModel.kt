package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.BuildConfig
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.GeminiService
import com.example.edupathai.data.NoteBookEntry
import com.example.edupathai.data.NoteFolder
import com.example.edupathai.data.NoteRepository
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
    val errorMessage: String? = null,
    val availableFolders: List<NoteFolder> = emptyList(),
    val isSavingNote: Boolean = false,
    val actionFeedbackMessage: String? = null
)

class ChatViewModel(
    private val initialSessionId: String? = null,
    private val initialSessionTitle: String? = null,
    private val repository: ChatRepository = ChatRepository(),
    private val noteRepository: NoteRepository = NoteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            currentSessionId = initialSessionId
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    init {
        if (initialSessionId != null) {
            loadMessages(initialSessionId)
        } else {
            startNewSession()
        }
        loadAvailableFolders()
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val messages = repository.fetchMessages(sessionId)
                _uiState.update {
                    it.copy(
                        messages = messages,
                        currentSessionId = sessionId,
                        isLoading = false
                    )
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

    fun loadAvailableFolders() {
        viewModelScope.launch {
            val folders = noteRepository.getFolders()
            _uiState.update { it.copy(availableFolders = folders) }
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val sessionId = _uiState.value.currentSessionId
        val userMessage = ChatMessage(sessionId = sessionId, sender = "user", text = trimmed)

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            repository.saveMessage(userMessage)

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

            val botMessage = ChatMessage(sessionId = sessionId, sender = "model", text = botResponseText)
            repository.saveMessage(botMessage)

            _uiState.update {
                it.copy(
                    messages = it.messages + botMessage,
                    isLoading = false
                )
            }
        }
    }

    fun saveAiResponseToNotes(
        folderId: String,
        noteTitle: String,
        noteContent: String,
        onSuccess: () -> Unit
    ) {
        if (folderId.isBlank() || noteTitle.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingNote = true) }
            try {
                val newNote = NoteBookEntry(
                    folderId = folderId,
                    title = noteTitle.trim(),
                    contentMarkdown = noteContent.trim()
                )
                noteRepository.saveNote(newNote)
                _uiState.update {
                    it.copy(
                        isSavingNote = false,
                        actionFeedbackMessage = "Saved to Notebook successfully!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingNote = false,
                        actionFeedbackMessage = "Failed to save note: ${e.message}"
                    )
                }
            }
        }
    }

    fun createFolderAndSaveNote(
        folderName: String,
        noteTitle: String,
        noteContent: String,
        onSuccess: () -> Unit
    ) {
        if (folderName.isBlank() || noteTitle.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingNote = true) }
            try {
                val createdFolder = noteRepository.addFolder(name = folderName.trim())
                val targetFolderId = createdFolder?.id

                if (!targetFolderId.isNullOrBlank()) {
                    val newNote = NoteBookEntry(
                        folderId = targetFolderId,
                        title = noteTitle.trim(),
                        contentMarkdown = noteContent.trim()
                    )
                    noteRepository.saveNote(newNote)
                    val updatedFolders = noteRepository.getFolders()
                    _uiState.update {
                        it.copy(
                            availableFolders = updatedFolders,
                            isSavingNote = false,
                            actionFeedbackMessage = "Folder and Note saved successfully!"
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSavingNote = false,
                            actionFeedbackMessage = "Failed to create folder."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingNote = false,
                        actionFeedbackMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(actionFeedbackMessage = null) }
    }
}