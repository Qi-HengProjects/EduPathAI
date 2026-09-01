package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String? = null,
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("user_id") val userId: String? = null,
    val sender: String = "user",
    val message: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    val content: String
        get() = message
}