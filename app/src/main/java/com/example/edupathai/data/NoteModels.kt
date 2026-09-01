package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NoteFolder(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    @SerialName("color_hex") val colorHex: String = "#3B82F6",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Note(
    val id: String = "",
    @SerialName("folder_id") val folderId: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val content: String = "",
    @SerialName("simplified_content") val simplifiedContent: String = "",
    @SerialName("flashcards_json") val flashcardsJson: String = "",
    @SerialName("mindmap_json") val mindmapJson: String = "",
    @SerialName("quiz_json") val quizJson: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    fun getFlashcards(): List<Flashcard> {
        if (flashcardsJson.isBlank()) return emptyList()
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(flashcardsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getMindmap(): MindmapData? {
        if (mindmapJson.isBlank()) return null
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(mindmapJson)
        } catch (_: Exception) {
            null
        }
    }

    fun getQuiz(): List<QuizQuestion> {
        if (quizJson.isBlank()) return emptyList()
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(quizJson)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Serializable
data class Flashcard(
    val question: String = "",
    val answer: String = ""
)

@Serializable
data class MindmapBranch(
    val title: String = "",
    @SerialName("subItems") val subItems: List<String> = emptyList()
)

@Serializable
data class MindmapData(
    @SerialName("rootTitle") val rootTitle: String = "",
    val branches: List<MindmapBranch> = emptyList()
)

@Serializable
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    @SerialName("correctAnswer") val correctAnswer: String = "",
    val explanation: String = ""
)