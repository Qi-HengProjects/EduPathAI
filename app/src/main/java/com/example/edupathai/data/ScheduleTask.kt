package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleTask(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("energy_level") val energyLevel: String = "medium",
    @SerialName("task_type") val taskType: String = "study",
    @SerialName("color_hex") val colorHex: String = "#3B82F6",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)