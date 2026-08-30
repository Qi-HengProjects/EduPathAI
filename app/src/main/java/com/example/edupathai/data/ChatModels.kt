package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String = "New Conversation",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val sender: String = "user", // "user" or "model"
    val text: String = "",
    @SerialName("created_at") val createdAt: String? = null
)