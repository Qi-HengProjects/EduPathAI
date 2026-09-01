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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = repository.getTasks()
                _uiState.update { it.copy(tasks = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun createTask(
        title: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        taskType: String,
        colorHex: String,
        date: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            val task = ScheduleTask(
                title = title,
                startTime = startTime,
                endTime = endTime,
                energyLevel = energyLevel,
                taskType = taskType,
                colorHex = colorHex,
                createdAt = "${date}T${startTime}:00Z"
            )
            repository.createTask(task)
            loadTasks()
        }
    }

    fun toggleTaskCompleted(task: ScheduleTask) {
        val id = task.id ?: return
        viewModelScope.launch {
            repository.updateTaskCompletion(id, !task.isCompleted)
            loadTasks()
        }
    }

    fun updateTask(task: ScheduleTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            loadTasks()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            loadTasks()
        }
    }
}
