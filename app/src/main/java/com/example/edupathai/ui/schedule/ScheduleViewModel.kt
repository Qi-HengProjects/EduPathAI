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
import java.time.LocalDate

data class ScheduleUiState(
    val tasks: List<ScheduleTask> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val activeTabIndex: Int = 0, // 0 = Daily Timeline, 1 = Monthly Calendar
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(activeTabIndex = index) }
    }

    fun setSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fetchedTasks = repository.fetchTasks()
                _uiState.update { it.copy(tasks = fetchedTasks, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun toggleTaskCompletion(task: ScheduleTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            try {
                repository.updateTask(updated)
                _uiState.update { state ->
                    state.copy(tasks = state.tasks.map { if (it.id == task.id) updated else it })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteTask(task: ScheduleTask) {
        val taskId = task.id ?: return
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _uiState.update { state ->
                    state.copy(
                        tasks = state.tasks.filter { it.id != taskId },
                        snackbarMessage = "Task deleted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun addTask(task: ScheduleTask) {
        viewModelScope.launch {
            try {
                repository.addTask(task)
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}