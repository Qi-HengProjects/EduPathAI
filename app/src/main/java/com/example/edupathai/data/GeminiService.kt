package com.example.edupathai.data

import android.util.Log
import com.example.edupathai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.edupathai.data.ScheduleTask
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
                modelName = "gemini-2.5-flash",
                apiKey = rawApiKey,
            )
            val response = model.generateContent(prompt)
            response.text ?: "⚠️ AI returned an empty response."
        } catch (e: Exception) {
            Log.e("GeminiService", "API call failed", e)
            "❌ GEMINI API ERROR:\n${e.javaClass.simpleName}:${e.message}"
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
                modelName = "gemini-2.5-flash",
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
            "❌ GEMINI API ERROR:\n${e.javaClass.simpleName}:${e.message}"
        }
    }

    /**
     * AI Daily Schedule Generator. Parses input prompt into structured tasks.
     */
    suspend fun fetchStudySchedule(prompt: String): List<ScheduleTask> = withContext(Dispatchers.IO) {
        val rawApiKey = BuildConfig.GEMINI_API_KEY

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val currentTimeMs = System.currentTimeMillis()

        if (rawApiKey.isBlank() || prompt.isBlank()) {
            return@withContext fallbackTasks(prompt, sdf, currentTimeMs)
        }

        try {
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = rawApiKey
            )

            val systemPrompt = """
                Parse this study request: "$prompt".
                Return ONLY a JSON array of objects. Do not use Markdown formatting or ```json block.
                Each object must have these fields:
                - "title": (String) Task title
                - "description": (String) Short details
                - "energyLevel": (String) "high", "medium", or "low"
                - "colorHex": (String) Hex color string starting with '#' (e.g. "#FF7675", "#6C5CE7", "#00B894")
            """.trimIndent()

            val response = model.generateContent(systemPrompt)
            val responseText = response.text?.replace("```json", "")?.replace("```", "")?.trim()

            if (!responseText.isNullOrBlank() && responseText.startsWith("[")) {
                val jsonArray = JSONArray(responseText)
                val taskList = mutableListOf<ScheduleTask>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val startTimeIso = sdf.format(Date(currentTimeMs + (i * 3600000)))
                    val endTimeIso = sdf.format(Date(currentTimeMs + ((i + 1) * 3600000)))

                    taskList.add(
                        ScheduleTask(
                            title = item.optString("title", "Study Block ${i + 1}"),
                            description = item.optString("description", "AI Generated Session"),
                            startTime = startTimeIso,
                            endTime = endTimeIso,
                            energyLevel = item.optString("energyLevel", "medium"),
                            colorHex = item.optString("colorHex", "#6C5CE7")
                        )
                    )
                }
                return@withContext taskList
            } else {
                return@withContext fallbackTasks(prompt, sdf, currentTimeMs)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Schedule generation error", e)
            return@withContext fallbackTasks(prompt, sdf, currentTimeMs)
        }
    }

    private fun fallbackTasks(prompt: String, sdf: SimpleDateFormat, currentTimeMs: Long): List<ScheduleTask> {
        val nowIso = sdf.format(Date(currentTimeMs))
        val oneHourLaterIso = sdf.format(Date(currentTimeMs + 3600000))
        val twoHoursLaterIso = sdf.format(Date(currentTimeMs + 7200000))

        return listOf(
            ScheduleTask(
                title = "Study: $prompt (Part 1)",
                description = "Focus block generated for user study session.",
                startTime = nowIso,
                endTime = oneHourLaterIso,
                energyLevel = "high",
                colorHex = "#FF7675"
            ),
            ScheduleTask(
                title = "Review: $prompt (Part 2)",
                description = "Follow-up revision and practice.",
                startTime = oneHourLaterIso,
                endTime = twoHoursLaterIso,
                energyLevel = "medium",
                colorHex = "#6C5CE7"
            )
        )
    }
}