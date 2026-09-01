package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.AiIslandMode
import com.example.edupathai.data.Flashcard
import com.example.edupathai.data.GeminiService
import com.example.edupathai.data.MindmapData
import com.example.edupathai.data.Note
import com.example.edupathai.data.NoteRepository
import com.example.edupathai.data.QuizQuestion
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class NoteDetailUiState(
    val notes: List<Note> = emptyList(),
    val currentNote: Note? = null,
    val currentNoteIndex: Int = 0,
    val noteTitle: String = "",
    val noteContent: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAiProcessing: Boolean = false,
    val aiMode: AiIslandMode = AiIslandMode.NONE,
    val simplifiedText: String? = null,
    val flashcards: List<Flashcard> = emptyList(),
    val currentFlashcardIndex: Int = 0,
    val isFlashcardFlipped: Boolean = false,
    val mindmapData: MindmapData? = null,
    val mindmap: MindmapData? = null,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val currentQuizIndex: Int = 0,
    val selectedQuizAnswer: String? = null,
    val selectedQuizOption: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val quizScore: Int = 0,
    val isQuizFinished: Boolean = false,
    val userNotification: String? = null,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null
)

class NoteDetailViewModel(
    private val folderId: String = "",
    private val noteRepository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        if (folderId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = noteRepository.getNotesForFolder(folderId)
                if (list.isNotEmpty()) {
                    val first = list.first()
                    _uiState.update {
                        it.copy(
                            notes = list,
                            currentNote = first,
                            currentNoteIndex = 0,
                            noteTitle = first.title,
                            noteContent = first.content,
                            isLoading = false
                        )
                    }
                } else {
                    // Create first note cleanly through repository
                    val created = noteRepository.createNote(folderId = folderId, title = "Untitled Note", content = "")
                    val listWithInitial = if (created != null) listOf(created) else emptyList()
                    _uiState.update {
                        it.copy(
                            notes = listWithInitial,
                            currentNote = created,
                            currentNoteIndex = 0,
                            noteTitle = created?.title ?: "Untitled Note",
                            noteContent = created?.content ?: "",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading notes: ${e.message}") }
            }
        }
    }

    fun selectNote(note: Note) {
        val index = _uiState.value.notes.indexOfFirst { it.id == note.id }.coerceAtLeast(0)
        _uiState.update {
            it.copy(
                currentNote = note,
                currentNoteIndex = index,
                noteTitle = note.title,
                noteContent = note.content
            )
        }
    }

    fun selectNoteTab(index: Int) {
        val notes = _uiState.value.notes
        if (index in notes.indices) {
            val target = notes[index]
            selectNote(target)
        }
    }

    fun createNewNote() {
        val state = _uiState.value
        val activeNote = state.currentNote

        // Check if the current note is already a blank, unedited note to avoid duplicate empty rows
        if (activeNote != null && activeNote.id != null &&
            (activeNote.title == "Untitled Note" || activeNote.title.isBlank()) &&
            activeNote.content.isBlank()
        ) {
            _uiState.update { it.copy(userNotification = "Current note is already empty.") }
            return
        }

        viewModelScope.launch {
            // Save active note if it has unsaved edits
            if (activeNote != null && activeNote.id != null) {
                saveCurrentNote()
            }

            val saved = noteRepository.createNote(folderId = folderId, title = "New Note", content = "")
            if (saved != null) {
                val updatedList = _uiState.value.notes + saved
                val newIndex = updatedList.size - 1
                _uiState.update {
                    it.copy(
                        notes = updatedList,
                        currentNote = saved,
                        currentNoteIndex = newIndex,
                        noteTitle = saved.title,
                        noteContent = saved.content,
                        userNotification = "New note created"
                    )
                }
            }
        }
    }

    fun createNewNoteTab() = createNewNote()

    fun updateCurrentNoteContent(content: String) {
        _uiState.update {
            it.copy(
                noteContent = content,
                currentNote = it.currentNote?.copy(content = content)
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.update {
            it.copy(
                noteTitle = title,
                currentNote = it.currentNote?.copy(title = title)
            )
        }
    }

    fun updateContent(content: String) = updateCurrentNoteContent(content)

    fun saveCurrentNote() {
        val state = _uiState.value
        val currentNotes = state.notes
        if (currentNotes.isEmpty()) return
        val currentNote = currentNotes.getOrNull(state.currentNoteIndex) ?: state.currentNote ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updated = currentNote.copy(
                    title = state.noteTitle.ifBlank { "Untitled Note" },
                    content = state.noteContent
                )
                val persisted = noteRepository.saveNote(updated)
                if (persisted != null) {
                    val updatedList = state.notes.map { if (it.id == persisted.id) persisted else it }
                    _uiState.update {
                        it.copy(
                            notes = updatedList,
                            currentNote = persisted,
                            isSaving = false,
                            userNotification = "Saved successfully!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save note") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save note") }
            }
        }
    }

    fun deleteCurrentNote() {
        val state = _uiState.value
        val notes = state.notes
        if (notes.isEmpty()) return
        val target = notes.getOrNull(state.currentNoteIndex) ?: state.currentNote ?: return

        viewModelScope.launch {
            val targetId = target.id
            if (targetId != null) {
                noteRepository.deleteNote(targetId)
            }
            val remaining = notes.filterNot { it.id == target.id }
            if (remaining.isNotEmpty()) {
                val first = remaining.first()
                _uiState.update {
                    it.copy(
                        notes = remaining,
                        currentNote = first,
                        currentNoteIndex = 0,
                        noteTitle = first.title,
                        noteContent = first.content
                    )
                }
            } else {
                createNewNote()
            }
        }
    }

    fun setAiMode(mode: AiIslandMode) {
        _uiState.update { it.copy(aiMode = mode) }
    }

    fun dismissAiIsland() {
        _uiState.update { it.copy(aiMode = AiIslandMode.NONE) }
    }

    fun generateSimplifiedNotes(forceRegenerate: Boolean = false) {
        val content = _uiState.value.noteContent
        if (content.isBlank()) return
        _uiState.update { it.copy(aiMode = AiIslandMode.SIMPLIFY, isAiProcessing = true) }
        viewModelScope.launch {
            val simplified = GeminiService.simplifyNote(content)
            _uiState.update {
                it.copy(
                    simplifiedText = simplified,
                    isAiProcessing = false
                )
            }
        }
    }

    fun simplifyNote() = generateSimplifiedNotes()

    fun generateFlashcards(forceRegenerate: Boolean = false) {
        val content = _uiState.value.noteContent
        if (content.isBlank()) return
        _uiState.update { it.copy(aiMode = AiIslandMode.FLASHCARDS, isAiProcessing = true) }
        viewModelScope.launch {
            val cards = GeminiService.generateFlashcards(content)
            _uiState.update {
                it.copy(
                    flashcards = cards,
                    currentFlashcardIndex = 0,
                    isFlashcardFlipped = false,
                    isAiProcessing = false
                )
            }
        }
    }

    fun flipFlashcard() {
        _uiState.update { it.copy(isFlashcardFlipped = !it.isFlashcardFlipped) }
    }

    fun nextFlashcard() {
        val state = _uiState.value
        if (state.flashcards.isEmpty()) return
        val next = (state.currentFlashcardIndex + 1) % state.flashcards.size
        _uiState.update { it.copy(currentFlashcardIndex = next, isFlashcardFlipped = false) }
    }

    fun generateMindmap(forceRegenerate: Boolean = false) {
        val content = _uiState.value.noteContent
        if (content.isBlank()) return
        _uiState.update { it.copy(aiMode = AiIslandMode.MINDMAP, isAiProcessing = true) }
        viewModelScope.launch {
            val mapData = GeminiService.generateMindmap(content)
            _uiState.update {
                it.copy(
                    mindmapData = mapData,
                    mindmap = mapData,
                    isAiProcessing = false
                )
            }
        }
    }

    fun generateQuiz(forceRegenerate: Boolean = false) {
        val content = _uiState.value.noteContent
        if (content.isBlank()) return
        _uiState.update { it.copy(aiMode = AiIslandMode.QUIZ, isAiProcessing = true) }
        viewModelScope.launch {
            val questions = GeminiService.generateQuiz(content)
            _uiState.update {
                it.copy(
                    quizQuestions = questions,
                    currentQuizIndex = 0,
                    selectedQuizAnswer = null,
                    selectedQuizOption = null,
                    isAnswerSubmitted = false,
                    quizScore = 0,
                    isQuizFinished = false,
                    isAiProcessing = false
                )
            }
        }
    }

    fun selectQuizAnswer(answer: String) {
        val state = _uiState.value
        if (state.isAnswerSubmitted) return
        val currentQ = state.quizQuestions.getOrNull(state.currentQuizIndex)
        val optionIndex = currentQ?.options?.indexOf(answer)
        _uiState.update {
            it.copy(
                selectedQuizAnswer = answer,
                selectedQuizOption = if (optionIndex != null && optionIndex >= 0) optionIndex else null
            )
        }
    }

    fun submitQuizAnswer() {
        val state = _uiState.value
        if (state.selectedQuizAnswer == null || state.isAnswerSubmitted) return
        val currentQ = state.quizQuestions.getOrNull(state.currentQuizIndex) ?: return
        val isCorrect = currentQ.correctAnswer.equals(state.selectedQuizAnswer, ignoreCase = true)

        _uiState.update {
            it.copy(
                isAnswerSubmitted = true,
                quizScore = if (isCorrect) it.quizScore + 1 else it.quizScore
            )
        }
    }

    fun nextQuizQuestion() {
        val state = _uiState.value
        val nextIdx = state.currentQuizIndex + 1
        if (nextIdx < state.quizQuestions.size) {
            _uiState.update {
                it.copy(
                    currentQuizIndex = nextIdx,
                    selectedQuizAnswer = null,
                    selectedQuizOption = null,
                    isAnswerSubmitted = false
                )
            }
        } else {
            _uiState.update { it.copy(isQuizFinished = true, isAnswerSubmitted = false) }
        }
    }

    fun resetQuiz() {
        _uiState.update {
            it.copy(
                currentQuizIndex = 0,
                selectedQuizAnswer = null,
                selectedQuizOption = null,
                isAnswerSubmitted = false,
                quizScore = 0,
                isQuizFinished = false
            )
        }
    }

    fun scheduleNoteTask(title: String = "", durationMinutes: Int = 30) {
        viewModelScope.launch {
            try {
                val now = LocalTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val start = now.format(formatter)
                val end = now.plusMinutes(durationMinutes.toLong()).format(formatter)
                val taskTitle = title.ifBlank { "Study: ${_uiState.value.noteTitle.ifBlank { "Note Review" }}" }

                val task = ScheduleTask(
                    title = taskTitle,
                    startTime = start,
                    endTime = end,
                    energyLevel = "Medium",
                    colorHex = "#3B82F6"
                )
                scheduleRepository.createTask(task)
                _uiState.update { it.copy(userNotification = "Scheduled task to timeline!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to schedule task") }
            }
        }
    }

    fun clearNotification() {
        _uiState.update {
            it.copy(
                userNotification = null,
                errorMessage = null,
                feedbackMessage = null
            )
        }
    }
}