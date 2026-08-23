package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id") val id: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("bio") val bio: String = "Aspiring Learner",
    @SerialName("learning_style") val learningStyle: String = "Visual & Practical",
    @SerialName("focus_mode_enabled") val focusModeEnabled: Boolean = false,
    @SerialName("ai_voice_speed") val aiVoiceSpeed: Float = 1.0f,
    @SerialName("weekly_study_hours") val weeklyStudyHours: Int = 14,
    @SerialName("completion_rate") val completionRate: Int = 85
)