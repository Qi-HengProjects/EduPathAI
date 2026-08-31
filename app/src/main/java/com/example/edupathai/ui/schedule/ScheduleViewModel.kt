package com.example.edupathai.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val tasks: List<ScheduleTask> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notificationMessage: String? = null
)

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val fetchedTasks = repository.getTasks()
                _uiState.update {
                    it.copy(
                        tasks = fetchedTasks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load schedule: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleTaskCompletion(task: ScheduleTask) {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        viewModelScope.launch {
            try {
                // Optimistic UI update
                _uiState.update { current ->
                    current.copy(
                        tasks = current.tasks.map { if (it.id == task.id) updatedTask else it }
                    )
                }
                repository.updateTask(updatedTask)
            } catch (e: Exception) {
                loadTasks()
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { current ->
                    current.copy(tasks = current.tasks.filterNot { it.id == taskId })
                }
                repository.deleteTask(taskId)
            } catch (e: Exception) {
                loadTasks()
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, errorMessage = null) }
    }
}