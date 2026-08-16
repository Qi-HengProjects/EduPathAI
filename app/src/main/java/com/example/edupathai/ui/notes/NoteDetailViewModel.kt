package com.example.edupathai.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.AiPromptType
import com.example.edupathai.data.GeminiService
import com.example.edupathai.data.NoteBookEntry
import com.example.edupathai.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteDetailUiState(
    val isLoading: Boolean = false,
    val isAiProcessing: Boolean = false,
    val notes: List<NoteBookEntry> = emptyList(),
    val currentNoteId: String? = null,
    val title: String = "",
    val content: String = "",
    val isPreviewMode: Boolean = false,
    val isInitialLoadDone: Boolean = false,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
)

class NoteDetailViewModel(
    private val folderId: String,
    private val repository: NoteRepository = NoteRepository(),
    private val geminiService: GeminiService = GeminiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fetchedNotes = repository.getNotesByFolder(folderId)
                val activeNote = fetchedNotes.firstOrNull()

                _uiState.update { currentState ->
                    // Prevent overwriting if the user has already started typing
                    val shouldKeepUserDraft = currentState.content.isNotBlank() && !currentState.isInitialLoadDone

                    currentState.copy(
                        isLoading = false,
                        isInitialLoadDone = true,
                        notes = fetchedNotes,
                        currentNoteId = if (shouldKeepUserDraft) currentState.currentNoteId else activeNote?.id,
                        title = if (shouldKeepUserDraft) currentState.title else (activeNote?.title ?: "Untitled Note"),
                        content = if (shouldKeepUserDraft) currentState.content else (activeNote?.contentMarkdown ?: ""),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isInitialLoadDone = true, errorMessage = e.message)
                }
            }
        }
    }

    fun updateDraft(title: String, content: String) {
        _uiState.update { it.copy(title = title, content = content) }
    }

    fun runAiAction(actionType: AiPromptType) {
        val currentContent = _uiState.value.content

        if (currentContent.trim().isBlank()) {
            _uiState.update {
                it.copy(
                    content = "⚠️ Please write some notes here first before clicking '${actionType.title}'.",
                    snackbarMessage = "Note is empty."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true) }

            // Guaranteed execution inside try/catch so coroutine never dies silently
            val resultText = try {
                geminiService.processNoteContent(actionType, currentContent)
            } catch (e: Throwable) {
                "❌ UNEXPECTED ERROR: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }

            _uiState.update { state ->
                state.copy(
                    isAiProcessing = false,
                    content = "${state.content}\n\n---\n### 🤖 ${actionType.title}\n$resultText",
                    snackbarMessage = "AI ${actionType.title} complete!"
                )
            }
        }
    }

    fun selectNote(note: NoteBookEntry) {
        _uiState.update {
            it.copy(
                currentNoteId = note.id,
                title = note.title,
                content = note.contentMarkdown,
                isPreviewMode = false
            )
        }
    }

    fun createNewNote() {
        _uiState.update {
            it.copy(
                currentNoteId = null,
                title = "New Note",
                content = "",
                isPreviewMode = false
            )
        }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun saveNote() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (state.currentNoteId != null) {
                    repository.updateNoteEntry(state.currentNoteId, state.title, state.content)
                } else {
                    val newEntry = repository.createNoteEntry(
                        NoteBookEntry(
                            folderId = folderId,
                            title = state.title,
                            contentMarkdown = state.content
                        )
                    )
                    _uiState.update { it.copy(currentNoteId = newEntry.id) }
                }
                val refreshedNotes = repository.getNotesByFolder(folderId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notes = refreshedNotes,
                        snackbarMessage = "Note saved!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun deleteNote() {
        val noteId = _uiState.value.currentNoteId ?: return
        viewModelScope.launch {
            try {
                repository.deleteNoteEntry(noteId)
                loadNotes()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}