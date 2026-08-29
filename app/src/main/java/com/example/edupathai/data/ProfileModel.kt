package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val username: String? = null,
    val bio: String? = null,
    @SerialName("learning_style")
    val learningStyle: String? = "Visual",
    @SerialName("created_at")
    val createdAt: String? = null
)