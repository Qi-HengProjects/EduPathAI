package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ChatUiState(
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "AI Study Assistant",
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isLoading: Boolean = false,
    val availableFolders: List<NoteFolder> = emptyList(),
    val notificationMessage: String? = null,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val noteRepository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadLatestSession()
    }

    private fun loadLatestSession() {
        viewModelScope.launch {
            val sessions = chatRepository.getSessions()
            if (sessions.isNotEmpty()) {
                val latest = sessions.first()
                selectSession(latest.id ?: "", latest.title)
            } else {
                createNewSession()
            }
        }
    }

    fun selectSession(sessionId: String, title: String) {
        if (sessionId.isBlank() || sessionId == "NEW") {
            createNewSession()
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentSessionId = sessionId,
                    currentSessionTitle = title,
                    messages = emptyList(),
                    isLoading = true
                )
            }
            val msgs = chatRepository.getMessages(sessionId)
            _uiState.update {
                it.copy(
                    messages = msgs,
                    isLoading = false
                )
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newSession = chatRepository.createSession("New Conversation")
            _uiState.update {
                it.copy(
                    currentSessionId = newSession.id,
                    currentSessionTitle = newSession.title,
                    messages = emptyList(),
                    userInput = ""
                )
            }
        }
    }

    fun updateUserInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }

    fun sendMessage() {
        val query = _uiState.value.userInput.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            var sId = _uiState.value.currentSessionId
            if (sId.isNullOrBlank() || sId == "NEW") {
                val smartTitle = if (query.length > 28) query.take(25).trim() + "..." else query.trim()
                val createdSession = chatRepository.createSession(smartTitle)
                sId = createdSession.id ?: UUID.randomUUID().toString()
                _uiState.update {
                    it.copy(
                        currentSessionId = sId,
                        currentSessionTitle = createdSession.title
                    )
                }
            }

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sId,
                sender = "user",
                message = query,
                createdAt = Instant.now().toString()
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage,
                    userInput = "",
                    isLoading = true
                )
            }

            chatRepository.sendMessage(userMessage)

            val reply = GeminiService.sendChatMessage(_uiState.value.messages, query)

            val aiMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sId,
                sender = "assistant",
                message = reply,
                createdAt = Instant.now().toString()
            )

            chatRepository.sendMessage(aiMessage)

            val currentTitle = _uiState.value.currentSessionTitle
            if (currentTitle == "New Conversation" || currentTitle == "AI Study Assistant" || currentTitle.isBlank()) {
                val smartTitle = if (query.length > 28) query.take(25).trim() + "..." else query.trim()
                chatRepository.renameSession(sId, smartTitle)
                _uiState.update { it.copy(currentSessionTitle = smartTitle) }
            }

            _uiState.update {
                it.copy(
                    messages = it.messages + aiMessage,
                    isLoading = false
                )
            }
        }
    }

    fun stopThinking() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun loadFolders() {
        viewModelScope.launch {
            val folders = noteRepository.getFolders()
            _uiState.update { it.copy(availableFolders = folders) }
        }
    }

    fun saveMessageToNote(messageText: String, folderId: String, noteTitle: String) {
        viewModelScope.launch {
            val newNote = Note(
                folderId = folderId,
                title = noteTitle.ifBlank { "AI Generated Note" },
                content = messageText
            )
            noteRepository.saveNote(newNote)
            _uiState.update { it.copy(notificationMessage = "Saved to notebook successfully!") }
        }
    }

    fun createFolderAndSaveNote(messageText: String, folderName: String, colorHex: String, noteTitle: String) {
        viewModelScope.launch {
            val folder = noteRepository.createFolder(folderName, colorHex)
            if (folder?.id != null) {
                saveMessageToNote(messageText, folder.id, noteTitle)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to create folder") }
            }
        }
    }

    fun scheduleStudySession(
        title: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        colorHex: String,
        date: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            val task = ScheduleTask(
                title = title,
                startTime = startTime,
                endTime = endTime,
                energyLevel = energyLevel,
                taskType = "study",
                colorHex = colorHex,
                createdAt = "${date}T${startTime}:00Z"
            )
            scheduleRepository.createTask(task)
            _uiState.update { it.copy(notificationMessage = "Scheduled task to Timeline!") }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, errorMessage = null) }
    }
}