package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "New Conversation",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("session_id") val sessionId: String = "",
    val sender: String = "user",
    val message: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    val content: String get() = message
    val text: String get() = message
    val isUser: Boolean get() = sender == "user"
}