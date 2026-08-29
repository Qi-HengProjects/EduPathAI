package com.example.edupathai.data

import android.util.Log
import com.example.edupathai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AiPromptType(val title: String) {
    SIMPLIFY_JARGON("Simplified Explanation"),
    GENERATE_QUIZ("Active Recall Flashcards"),
    MINDMAP("Visual Study Outline")
}

data class MindmapBranch(val title: String, val subItems: List<String>)
data class MindmapData(val rootTitle: String, val branches: List<MindmapBranch>)

class GeminiService {

    suspend fun processNoteContent(action: AiPromptType, content: String): String = withContext(Dispatchers.IO) {
        val rawApiKey = BuildConfig.GEMINI_API_KEY
        if (rawApiKey.isBlank()) return@withContext "CONFIG ERROR: GEMINI_API_KEY is missing in local.properties."
        if (content.isBlank()) return@withContext "Note is empty. Please enter some text first."

        val prompt = when (action) {
            AiPromptType.SIMPLIFY_JARGON -> """
                You are a learning tutor. Explain the key ideas of the material below in plain language with analogies.
                
                RULES:
                - Use '• ' for bullet points.
                - Leave an empty line between bullet points.
                - STRICT: Do NOT use markdown symbols. Never use asterisks (** or *), hashtags (#), or backticks.
                
                Material:
                $content
            """.trimIndent()

            AiPromptType.GENERATE_QUIZ -> """
                Create 3 active recall flashcards from the text below.
                
                Format each pair strictly as:
                Q: [Question]
                A: [Answer]
                
                RULES:
                - Leave an empty line between each flashcard.
                - STRICT: Do NOT use markdown symbols. Never use asterisks (** or *), hashtags (#), or backticks.
                
                Material:
                $content
            """.trimIndent()

            AiPromptType.MINDMAP -> """
                You are a visual diagram generator. Create a structured mindmap hierarchy from the text below.
                
                FORMAT RULES:
                Line 1 must be: ROOT: [Central Topic Name]
                Followed by 3 to 5 branch lines formatted exactly as:
                BRANCH: [Branch Title] | [Sub-concept 1], [Sub-concept 2], [Sub-concept 3]
                
                - STRICT: Do NOT use asterisks (** or *), hashtags (#), backticks, or other markdown.
                
                Material:
                $content
            """.trimIndent()
        }

        return@withContext try {
            val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = rawApiKey)
            val response = model.generateContent(prompt)
            sanitizeText(response.text ?: "No response generated.")
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini API request failed", e)
            "AI Processing Failed: ${e.message}"
        }
    }

    suspend fun sendChatMessage(history: List<ChatMessage>, userPrompt: String): String = withContext(Dispatchers.IO) {
        val rawApiKey = BuildConfig.GEMINI_API_KEY
        if (rawApiKey.isBlank()) return@withContext "❌ CONFIG ERROR: No API Key."

        try {
            val model = GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = rawApiKey)
            val chatHistory = history.map {
                com.google.ai.client.generativeai.type.content(role = it.role) { text(it.content) }
            }
            val chat = model.startChat(history = chatHistory)
            val response = chat.sendMessage(userPrompt)
            sanitizeText(response.text ?: "⚠️ AI returned empty text.")
        } catch (e: Exception) {
            Log.e("GeminiService", "Chat failed", e)
            "❌ CHAT ERROR: ${e.localizedMessage}"
        }
    }

    /**
     * Parses the sanitized AI mindmap text into structured node data for drawing.
     */
    fun parseMindmapData(rawText: String, defaultTitle: String): MindmapData {
        var root = defaultTitle.ifBlank { "Main Topic" }
        val branches = mutableListOf<MindmapBranch>()

        rawText.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("ROOT:", ignoreCase = true) -> {
                    root = trimmed.substringAfter(":").trim().ifBlank { root }
                }
                trimmed.startsWith("BRANCH:", ignoreCase = true) -> {
                    val content = trimmed.substringAfter(":")
                    val parts = content.split("|").map { it.trim() }
                    val branchTitle = parts.getOrNull(0) ?: "Topic"
                    val subItems = parts.getOrNull(1)
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                    branches.add(MindmapBranch(branchTitle, subItems))
                }
            }
        }

        if (branches.isEmpty()) {
            // Fallback parser if format was loosely structured
            val lines = rawText.lines().filter { it.isNotBlank() }
            root = lines.firstOrNull() ?: defaultTitle
            lines.drop(1).take(4).forEach { item ->
                branches.add(MindmapBranch(item.replace("•", "").trim(), emptyList()))
            }
        }

        return MindmapData(root, branches)
    }

    companion object {
        /**
         * Cleans out all Markdown hashtags, bold/italic asterisks, and backticks.
         */
        fun sanitizeText(raw: String): String {
            return raw
                .replace(Regex("```[a-zA-Z]*"), "")
                .replace("```", "")
                .replace(Regex("(?m)^#{1,6}\\s*"), "") // Strips #, ##, ###, ####
                .replace(Regex("\\*{1,}"), "")         // Strips *, **, ***
                .replace("`", "")                      // Strips inline backticks
                .replace(Regex("(?m)^[\\-\\+]\\s+"), "• ") // Standardize bullets
                .replace(Regex("\n{3,}"), "\n\n")      // Normalize line gaps
                .trim()
        }
    }
}