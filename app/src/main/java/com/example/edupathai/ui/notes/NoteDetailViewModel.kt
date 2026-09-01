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
                    val created = noteRepository.createNote(folderId = folderId, title = "Untitled Note", content = "")
                    _uiState.update {
                        it.copy(
                            notes = listOf(created),
                            currentNote = created,
                            currentNoteIndex = 0,
                            noteTitle = created.title,
                            noteContent = created.content,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading notes: ${e.message}") }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { state ->
            val updatedCurrent = state.currentNote?.copy(title = newTitle)
            val updatedList = state.notes.map { note ->
                if (note.id == state.currentNote?.id) note.copy(title = newTitle) else note
            }
            state.copy(
                noteTitle = newTitle,
                currentNote = updatedCurrent,
                notes = updatedList
            )
        }
    }

    fun updateContent(newContent: String) {
        _uiState.update { state ->
            val updatedCurrent = state.currentNote?.copy(content = newContent)
            val updatedList = state.notes.map { note ->
                if (note.id == state.currentNote?.id) note.copy(content = newContent) else note
            }
            state.copy(
                noteContent = newContent,
                currentNote = updatedCurrent,
                notes = updatedList
            )
        }
    }

    fun updateCurrentNoteContent(content: String) = updateContent(content)

    fun selectNote(note: Note) {
        val currentState = _uiState.value
        val currentActiveNote = currentState.currentNote

        // If clicking the same note, do nothing
        if (currentActiveNote?.id == note.id) return

        // Auto-save the currently active note snapshot to the database
        if (currentActiveNote != null && currentActiveNote.id != null) {
            val noteToSave = currentActiveNote.copy(
                title = currentState.noteTitle,
                content = currentState.noteContent
            )
            viewModelScope.launch {
                noteRepository.saveNote(noteToSave)
            }
        }

        // Find the fresh in-memory version of target note
        val freshNote = currentState.notes.find { it.id == note.id } ?: note
        val index = currentState.notes.indexOfFirst { it.id == note.id }.coerceAtLeast(0)

        _uiState.update {
            it.copy(
                currentNote = freshNote,
                currentNoteIndex = index,
                noteTitle = freshNote.title,
                noteContent = freshNote.content,
                aiMode = AiIslandMode.NONE // Reset AI island when switching notes
            )
        }
    }

    fun selectNoteTab(index: Int) {
        val notes = _uiState.value.notes
        if (index in notes.indices) {
            selectNote(notes[index])
        }
    }

    fun saveCurrentNote() {
        val state = _uiState.value
        val current = state.currentNote ?: return
        val noteToPersist = current.copy(
            title = state.noteTitle.ifBlank { "Untitled Note" },
            content = state.noteContent
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val saved = noteRepository.saveNote(noteToPersist)
            _uiState.update { latest ->
                val updatedList = latest.notes.map { if (it.id == saved.id) saved else it }
                val isStillActive = latest.currentNote?.id == saved.id
                latest.copy(
                    notes = updatedList,
                    currentNote = if (isStillActive) saved else latest.currentNote,
                    noteTitle = if (isStillActive) saved.title else latest.noteTitle,
                    noteContent = if (isStillActive) saved.content else latest.noteContent,
                    isSaving = false,
                    userNotification = "Saved successfully!"
                )
            }
        }
    }

    fun createNewNote() {
        val state = _uiState.value
        val active = state.currentNote

        // Prevent generating redundant empty notes if current note has never been edited
        if (active != null &&
            (active.title == "Untitled Note" || active.title == "New Note") &&
            active.content.isBlank() &&
            state.noteTitle.isBlank() &&
            state.noteContent.isBlank()
        ) {
            _uiState.update { it.copy(userNotification = "Current note is already empty.") }
            return
        }

        // Auto-save the existing active note
        if (active != null && active.id != null) {
            val toSave = active.copy(title = state.noteTitle, content = state.noteContent)
            viewModelScope.launch { noteRepository.saveNote(toSave) }
        }

        viewModelScope.launch {
            val newNote = noteRepository.createNote(folderId = folderId, title = "New Note", content = "")
            _uiState.update { latest ->
                val updatedList = latest.notes + newNote
                latest.copy(
                    notes = updatedList,
                    currentNote = newNote,
                    currentNoteIndex = updatedList.size - 1,
                    noteTitle = newNote.title,
                    noteContent = newNote.content,
                    aiMode = AiIslandMode.NONE,
                    userNotification = "New note created"
                )
            }
        }
    }

    fun createNewNoteTab() = createNewNote()

    fun deleteCurrentNote() {
        val state = _uiState.value
        val target = state.currentNote ?: return

        viewModelScope.launch {
            target.id?.let { noteRepository.deleteNote(it) }
            val remaining = state.notes.filterNot { it.id == target.id }
            if (remaining.isNotEmpty()) {
                val first = remaining.first()
                _uiState.update {
                    it.copy(
                        notes = remaining,
                        currentNote = first,
                        currentNoteIndex = 0,
                        noteTitle = first.title,
                        noteContent = first.content,
                        aiMode = AiIslandMode.NONE
                    )
                }
            } else {
                val fresh = noteRepository.createNote(folderId = folderId, title = "Untitled Note", content = "")
                _uiState.update {
                    it.copy(
                        notes = listOf(fresh),
                        currentNote = fresh,
                        currentNoteIndex = 0,
                        noteTitle = fresh.title,
                        noteContent = fresh.content,
                        aiMode = AiIslandMode.NONE
                    )
                }
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