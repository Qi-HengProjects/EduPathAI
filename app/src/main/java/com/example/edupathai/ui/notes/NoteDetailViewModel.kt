package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class NoteViewMode {
    EDITOR, SIMPLIFY, FLASHCARDS, MINDMAP, QUIZ
}

data class NoteDetailUiState(
    val notes: List<Note> = emptyList(),
    val selectedNoteId: String? = null,
    val noteTitle: String = "",
    val noteContent: String = "",
    val viewMode: NoteViewMode = NoteViewMode.EDITOR,
    val simplifiedContent: String = "",
    val flashcards: List<Flashcard> = emptyList(),
    val mindmap: MindmapData? = null,
    val quiz: List<QuizQuestion> = emptyList(),
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val notificationMessage: String? = null,
    val errorMessage: String? = null
)

class NoteDetailViewModel(
    private val folderId: String,
    private val noteRepository: NoteRepository = NoteRepository(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Loading notes...") }
            val notesList = noteRepository.getNotesByFolder(folderId)

            if (notesList.isNotEmpty()) {
                val currentSelectedId = _uiState.value.selectedNoteId
                val targetNote = notesList.find { it.id == currentSelectedId } ?: notesList.first()
                applyNoteToState(targetNote, notesList)
            } else {
                val newNote = Note(
                    id = UUID.randomUUID().toString(),
                    folderId = folderId,
                    title = "New Note",
                    content = "",
                    createdAt = Instant.now().toString()
                )
                noteRepository.saveNote(newNote)
                applyNoteToState(newNote, listOf(newNote))
            }
        }
    }

    private fun applyNoteToState(note: Note, notesList: List<Note>) {
        _uiState.update {
            it.copy(
                notes = notesList,
                selectedNoteId = note.id,
                noteTitle = note.title,
                noteContent = note.content,
                simplifiedContent = note.simplifiedContent,
                flashcards = note.getFlashcards(),
                mindmap = note.getMindmap(),
                quiz = note.getQuiz(),
                isLoading = false,
                loadingMessage = ""
            )
        }
    }

    fun selectNote(noteId: String) {
        saveCurrentNote()
        val note = _uiState.value.notes.find { it.id == noteId } ?: return
        applyNoteToState(note, _uiState.value.notes)
    }

    fun createNewNote() {
        saveCurrentNote()
        viewModelScope.launch {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                folderId = folderId,
                title = "New Note",
                content = "",
                createdAt = Instant.now().toString()
            )
            noteRepository.saveNote(newNote)
            val updatedList = _uiState.value.notes + newNote
            applyNoteToState(newNote, updatedList)
            _uiState.update { it.copy(viewMode = NoteViewMode.EDITOR) }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(noteTitle = title) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(noteContent = content) }
    }

    fun saveCurrentNote() {
        val currentId = _uiState.value.selectedNoteId ?: return
        val currentNote = _uiState.value.notes.find { it.id == currentId } ?: return

        val updatedNote = currentNote.copy(
            title = _uiState.value.noteTitle.ifBlank { "Untitled Note" },
            content = _uiState.value.noteContent,
            simplifiedContent = _uiState.value.simplifiedContent,
            flashcardsJson = if (_uiState.value.flashcards.isNotEmpty()) Json.encodeToString(_uiState.value.flashcards) else "",
            mindmapJson = _uiState.value.mindmap?.let { Json.encodeToString(it) } ?: "",
            quizJson = if (_uiState.value.quiz.isNotEmpty()) Json.encodeToString(_uiState.value.quiz) else ""
        )

        viewModelScope.launch {
            noteRepository.saveNote(updatedNote)
            val updatedList = _uiState.value.notes.map { if (it.id == currentId) updatedNote else it }
            _uiState.update { it.copy(notes = updatedList, notificationMessage = "Saved!") }
        }
    }

    fun deleteCurrentNote() {
        val currentId = _uiState.value.selectedNoteId ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(currentId)
            loadNotes()
        }
    }

    fun toggleViewMode(mode: NoteViewMode) {
        if (_uiState.value.viewMode == mode) {
            _uiState.update { it.copy(viewMode = NoteViewMode.EDITOR) }
            return
        }

        val content = _uiState.value.noteContent.trim()

        when (mode) {
            NoteViewMode.EDITOR -> {
                _uiState.update { it.copy(viewMode = NoteViewMode.EDITOR) }
            }
            NoteViewMode.SIMPLIFY -> {
                if (_uiState.value.simplifiedContent.isNotBlank()) {
                    _uiState.update { it.copy(viewMode = NoteViewMode.SIMPLIFY) }
                } else if (content.isNotBlank()) {
                    generateSimplify()
                } else {
                    _uiState.update { it.copy(errorMessage = "Add some note content first!") }
                }
            }
            NoteViewMode.FLASHCARDS -> {
                if (_uiState.value.flashcards.isNotEmpty()) {
                    _uiState.update { it.copy(viewMode = NoteViewMode.FLASHCARDS) }
                } else if (content.isNotBlank()) {
                    generateFlashcards()
                } else {
                    _uiState.update { it.copy(errorMessage = "Add some note content first!") }
                }
            }
            NoteViewMode.MINDMAP -> {
                if (_uiState.value.mindmap != null) {
                    _uiState.update { it.copy(viewMode = NoteViewMode.MINDMAP) }
                } else if (content.isNotBlank()) {
                    generateMindmap()
                } else {
                    _uiState.update { it.copy(errorMessage = "Add some note content first!") }
                }
            }
            NoteViewMode.QUIZ -> {
                if (_uiState.value.quiz.isNotEmpty()) {
                    _uiState.update { it.copy(viewMode = NoteViewMode.QUIZ) }
                } else if (content.isNotBlank()) {
                    generateQuiz()
                } else {
                    _uiState.update { it.copy(errorMessage = "Add some note content first!") }
                }
            }
        }
    }

    fun generateSimplify() {
        val content = _uiState.value.noteContent.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Summarizing with Gemini AI...") }
            val simplified = GeminiService.simplifyNote(content)

            val currentId = _uiState.value.selectedNoteId
            val currentNote = _uiState.value.notes.find { it.id == currentId }
            if (currentNote != null) {
                val updated = currentNote.copy(
                    title = _uiState.value.noteTitle,
                    content = _uiState.value.noteContent,
                    simplifiedContent = simplified
                )
                noteRepository.saveNote(updated)
                val updatedList = _uiState.value.notes.map { if (it.id == currentId) updated else it }
                _uiState.update {
                    it.copy(
                        notes = updatedList,
                        simplifiedContent = simplified,
                        viewMode = NoteViewMode.SIMPLIFY,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(simplifiedContent = simplified, viewMode = NoteViewMode.SIMPLIFY, isLoading = false) }
            }
        }
    }

    fun generateFlashcards() {
        val content = _uiState.value.noteContent.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Generating Flashcards...") }
            val cards = GeminiService.generateFlashcards(content)
            val jsonStr = Json.encodeToString(cards)

            val currentId = _uiState.value.selectedNoteId
            val currentNote = _uiState.value.notes.find { it.id == currentId }
            if (currentNote != null) {
                val updated = currentNote.copy(
                    title = _uiState.value.noteTitle,
                    content = _uiState.value.noteContent,
                    flashcardsJson = jsonStr
                )
                noteRepository.saveNote(updated)
                val updatedList = _uiState.value.notes.map { if (it.id == currentId) updated else it }
                _uiState.update {
                    it.copy(
                        notes = updatedList,
                        flashcards = cards,
                        viewMode = NoteViewMode.FLASHCARDS,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(flashcards = cards, viewMode = NoteViewMode.FLASHCARDS, isLoading = false) }
            }
        }
    }

    fun generateMindmap() {
        val content = _uiState.value.noteContent.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Building Visual Mindmap...") }
            val map = GeminiService.generateMindmap(content)
            val jsonStr = Json.encodeToString(map)

            val currentId = _uiState.value.selectedNoteId
            val currentNote = _uiState.value.notes.find { it.id == currentId }
            if (currentNote != null) {
                val updated = currentNote.copy(
                    title = _uiState.value.noteTitle,
                    content = _uiState.value.noteContent,
                    mindmapJson = jsonStr
                )
                noteRepository.saveNote(updated)
                val updatedList = _uiState.value.notes.map { if (it.id == currentId) updated else it }
                _uiState.update {
                    it.copy(
                        notes = updatedList,
                        mindmap = map,
                        viewMode = NoteViewMode.MINDMAP,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(mindmap = map, viewMode = NoteViewMode.MINDMAP, isLoading = false) }
            }
        }
    }

    fun generateQuiz() {
        val content = _uiState.value.noteContent.trim()
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Creating Interactive Quiz...") }
            val quizQuestions = GeminiService.generateQuiz(content)
            val jsonStr = Json.encodeToString(quizQuestions)

            val currentId = _uiState.value.selectedNoteId
            val currentNote = _uiState.value.notes.find { it.id == currentId }
            if (currentNote != null) {
                val updated = currentNote.copy(
                    title = _uiState.value.noteTitle,
                    content = _uiState.value.noteContent,
                    quizJson = jsonStr
                )
                noteRepository.saveNote(updated)
                val updatedList = _uiState.value.notes.map { if (it.id == currentId) updated else it }
                _uiState.update {
                    it.copy(
                        notes = updatedList,
                        quiz = quizQuestions,
                        viewMode = NoteViewMode.QUIZ,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(quiz = quizQuestions, viewMode = NoteViewMode.QUIZ, isLoading = false) }
            }
        }
    }

    fun deleteCurrentTool() {
        val currentId = _uiState.value.selectedNoteId ?: return
        val currentNote = _uiState.value.notes.find { it.id == currentId } ?: return

        val updated = when (_uiState.value.viewMode) {
            NoteViewMode.SIMPLIFY -> currentNote.copy(simplifiedContent = "")
            NoteViewMode.FLASHCARDS -> currentNote.copy(flashcardsJson = "")
            NoteViewMode.MINDMAP -> currentNote.copy(mindmapJson = "")
            NoteViewMode.QUIZ -> currentNote.copy(quizJson = "")
            NoteViewMode.EDITOR -> currentNote
        }

        viewModelScope.launch {
            noteRepository.saveNote(updated)
            val updatedList = _uiState.value.notes.map { if (it.id == currentId) updated else it }
            _uiState.update {
                it.copy(
                    notes = updatedList,
                    simplifiedContent = if (it.viewMode == NoteViewMode.SIMPLIFY) "" else it.simplifiedContent,
                    flashcards = if (it.viewMode == NoteViewMode.FLASHCARDS) emptyList() else it.flashcards,
                    mindmap = if (it.viewMode == NoteViewMode.MINDMAP) null else it.mindmap,
                    quiz = if (it.viewMode == NoteViewMode.QUIZ) emptyList() else it.quiz,
                    viewMode = NoteViewMode.EDITOR,
                    notificationMessage = "Item deleted."
                )
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
            try {
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
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to schedule task: ${e.message}") }
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, errorMessage = null) }
    }
}