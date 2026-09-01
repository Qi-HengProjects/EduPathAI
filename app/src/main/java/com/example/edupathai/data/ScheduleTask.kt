package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class ScheduleTask(
    val id: String? = null,
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("energy_level") val energyLevel: String = "Medium",
    @SerialName("task_type") val taskType: String = "study",
    @SerialName("color_hex") val colorHex: String = "#3B82F6",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    val effectiveDate: String
        get() = createdAt?.take(10) ?: LocalDate.now().toString()

    /**
     * True if the task is not completed and its scheduled date/time has passed.
     */
    val isOverdue: Boolean
        get() {
            if (isCompleted) return false
            return try {
                val taskLocalDate = LocalDate.parse(effectiveDate)
                val today = LocalDate.now()
                if (taskLocalDate.isBefore(today)) {
                    true
                } else if (taskLocalDate.isEqual(today)) {
                    val timeStr = if (endTime.isNotBlank()) endTime else startTime
                    if (timeStr.isNotBlank()) {
                        val parts = timeStr.trim().split(":")
                        val hour = parts[0].toIntOrNull() ?: 0
                        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val taskTime = LocalTime.of(hour, min)
                        taskTime.isBefore(LocalTime.now())
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
}