package com.example.edupathai.data

import android.util.Log
import com.example.edupathai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

object GeminiService {
    private const val TAG = "GeminiService"

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Parse comma-separated keys from BuildConfig
    private val apiKeys: List<String> by lazy {
        BuildConfig.GEMINI_API_KEYS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private val currentKeyIndex = AtomicInteger(0)

    fun sanitizeText(text: String): String {
        return text
            .replace("**", "")
            .replace("*", "")
            .replace("### ", "")
            .replace("## ", "")
            .replace("# ", "")
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    /**
     * Executes AI calls with automatic multi-key fallback rotation.
     * If the active key runs out of quota, it rotates to the next available key.
     */
    private suspend fun <T> executeWithFallback(
        operationName: String,
        action: suspend (GenerativeModel) -> T
    ): T? = withContext(Dispatchers.IO) {
        if (apiKeys.isEmpty()) {
            Log.e(TAG, "No Gemini API keys found in local.properties.")
            return@withContext null
        }

        val totalKeys = apiKeys.size
        var attempts = 0

        while (attempts < totalKeys) {
            val keyIndex = currentKeyIndex.get() % totalKeys
            val activeKey = apiKeys[keyIndex]

            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.6-flash",
                    apiKey = activeKey
                )
                return@withContext action(model)
            } catch (e: Exception) {
                attempts++
                Log.w(
                    TAG,
                    "API Key #$keyIndex failed during $operationName: ${e.message}. Rotating to next key..."
                )
                currentKeyIndex.incrementAndGet()
            }
        }

        Log.e(TAG, "All $totalKeys Gemini API keys exhausted for $operationName.")
        null
    }

    suspend fun sendChatMessage(userPrompt: String): String {
        val result = executeWithFallback("sendChatMessage") { model ->
            val response = model.generateContent(
                """
                You are EduPath AI, an encouraging and clear academic study assistant.
                Answer the student's question directly, clearly, and concisely.
                
                RULES:
                - Do NOT use markdown symbols like asterisks (** or *) or hashtags (#).
                - Use clean bullet points (• ) when listing items.
                - Keep explanations structured and easy to read.
                
                Student Question:
                $userPrompt
                """.trimIndent()
            )
            sanitizeText(response.text ?: "Sorry, I couldn't formulate a response. Please try again.")
        }
        return result ?: "AI service is currently unavailable. All API keys reached quota limits."
    }

    suspend fun generateSessionTitle(prompt: String): String {
        val result = executeWithFallback("generateSessionTitle") { model ->
            val response = model.generateContent(
                """
                Generate a short 2 to 4 word title summarising this study topic:
                "$prompt"
                
                RULES:
                - Return ONLY the title text.
                - No quotes, no markdown, no punctuation.
                """.trimIndent()
            )
            val generated = sanitizeText(response.text ?: "")
            if (generated.isNotBlank()) generated.take(30) else prompt.take(26).trim()
        }
        return result ?: prompt.take(26).trim()
    }

    suspend fun simplifyNote(content: String): String {
        val result = executeWithFallback("simplifyNote") { model ->
            val response = model.generateContent(
                """
                Simplify and explain the following study notes into high-impact key takeaways.
                RULES:
                - Do NOT use markdown asterisks (**) or hashtags (#).
                - Use clear bullet points (• ) for key points.
                
                Content:
                $content
                """.trimIndent()
            )
            sanitizeText(response.text ?: "Could not simplify content.")
        }
        return result ?: "Unable to simplify note. Please try again later."
    }

    suspend fun generateFlashcards(content: String): List<Flashcard> {
        val result = executeWithFallback("generateFlashcards") { model ->
            val prompt = """
                Generate 4 study flashcards from the text below.
                Return ONLY a valid JSON array of objects with keys "question" and "answer".
                Do not include markdown tags.
                Text:
                $content
            """.trimIndent()

            val response = model.generateContent(prompt)
            val raw = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "[]"
            jsonParser.decodeFromString<List<Flashcard>>(raw)
        }
        return result ?: listOf(Flashcard(question = "Core Concept", answer = content.take(120)))
    }

    suspend fun generateMindmap(content: String): MindmapData {
        val result = executeWithFallback("generateMindmap") { model ->
            val prompt = """
                Convert the following notes into a structured mindmap hierarchy.
                Return ONLY a valid JSON object matching this exact schema:
                {
                  "rootTitle": "Main Subject Title",
                  "branches": [
                    {
                      "title": "Branch Name",
                      "subItems": ["Detail 1", "Detail 2"]
                    }
                  ]
                }
                Text:
                $content
            """.trimIndent()

            val response = model.generateContent(prompt)
            val raw = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
            jsonParser.decodeFromString<MindmapData>(raw)
        }
        return result ?: MindmapData(
            rootTitle = "Study Notes",
            branches = listOf(MindmapBranch(title = "Overview", subItems = listOf(content.take(80))))
        )
    }

    suspend fun generateQuiz(content: String): List<QuizQuestion> {
        val result = executeWithFallback("generateQuiz") { model ->
            val prompt = """
                Create 4 multiple-choice quiz questions based on the text below.
                Return ONLY a valid JSON array of objects with this exact structure:
                [
                  {
                    "question": "Question text here?",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswer": "Option A",
                    "explanation": "Why this option is correct."
                  }
                ]
                Text:
                $content
            """.trimIndent()

            val response = model.generateContent(prompt)
            val raw = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "[]"
            jsonParser.decodeFromString<List<QuizQuestion>>(raw)
        }
        return result ?: emptyList()
    }
}