package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.ChatSession
import com.example.edupathai.data.GeminiService
import com.example.edupathai.data.Note
import com.example.edupathai.data.NoteFolder
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "Study Assistant",
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isLoading: Boolean = false,
    val isSavingNote: Boolean = false,
    val isSchedulingTask: Boolean = false,
    val availableFolders: List<NoteFolder> = emptyList(),
    val notificationMessage: String? = null,
    val actionFeedbackMessage: String? = null
) {
    val inputText: String get() = userInput
}

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val noteRepository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentChatJob: Job? = null

    init {
        loadSessions()
        loadFolders()
    }

    fun loadSessions() {
        viewModelScope.launch {
            val sessions = chatRepository.getSessions()
            _uiState.update { it.copy(sessions = sessions) }
            if (sessions.isNotEmpty() && _uiState.value.currentSessionId == null) {
                val firstSession = sessions.first()
                selectSession(firstSession.id, firstSession.title)
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            val folders = noteRepository.getFolders()
            _uiState.update { it.copy(availableFolders = folders) }
        }
    }

    fun loadAvailableFolders() = loadFolders()

    fun selectSession(sessionId: String, title: String) {
        stopThinking()
        _uiState.update { it.copy(currentSessionId = sessionId, currentSessionTitle = title) }
        viewModelScope.launch {
            val messages = chatRepository.getMessages(sessionId)
            _uiState.update { it.copy(messages = messages) }
        }
    }

    fun loadSession(sessionId: String, title: String) = selectSession(sessionId, title)

    fun createNewSession() {
        stopThinking()
        viewModelScope.launch {
            val newSession = chatRepository.createSession("New Conversation")
            if (newSession != null) {
                _uiState.update {
                    it.copy(
                        sessions = listOf(newSession) + it.sessions,
                        currentSessionId = newSession.id,
                        currentSessionTitle = newSession.title,
                        messages = emptyList()
                    )
                }
            }
        }
    }

    fun startNewSession() = createNewSession()

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            val remaining = _uiState.value.sessions.filter { it.id != sessionId }
            _uiState.update { it.copy(sessions = remaining) }
            if (_uiState.value.currentSessionId == sessionId) {
                if (remaining.isNotEmpty()) {
                    val first = remaining.first()
                    selectSession(first.id, first.title)
                } else {
                    createNewSession()
                }
            }
        }
    }

    fun updateUserInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }

    fun updateInputText(input: String) = updateUserInput(input)

    fun sendMessage() {
        val text = _uiState.value.userInput.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        var activeSessionId = _uiState.value.currentSessionId

        currentChatJob = viewModelScope.launch {
            _uiState.update { it.copy(userInput = "", isLoading = true) }

            try {
                if (activeSessionId == null) {
                    val title = GeminiService.generateSessionTitle(text)
                    val newSession = chatRepository.createSession(title)
                    if (newSession != null) {
                        activeSessionId = newSession.id
                        _uiState.update {
                            it.copy(
                                currentSessionId = newSession.id,
                                currentSessionTitle = newSession.title,
                                sessions = listOf(newSession) + it.sessions
                            )
                        }
                    }
                }

                val sessionId = activeSessionId ?: return@launch

                val userMsg = ChatMessage(sessionId = sessionId, sender = "user", message = text)
                chatRepository.sendMessage(userMsg)
                _uiState.update { it.copy(messages = it.messages + userMsg) }

                val responseText = GeminiService.sendChatMessage(_uiState.value.messages, text)
                val aiMsg = ChatMessage(sessionId = sessionId, sender = "ai", message = responseText)
                chatRepository.sendMessage(aiMsg)

                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMsg,
                        isLoading = false
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // User intentionally pressed "Stop Thinking"
                _uiState.update { it.copy(isLoading = false, notificationMessage = "Generation stopped.") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notificationMessage = "Error connecting to AI: ${e.message}"
                    )
                }
            }
        }
    }

    fun stopThinking() {
        currentChatJob?.cancel()
        currentChatJob = null
        _uiState.update { it.copy(isLoading = false) }
    }

    fun saveMessageToNote(
        messageText: String,
        folderId: String,
        noteTitle: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingNote = true) }
            val entry = Note(
                folderId = folderId,
                title = noteTitle.ifBlank { "Saved AI Note" },
                content = messageText
            )
            val result = noteRepository.saveNote(entry)
            _uiState.update {
                it.copy(
                    isSavingNote = false,
                    notificationMessage = if (result != null) "Note saved successfully!" else "Failed to save note"
                )
            }
            if (result != null) onSuccess?.invoke()
        }
    }

    fun saveAiResponseToNotes(
        folderId: String,
        noteTitle: String,
        noteContent: String,
        onSuccess: (() -> Unit)? = null
    ) = saveMessageToNote(noteContent, folderId, noteTitle, onSuccess)

    fun createFolderAndSaveNote(
        messageText: String,
        folderName: String,
        colorHex: String,
        noteTitle: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingNote = true) }
            val newFolder = noteRepository.createFolder(folderName, colorHex)
            if (newFolder != null) {
                val entry = Note(
                    folderId = newFolder.id,
                    title = noteTitle.ifBlank { "Saved AI Note" },
                    content = messageText
                )
                noteRepository.saveNote(entry)
                loadFolders()
                _uiState.update {
                    it.copy(
                        isSavingNote = false,
                        notificationMessage = "Note saved to new folder!"
                    )
                }
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSavingNote = false,
                        notificationMessage = "Failed to create folder"
                    )
                }
            }
        }
    }

    fun scheduleStudySession(
        title: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        colorHex: String,
        taskType: String = "study",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSchedulingTask = true) }
            val task = ScheduleTask(
                title = title,
                startTime = startTime,
                endTime = endTime,
                energyLevel = energyLevel,
                colorHex = colorHex
            )
            val created = scheduleRepository.createTask(task)
            _uiState.update {
                it.copy(
                    isSchedulingTask = false,
                    notificationMessage = if (created != null) "Scheduled on your Timeline!" else "Failed to schedule task"
                )
            }
            if (created != null) onSuccess?.invoke()
        }
    }

    fun scheduleTimelineTask(
        title: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        taskType: String,
        colorHex: String,
        onSuccess: (() -> Unit)? = null
    ) = scheduleStudySession(title, startTime, endTime, energyLevel, colorHex, taskType, onSuccess)

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, actionFeedbackMessage = null) }
    }

    fun clearFeedbackMessage() = clearNotification()
}