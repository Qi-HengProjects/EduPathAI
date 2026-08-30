package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.Flashcard
import com.example.edupathai.data.GeminiService
import com.example.edupathai.data.MindmapData
import com.example.edupathai.data.NoteBookEntry
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.QuizQuestion
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class AiIslandMode {
    NONE, SIMPLIFY, FLASHCARDS, MINDMAP, QUIZ
}

data class NoteDetailUiState(
    val notes: List<NoteBookEntry> = emptyList(),
    val currentNote: NoteBookEntry? = null,
    val isLoading: Boolean = false,
    val isAiProcessing: Boolean = false,
    val aiMode: AiIslandMode = AiIslandMode.NONE,
    val simplifiedText: String? = null,
    val flashcards: List<Flashcard> = emptyList(),
    val mindmapData: MindmapData? = null,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val userNotification: String? = null,
    val errorMessage: String? = null
)

class NoteDetailViewModel(
    val folderId: String,
    private val repository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val notes = repository.getNotes(folderId)
                val selected = notes.firstOrNull() ?: NoteBookEntry(
                    folderId = folderId,
                    title = "Untitled Note",
                    contentMarkdown = ""
                )
                _uiState.update {
                    it.copy(
                        notes = notes,
                        currentNote = selected,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectNote(note: NoteBookEntry) {
        _uiState.update {
            it.copy(
                currentNote = note,
                aiMode = AiIslandMode.NONE,
                simplifiedText = null,
                flashcards = emptyList(),
                mindmapData = null,
                quizQuestions = emptyList()
            )
        }
    }

    fun createNewNote() {
        val newNote = NoteBookEntry(
            folderId = folderId,
            title = "New Note",
            contentMarkdown = ""
        )
        _uiState.update {
            it.copy(
                currentNote = newNote,
                aiMode = AiIslandMode.NONE
            )
        }
    }

    fun updateCurrentNoteContent(title: String, content: String) {
        val active = _uiState.value.currentNote ?: NoteBookEntry(folderId = folderId)
        _uiState.update {
            it.copy(
                currentNote = active.copy(
                    title = title,
                    contentMarkdown = content
                )
            )
        }
    }

    fun saveCurrentNote(onComplete: () -> Unit = {}) {
        val noteToSave = _uiState.value.currentNote ?: return
        viewModelScope.launch {
            try {
                repository.saveNote(noteToSave)
                val updatedNotes = repository.getNotes(folderId)
                _uiState.update {
                    it.copy(
                        notes = updatedNotes,
                        userNotification = "Note saved successfully"
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun deleteCurrentNote() {
        val noteId = _uiState.value.currentNote?.id ?: return
        viewModelScope.launch {
            try {
                repository.deleteNote(noteId)
                loadNotes()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun simplifyNote() {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiMode = AiIslandMode.SIMPLIFY) }
            try {
                val result = GeminiService.simplifyNote(content)
                _uiState.update {
                    it.copy(
                        simplifiedText = result,
                        isAiProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        errorMessage = "AI Simplify error: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateFlashcards() {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiMode = AiIslandMode.FLASHCARDS) }
            try {
                val cards = GeminiService.generateFlashcards(content)
                _uiState.update {
                    it.copy(
                        flashcards = cards,
                        isAiProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        errorMessage = "Flashcards error: ${e.message}"
                    )
                }
            }
        }
    }

    fun generateMindmap() {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiMode = AiIslandMode.MINDMAP) }
            try {
                val map = GeminiService.generateMindmap(content)
                _uiState.update {
                    it.copy(
                        mindmapData = map,
                        isAiProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        errorMessage = "Mindmap error: ${e.message}"
                    )
                }
            }
        }
    }

    fun scheduleNoteTask(taskPrefix: String = "Review") {
        val noteTitle = _uiState.value.currentNote?.title ?: "Note"
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val startTime = now.plusHours(1).format(formatter)
                val endTime = now.plusHours(2).format(formatter)

                val task = ScheduleTask(
                    title = "$taskPrefix: $noteTitle",
                    startTime = startTime,
                    endTime = endTime,
                    energyLevel = "medium",
                    colorHex = "#3B82F6"
                )
                scheduleRepository.createTask(task)
                _uiState.update { it.copy(userNotification = "Scheduled to Daily Timeline!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Scheduling failed: ${e.message}") }
            }
        }
    }

    fun dismissAiIsland() {
        _uiState.update {
            it.copy(
                aiMode = AiIslandMode.NONE,
                simplifiedText = null,
                flashcards = emptyList(),
                mindmapData = null,
                quizQuestions = emptyList()
            )
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null, errorMessage = null) }
    }
}