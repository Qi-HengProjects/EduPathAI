package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteFolder(
    val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    val name: String = "",
    @SerialName("color_hex") val colorHex: String = "#3B82F6",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class NoteBookEntry(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("folder_id") val folderId: String = "",
    val title: String = "Untitled Note",
    @SerialName("content") val contentMarkdown: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val content: String get() = contentMarkdown
}

@Serializable
data class Flashcard(
    val question: String = "",
    val answer: String = ""
)

@Serializable
data class MindmapData(
    val rootTitle: String = "",
    val branches: List<MindmapBranch> = emptyList()
)

@Serializable
data class MindmapBranch(
    val title: String = "",
    val subItems: List<String> = emptyList()
)

@Serializable
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val explanation: String = ""
)