package com.example.edupathai.data

import com.example.edupathai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object GeminiService {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-3.6-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

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

    suspend fun sendChatMessage(userPrompt: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext "API Key is missing. Please add GEMINI_API_KEY to your local.properties file."
        }
        try {
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
        } catch (e: Exception) {
            "AI Error: ${e.localizedMessage ?: "Unable to connect to AI services."}"
        }
    }

    suspend fun simplifyNote(content: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return@withContext "API Key missing."
        try {
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
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun generateFlashcards(content: String): List<Flashcard> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return@withContext emptyList()
        try {
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
        } catch (e: Exception) {
            listOf(Flashcard(question = "Core Concept", answer = content.take(120)))
        }
    }

    suspend fun generateMindmap(content: String): MindmapData = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext MindmapData(rootTitle = "Study Subject", branches = emptyList())
        }
        try {
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
        } catch (e: Exception) {
            MindmapData(
                rootTitle = "Study Notes",
                branches = listOf(MindmapBranch(title = "Overview", subItems = listOf(content.take(80))))
            )
        }
    }

    suspend fun generateQuiz(content: String): List<QuizQuestion> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return@withContext emptyList()
        try {
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
        } catch (e: Exception) {
            emptyList()
        }
    }
}