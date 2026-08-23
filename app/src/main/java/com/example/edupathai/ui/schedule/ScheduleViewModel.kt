package com.example.edupathai.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edupathai.data.ScheduleRepository
import com.example.edupathai.data.ScheduleTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val isMonthlyView: Boolean = false, // Toggle between Monthly Calendar and Daily Timeline
    val allTasks: List<ScheduleTask> = emptyList(),
    val filteredDailyTasks: List<ScheduleTask> = emptyList(),

    val isLoading: Boolean = false,
    val isAiGenerating: Boolean = false,
    val tasks: List<ScheduleTask> = emptyList(),
    val errorMessage: String? = null,

    val isTimerRunning: Boolean = false,
    val timerSecondsRemaining: Int = 25 * 60, // Default 25 min
    val activeTimerTask: ScheduleTask? = null
)

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fetchedTasks = repository.getTasks()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tasks = fetchedTasks,
                        allTasks = fetchedTasks,
                        errorMessage = null
                    )
                }
                filterTasksByDate(_uiState.value.selectedDate)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        filterTasksByDate(date)
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
    }

    fun toggleViewMode(isMonthly: Boolean) {
        _uiState.update { it.copy(isMonthlyView = isMonthly) }
    }

    private fun filterTasksByDate(date: LocalDate) {
        val dateString = date.toString()
        val filtered = _uiState.value.allTasks.filter { task ->
            task.startTime.startsWith(dateString)
        }
        _uiState.update { it.copy(filteredDailyTasks = filtered) }
    }

    fun addTask(
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        energyLevel: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            try {
                val newTask = ScheduleTask(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    energyLevel = energyLevel,
                    colorHex = colorHex
                )
                repository.createTask(newTask)
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateTask(task: ScheduleTask) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleTaskCompletion(task: ScheduleTask) {
        val taskId = task.id ?: return
        val newStatus = !task.isCompleted

        val isCurrentActiveTimerTask = _uiState.value.activeTimerTask?.id == taskId
        val shouldStopTimer = newStatus && isCurrentActiveTimerTask

        if (shouldStopTimer) {
            timerJob?.cancel()
        }

        _uiState.update { state ->
            val updated = state.tasks.map {
                if (it.id == taskId) it.copy(isCompleted = newStatus) else it
            }
            state.copy(
                tasks = updated,
                allTasks = updated,
                isTimerRunning = if (shouldStopTimer) false else state.isTimerRunning,
                activeTimerTask = if (shouldStopTimer) null else state.activeTimerTask
            )
        }
        filterTasksByDate(_uiState.value.selectedDate)

        viewModelScope.launch {
            try {
                repository.toggleTaskCompletion(taskId, newStatus)
            } catch (e: Exception) {
                loadTasks()
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                if (_uiState.value.activeTimerTask?.id == taskId) {
                    pauseFocusTimer()
                    _uiState.update { it.copy(activeTimerTask = null) }
                }
                repository.deleteTask(taskId)
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun generateAISchedule(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true) }
            try {
                repository.generateAISchedule(prompt)
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isAiGenerating = false) }
            }
        }
    }

    fun startFocusTimer(task: ScheduleTask? = null) {
        timerJob?.cancel()

        if (_uiState.value.timerSecondsRemaining <= 0) {
            _uiState.update { it.copy(timerSecondsRemaining = 25 * 60) }
        }

        _uiState.update { it.copy(isTimerRunning = true, activeTimerTask = task) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSecondsRemaining > 0 && _uiState.value.isTimerRunning) {
                delay(1000L)
                _uiState.update { it.copy(timerSecondsRemaining = it.timerSecondsRemaining - 1) }
            }
            if (_uiState.value.timerSecondsRemaining <= 0) {
                _uiState.update { it.copy(isTimerRunning = false) }
            }
        }
    }

    fun pauseFocusTimer() {
        _uiState.update { it.copy(isTimerRunning = false) }
        timerJob?.cancel()
    }

    fun resetFocusTimer(minutes: Int = 25) {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isTimerRunning = false,
                timerSecondsRemaining = minutes * 60,
                activeTimerTask = null
            )
        }
    }
}