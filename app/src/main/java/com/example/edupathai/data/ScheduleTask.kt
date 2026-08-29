package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleTask(
    val id: String? = null,

    @SerialName("user_id")
    val userId: String? = null, 

    @SerialName("folder_id")
    val folderId: String? = null,

    val title: String,

    val description: String = "",

    @SerialName("start_time")
    val startTime: String,

    @SerialName("end_time")
    val endTime: String,

    @SerialName("is_completed")
    val isCompleted: Boolean = false,

    @SerialName("task_type")
    val taskType: String = "study",

    @SerialName("energy_level")
    val energyLevel: String = "medium",

    @SerialName("color_hex")
    val colorHex: String = "#4E75FF",

    @SerialName("created_at")
    val createdAt: String? = null
)