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
    val errorMessage: String? = null,
    val userNotification: String? = null
)

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository = ScheduleRepository()
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
                val list = scheduleRepository.getTasks()
                _uiState.update { it.copy(tasks = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun createTask(task: ScheduleTask) {
        viewModelScope.launch {
            val created = scheduleRepository.createTask(task)
            if (created != null) {
                _uiState.update { it.copy(tasks = it.tasks + created, userNotification = "Task added to timeline!") }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to add task") }
            }
        }
    }

    fun createTask(
        title: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        taskType: String = "study",
        colorHex: String = "#3B82F6",
        date: LocalDate = _uiState.value.selectedDate
    ) {
        val task = ScheduleTask(
            title = title,
            startTime = startTime,
            endTime = endTime,
            energyLevel = energyLevel,
            taskType = taskType,
            colorHex = colorHex,
            taskDate = date.toString()
        )
        createTask(task)
    }

    fun toggleTaskCompletion(task: ScheduleTask) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        _uiState.update { state ->
            state.copy(tasks = state.tasks.map { if (it.id == task.id) updated else it })
        }
        viewModelScope.launch {
            val success = scheduleRepository.updateTask(updated)
            if (!success) {
                // Revert optimistic update on database failure
                loadTasks()
                _uiState.update { it.copy(errorMessage = "Failed to sync task status.") }
            }
        }
    }

    fun deleteTask(taskId: String) {
        val previousTasks = _uiState.value.tasks
        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }
        viewModelScope.launch {
            val success = scheduleRepository.deleteTask(taskId)
            if (!success) {
                // Revert optimistic deletion on failure
                _uiState.update { it.copy(tasks = previousTasks, errorMessage = "Failed to delete task.") }
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null, errorMessage = null) }
    }
}