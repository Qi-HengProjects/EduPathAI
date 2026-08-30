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

data class NoteAiCache(
    val simplifiedText: String? = null,
    val flashcards: List<Flashcard> = emptyList(),
    val mindmapData: MindmapData? = null,
    val quizQuestions: List<QuizQuestion> = emptyList()
)

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
    val currentQuizIndex: Int = 0,
    val selectedQuizAnswer: String? = null,
    val isAnswerSubmitted: Boolean = false,
    val quizScore: Int = 0,
    val isQuizFinished: Boolean = false,
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

    // In-memory cache for AI artifacts keyed by Note ID
    private val aiArtifactsCache = mutableMapOf<String, NoteAiCache>()

    init {
        loadNotes()
    }

    fun loadNotes(selectLatestId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val notes = repository.getNotes(folderId)
                val selected = when {
                    selectLatestId != null -> notes.find { it.id == selectLatestId } ?: notes.firstOrNull()
                    _uiState.value.currentNote != null -> notes.find { it.id == _uiState.value.currentNote?.id } ?: notes.firstOrNull()
                    else -> notes.firstOrNull()
                } ?: NoteBookEntry(
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
                restoreAiCacheForCurrentNote()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun getNoteCacheKey(): String {
        return _uiState.value.currentNote?.id ?: _uiState.value.currentNote?.title ?: "default"
    }

    private fun restoreAiCacheForCurrentNote() {
        val key = getNoteCacheKey()
        val cached = aiArtifactsCache[key] ?: NoteAiCache()
        _uiState.update {
            it.copy(
                simplifiedText = cached.simplifiedText,
                flashcards = cached.flashcards,
                mindmapData = cached.mindmapData,
                quizQuestions = cached.quizQuestions
            )
        }
    }

    fun selectNote(note: NoteBookEntry) {
        // Auto-save current note before switching
        val active = _uiState.value.currentNote
        if (active != null && active.id != null && (active.title.isNotBlank() || active.contentMarkdown.isNotBlank())) {
            viewModelScope.launch {
                try {
                    repository.saveNote(active)
                } catch (_: Exception) {}
            }
        }

        _uiState.update {
            it.copy(
                currentNote = note,
                aiMode = AiIslandMode.NONE,
                selectedQuizAnswer = null,
                isAnswerSubmitted = false,
                currentQuizIndex = 0,
                isQuizFinished = false
            )
        }
        restoreAiCacheForCurrentNote()
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                // 1. Auto-save active note first
                val active = _uiState.value.currentNote
                if (active != null && (active.title.isNotBlank() || active.contentMarkdown.isNotBlank())) {
                    repository.saveNote(active)
                }

                // 2. Persist new note in Supabase
                val newNote = NoteBookEntry(
                    folderId = folderId,
                    title = "New Note",
                    contentMarkdown = ""
                )
                repository.saveNote(newNote)

                // 3. Reload list and select the new note
                val updatedNotes = repository.getNotes(folderId)
                val newlyCreated = updatedNotes.maxByOrNull { it.createdAt ?: "" } ?: updatedNotes.lastOrNull()

                _uiState.update {
                    it.copy(
                        notes = updatedNotes,
                        currentNote = newlyCreated ?: newNote,
                        aiMode = AiIslandMode.NONE,
                        simplifiedText = null,
                        flashcards = emptyList(),
                        mindmapData = null,
                        quizQuestions = emptyList(),
                        userNotification = "New note created!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to create note: ${e.message}") }
            }
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
                val selected = updatedNotes.find { it.id == noteToSave.id } ?: updatedNotes.firstOrNull()
                _uiState.update {
                    it.copy(
                        notes = updatedNotes,
                        currentNote = selected ?: noteToSave,
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
                aiArtifactsCache.remove(noteId)
                loadNotes()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun simplifyNote(forceRegenerate: Boolean = false) {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        val key = getNoteCacheKey()
        val cached = aiArtifactsCache[key]
        if (!forceRegenerate && !cached?.simplifiedText.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    aiMode = AiIslandMode.SIMPLIFY,
                    simplifiedText = cached?.simplifiedText
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiMode = AiIslandMode.SIMPLIFY) }
            try {
                val result = GeminiService.simplifyNote(content)
                val currentCache = aiArtifactsCache[key] ?: NoteAiCache()
                aiArtifactsCache[key] = currentCache.copy(simplifiedText = result)

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

    fun generateFlashcards(forceRegenerate: Boolean = false) {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        val key = getNoteCacheKey()
        val cached = aiArtifactsCache[key]
        if (!forceRegenerate && !cached?.flashcards.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    aiMode = AiIslandMode.FLASHCARDS,
                    flashcards = cached?.flashcards ?: emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiMode = AiIslandMode.FLASHCARDS) }
            try {
                val cards = GeminiService.generateFlashcards(content)
                val currentCache = aiArtifactsCache[key] ?: NoteAiCache()
                aiArtifactsCache[key] = currentCache.copy(flashcards = cards)

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

    fun generateMindmap(forceRegenerate: Boolean = false) {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        val key = getNoteCacheKey()
        val cached = aiArtifactsCache[key]
        if (!forceRegenerate && cached?.mindmapData != null) {
            _uiState.update {
                it.copy(
                    aiMode = AiIslandMode.MINDMAP,
                    mindmapData = cached.mindmapData
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true, aiMode = AiIslandMode.MINDMAP) }
            try {
                val map = GeminiService.generateMindmap(content)
                val currentCache = aiArtifactsCache[key] ?: NoteAiCache()
                aiArtifactsCache[key] = currentCache.copy(mindmapData = map)

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

    fun generateQuiz(forceRegenerate: Boolean = false) {
        val content = _uiState.value.currentNote?.contentMarkdown.orEmpty()
        if (content.isBlank()) {
            _uiState.update { it.copy(userNotification = "Note content is empty") }
            return
        }

        val key = getNoteCacheKey()
        val cached = aiArtifactsCache[key]
        if (!forceRegenerate && !cached?.quizQuestions.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    aiMode = AiIslandMode.QUIZ,
                    quizQuestions = cached?.quizQuestions ?: emptyList(),
                    currentQuizIndex = 0,
                    selectedQuizAnswer = null,
                    isAnswerSubmitted = false,
                    quizScore = 0,
                    isQuizFinished = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiMode = AiIslandMode.QUIZ,
                    currentQuizIndex = 0,
                    selectedQuizAnswer = null,
                    isAnswerSubmitted = false,
                    quizScore = 0,
                    isQuizFinished = false
                )
            }
            try {
                val quiz = GeminiService.generateQuiz(content)
                val currentCache = aiArtifactsCache[key] ?: NoteAiCache()
                aiArtifactsCache[key] = currentCache.copy(quizQuestions = quiz)

                _uiState.update {
                    it.copy(
                        quizQuestions = quiz,
                        isAiProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        errorMessage = "Quiz generation error: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectQuizOption(option: String) {
        val currentState = _uiState.value
        if (currentState.isAnswerSubmitted) return

        val currentQ = currentState.quizQuestions.getOrNull(currentState.currentQuizIndex) ?: return
        val isCorrect = option.trim().equals(currentQ.correctAnswer.trim(), ignoreCase = true)

        _uiState.update {
            it.copy(
                selectedQuizAnswer = option,
                isAnswerSubmitted = true,
                quizScore = if (isCorrect) it.quizScore + 1 else it.quizScore
            )
        }
    }

    fun nextQuizQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentQuizIndex + 1

        if (nextIndex < currentState.quizQuestions.size) {
            _uiState.update {
                it.copy(
                    currentQuizIndex = nextIndex,
                    selectedQuizAnswer = null,
                    isAnswerSubmitted = false
                )
            }
        } else {
            _uiState.update {
                it.copy(isQuizFinished = true)
            }
        }
    }

    fun resetQuiz() {
        _uiState.update {
            it.copy(
                currentQuizIndex = 0,
                selectedQuizAnswer = null,
                isAnswerSubmitted = false,
                quizScore = 0,
                isQuizFinished = false
            )
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
                isQuizFinished = false
            )
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null, errorMessage = null) }
    }
}