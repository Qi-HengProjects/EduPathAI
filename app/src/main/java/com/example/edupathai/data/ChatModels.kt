package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String = "",
    val title: String = "New Chat",
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    @SerialName("is_starred")
    val isStarred: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("session_id")
    val sessionId: String = "",
    val sender: String = "user", // "user" or "model"
    val content: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)
