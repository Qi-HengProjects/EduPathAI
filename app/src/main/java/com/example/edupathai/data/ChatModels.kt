package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "New Conversation",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("user_id") val userId: String = "",
    val sender: String = "user",
    val message: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    val content: String
        get() = message
}