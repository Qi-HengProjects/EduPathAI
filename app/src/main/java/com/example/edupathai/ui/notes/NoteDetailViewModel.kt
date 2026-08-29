package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NoteDetailUiState(
    val noteId: String? = null,
    val folderId: String = "",
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isAiProcessing: Boolean = false,
    val activeAiAction: AiPromptType? = null,
    val simplifiedJargon: String? = null,
    val activeRecallQuiz: String? = null,
    val mindmapData: MindmapData? = null, // Visual Node Diagram Data
    val snackbarMessage: String? = null
)

class NoteDetailViewModel(
    private val folderId: String,
    private val repository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository(),
    private val geminiService: GeminiService = GeminiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState(folderId = folderId))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun runAiAction(actionType: AiPromptType) {
        val currentContent = _uiState.value.content
        if (currentContent.trim().isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please write some note content first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, activeAiAction = actionType) }

            val resultText = try {
                geminiService.processNoteContent(actionType, currentContent)
            } catch (e: Throwable) {
                "Error processing AI request: ${e.message}"
            }

            _uiState.update { state ->
                when (actionType) {
                    AiPromptType.SIMPLIFY_JARGON -> state.copy(
                        isAiProcessing = false,
                        activeAiAction = null,
                        simplifiedJargon = resultText,
                        snackbarMessage = "Simplified explanation ready!"
                    )
                    AiPromptType.GENERATE_QUIZ -> state.copy(
                        isAiProcessing = false,
                        activeAiAction = null,
                        activeRecallQuiz = resultText,
                        snackbarMessage = "Quiz flashcards ready!"
                    )
                    AiPromptType.MINDMAP -> state.copy(
                        isAiProcessing = false,
                        activeAiAction = null,
                        mindmapData = geminiService.parseMindmapData(resultText, state.title),
                        snackbarMessage = "Visual Mindmap diagram generated!"
                    )
                }
            }
        }
    }

    fun scheduleReviewTask(actionType: AiPromptType) {
        viewModelScope.launch {
            val noteTitle = _uiState.value.title.ifBlank { "Study Notes" }
            val taskTitle = when (actionType) {
                AiPromptType.SIMPLIFY_JARGON -> "Review Simplified: $noteTitle"
                AiPromptType.GENERATE_QUIZ -> "Practice Flashcards: $noteTitle"
                AiPromptType.MINDMAP -> "Study Mindmap: $noteTitle"
            }

            val today = LocalDate.now().toString()
            val task = ScheduleTask(
                title = taskTitle,
                startTime = "${today}T14:00:00",
                endTime = "${today}T14:45:00",
                energyLevel = "medium",
                taskType = "study",
                isCompleted = false
            )

            try {
                scheduleRepository.addTask(task)
                _uiState.update { it.copy(snackbarMessage = "Added to Daily Timeline!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Failed to schedule: ${e.message}") }
            }
        }
    }

    fun clearAiIsland(actionType: AiPromptType) {
        _uiState.update { state ->
            when (actionType) {
                AiPromptType.SIMPLIFY_JARGON -> state.copy(simplifiedJargon = null)
                AiPromptType.GENERATE_QUIZ -> state.copy(activeRecallQuiz = null)
                AiPromptType.MINDMAP -> state.copy(mindmapData = null)
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun saveNote(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = NoteBookEntry(
                id = state.noteId,
                folderId = state.folderId,
                title = state.title.ifBlank { "Untitled Note" },
                contentMarkdown = state.content
            )
            try {
                repository.saveNote(entry)
                _uiState.update { it.copy(snackbarMessage = "Note saved successfully!") }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Failed to save: ${e.message}") }
            }
        }
    }
}