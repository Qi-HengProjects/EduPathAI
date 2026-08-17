package com.example.edupathai.data

import android.util.Log
import com.example.edupathai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AiPromptType(val title: String) {
    SIMPLIFY_JARGON("Simplify Jargon"),
    GENERATE_QUIZ("Active Recall Quiz"),
    MINDMAP("Mindmap Outline")
}

class GeminiService {
    suspend fun processNoteContent(action: AiPromptType, content: String): String = withContext(Dispatchers.IO) {
        val rawApiKey = BuildConfig.GEMINI_API_KEY

        // Diagnostic Check 1: Key Validation
        if (rawApiKey.isBlank()) {
            return@withContext "❌ CONFIG ERROR: GEMINI_API_KEY is empty in BuildConfig.\n" +
                    "1. Ensure 'GEMINI_API_KEY=AIzaSy...' is in local.properties (no quotes).\n" +
                    "2. Run 'Build > Clean Project' and 'Build > Rebuild Project'."
        }

        if (rawApiKey.startsWith("\"") || rawApiKey.endsWith("\"")) {
            return@withContext "❌ FORMAT ERROR: GEMINI_API_KEY contains literal quotation marks: $rawApiKey\n" +
                    "Remove quotes inside local.properties and rebuild."
        }

        if (content.isBlank()) {
            return@withContext "⚠️ Note is empty. Write or paste some text first."
        }

        val prompt = when (action) {
            AiPromptType.SIMPLIFY_JARGON -> """
                Simplify the following study material for an alternative/neurodivergent learner:
                - Break down complex terms with simple real-world analogies.
                - Use clear, short bullet points.
                - Bold important key terms.
                
                Content:
                $content
            """.trimIndent()

            AiPromptType.GENERATE_QUIZ -> "Create 3 quick flashcard Q&As based on this text:\n$content"
            AiPromptType.MINDMAP -> "Create a Markdown hierarchy outline with emojis for:\n$content"
        }

        return@withContext try {
            val model = GenerativeModel(
                modelName = "gemini-3-flash-preview",
                apiKey = rawApiKey,
            )
            val response = model.generateContent(prompt)
            response.text ?: "⚠️ AI returned an empty response."
        } catch (e: Exception) {
            Log.e("GeminiService", "API call failed", e)
            "❌ GEMINI API ERROR:\n${e.javaClass.simpleName}: ${e.message}"
        }
    }

    /**
     * Multi-turn conversational reply for the AI Chatbot (Module 2).
     * [history] is the conversation so far (NOT including [newMessage]), oldest first.
     */
    suspend fun sendChatMessage(history: List<ChatMessage>, newMessage: String): String = withContext(Dispatchers.IO) {
        val rawApiKey = BuildConfig.GEMINI_API_KEY

        if (rawApiKey.isBlank()) {
            return@withContext "❌ CONFIG ERROR: GEMINI_API_KEY is empty in BuildConfig.\n" +
                    "1. Ensure 'GEMINI_API_KEY=AIzaSy...' is in local.properties (no quotes).\n" +
                    "2. Run 'Build > Clean Project' and 'Build > Rebuild Project'."
        }

        if (newMessage.isBlank()) {
            return@withContext "⚠️ Message is empty. Type or say something first."
        }

        return@withContext try {
            val model = GenerativeModel(
                modelName = "gemini-3-flash-preview",
                apiKey = rawApiKey,
            )

            val chatHistory = history.map { message ->
                content(role = if (message.role == "user") "user" else "model") {
                    text(message.content)
                }
            }

            val chat = model.startChat(history = chatHistory)
            val response = chat.sendMessage(newMessage)
            response.text ?: "⚠️ AI returned an empty response."
        } catch (e: Exception) {
            Log.e("GeminiService", "Chat API call failed", e)
            "❌ GEMINI API ERROR:\n${e.javaClass.simpleName}: ${e.message}"
        }
    }
}