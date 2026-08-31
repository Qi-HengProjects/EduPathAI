package com.example.edupathai.ui.chatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.ChatMessage
import com.example.edupathai.data.ChatRepository
import com.example.edupathai.data.GeminiService
import com.example.edupathai.data.NoteBookEntry
import com.example.edupathai.data.NoteFolder
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "AI Study Assistant",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val availableFolders: List<NoteFolder> = emptyList(),
    val isSavingNote: Boolean = false,
    val isSchedulingTask: Boolean = false,
    val actionFeedbackMessage: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val noteRepository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        startNewSession()
        loadAvailableFolders()
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun startNewSession() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                currentSessionId = null,
                currentSessionTitle = "AI Study Assistant",
                inputText = "",
                isLoading = false
            )
        }
    }

    fun loadSession(sessionId: String, title: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    currentSessionId = sessionId,
                    currentSessionTitle = title
                )
            }
            val msgs = repository.getMessages(sessionId)
            _uiState.update {
                it.copy(
                    messages = msgs,
                    isLoading = false
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

    fun sendMessage() {
        val trimmed = _uiState.value.inputText.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val isFirstMessageInSession = _uiState.value.currentSessionId == null

        val initialUserMsg = ChatMessage(
            sessionId = _uiState.value.currentSessionId,
            sender = "user",
            text = trimmed
        )

        _uiState.update {
            it.copy(
                messages = it.messages + initialUserMsg,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            var activeSessionId = _uiState.value.currentSessionId

            // Automatically generate a topic-based title on the first message
            if (isFirstMessageInSession || activeSessionId == null) {
                val autoTitle = GeminiService.generateSessionTitle(trimmed)
                val newSession = repository.createSession(title = autoTitle)
                activeSessionId = newSession?.id
                _uiState.update {
                    it.copy(
                        currentSessionId = activeSessionId,
                        currentSessionTitle = autoTitle
                    )
                }
            }

            if (activeSessionId != null) {
                repository.saveMessage(initialUserMsg.copy(sessionId = activeSessionId))
            }

            val aiResponseText = GeminiService.sendChatMessage(trimmed)

            val botMessage = ChatMessage(
                sessionId = activeSessionId,
                sender = "model",
                text = aiResponseText
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + botMessage,
                    isLoading = false
                )
            }

            if (activeSessionId != null) {
                repository.saveMessage(botMessage)
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
                        actionFeedbackMessage = "Failed to save: ${e.message}"
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

    fun scheduleTimelineTask(
        title: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        taskType: String,
        colorHex: String,
        onSuccess: () -> Unit
    ) {
        if (title.isBlank() || startTime.isBlank() || endTime.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSchedulingTask = true) }
            try {
                val newTask = ScheduleTask(
                    title = title.trim(),
                    startTime = startTime.trim(),
                    endTime = endTime.trim(),
                    energyLevel = energyLevel,
                    taskType = taskType,
                    colorHex = colorHex,
                    isCompleted = false
                )
                scheduleRepository.createTask(newTask)
                _uiState.update {
                    it.copy(
                        isSchedulingTask = false,
                        actionFeedbackMessage = "Scheduled into Daily Timeline!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSchedulingTask = false,
                        actionFeedbackMessage = "Failed to schedule: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(actionFeedbackMessage = null) }
    }
}