package com.example.edupathai.data

import android.util.Log
import com.example.edupathai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

object GeminiService {
    private const val TAG = "GeminiService"
    private val apiKeys: List<String> by lazy {
        val multiKeys = BuildConfig.GEMINI_API_KEYS
        if (multiKeys.isNotBlank()) {
            multiKeys.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            val singleKey = BuildConfig.GEMINI_API_KEY
            if (singleKey.isNotBlank()) listOf(singleKey) else emptyList()
        }
    }

    private val currentKeyIndex = AtomicInteger(0)
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Strips all markdown symbols (**, ##, ---, *, _, `) and normalizes output into clean plain text.
     */
    fun cleanPlainText(raw: String): String {
        var text = raw
        // Remove code block markers
        text = text.replace("```[a-zA-Z]*".toRegex(), "").replace("```", "")
        // Remove bold and italic markers (***, **, *, ___, __, _)
        text = text.replace(Regex("\\*\\*\\*(.*?)\\*\\*\\*"), "$1")
        text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        text = text.replace(Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)"), "$1")
        text = text.replace(Regex("___(.*?)___"), "$1")
        text = text.replace(Regex("__(.*?)__"), "$1")
        text = text.replace(Regex("(?<!_)_(?!_)(.*?)(?<!_)_(?!_)"), "$1")
        // Remove markdown headers (# Title -> Title)
        text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")
        // Remove horizontal lines (---, ***, ___)
        text = text.replace(Regex("(?m)^[-*_]{3,}\\s*$"), "")
        // Convert markdown asterisks/hyphen bullets to clean standard bullet dots
        text = text.replace(Regex("(?m)^[ \\t]*[*\\-+]\\s+"), "• ")
        // Remove blockquotes (> Quote -> Quote)
        text = text.replace(Regex("(?m)^>\\s*"), "")
        // Remove inline code ticks `code` -> code
        text = text.replace(Regex("`([^`]+)`"), "$1")
        // Remove markdown links [title](url) -> title
        text = text.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        // Remove excessive empty lines
        text = text.replace(Regex("\n{3,}"), "\n\n")
        return text.trim()
    }

    // Holds the last real failure so callers can surface something useful
    // instead of the generic "all keys failed" message.
    @Volatile
    private var lastError: Throwable? = null

    private suspend fun <T> executeWithFallback(
        operationName: String,
        action: suspend (GenerativeModel) -> T
    ): T? = withContext(Dispatchers.IO) {
        lastError = null

        if (apiKeys.isEmpty()) {
            Log.e(TAG, "[$operationName] No Gemini API keys found. Check that " +
                    "GEMINI_API_KEY or GEMINI_API_KEYS is set in local.properties, then " +
                    "Sync Gradle + Rebuild (BuildConfig is baked in at build time).")
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                // Log the REAL reason this key/request failed — this is what
                // was previously being swallowed and reported only as
                // "all keys failed or are missing".
                Log.e(
                    TAG,
                    "[$operationName] Key #$keyIndex failed: ${e.javaClass.simpleName} - ${e.message}",
                    e
                )
                attempts++
                currentKeyIndex.incrementAndGet()
            }
        }
        null
    }

    /**
     * Human-readable detail for the most recent failure, if any.
     * Useful for surfacing a real cause (invalid key, network error,
     * model not found, quota exceeded, etc.) in the UI instead of a
     * generic message.
     */
    fun lastErrorMessage(): String? {
        val e = lastError ?: return null
        return "${e.javaClass.simpleName}: ${e.message ?: "no details"}"
    }

    suspend fun generateResponse(prompt: String): String {
        val plainTextInstruction = "88Respond in clear, natural plain text. Do NOT use markdown syntax (no asterisks, no hashes, no bold tags, no horizontal lines).\n\n"
        val raw = executeWithFallback("generateResponse") { model ->
            val response = model.generateContent(plainTextInstruction + prompt)
            response.text ?: "No response generated."
        } ?: buildErrorMessage()

        return cleanPlainText(raw)
    }

    private fun buildErrorMessage(): String {
        val detail = lastErrorMessage()
        return if (apiKeys.isEmpty()) {
            "Error: No Gemini API key found. Add GEMINI_API_KEY to local.properties, " +
                    "then Sync Gradle + Rebuild the project (the key is baked into BuildConfig " +
                    "at build time, so just re-running the app is not enough)."
        } else if (detail != null) {
            "Error: All Gemini API keys failed. Last error: $detail"
        } else {
            "Error: All Gemini API keys failed or are missing."
        }
    }

    suspend fun generateSessionTitle(firstUserMessage: String): String {
        val prompt = "Generate a short 3-5 word title summarizing this academic study query: \"$firstUserMessage\". Return ONLY the plain text title without quotes, periods, or markdown."
        val result = generateResponse(prompt).trim().removeSurrounding("\"").removeSurrounding("'")
        return if (result.isBlank() || result.startsWith("Error:")) "Study Session" else cleanPlainText(result)
    }

    suspend fun sendChatMessage(history: List<ChatMessage>, userMessage: String): String {
        val systemPrompt = "You are EduPath AI, an intelligent student study assistant. Answer clearly and concisely in plain text with 0 markdown symbols (never use ** for bold, never use ### for headings, never use --- for lines, use simple • bullet dots if making lists)."

        val raw = executeWithFallback("sendChatMessage") { model ->
            val chatHistory = mutableListOf<com.google.ai.client.generativeai.type.Content>()
            chatHistory.add(content(role = "user") { text(systemPrompt) })
            chatHistory.add(content(role = "model") { text("Understood. I will provide clear, well-structured plain text explanations without any markdown formatting symbols.") })

            history.forEach { msg ->
                chatHistory.add(
                    content(role = if (msg.sender == "user") "user" else "model") {
                        text(msg.message)
                    }
                )
            }
            val chat = model.startChat(history = chatHistory)
            val response = chat.sendMessage(userMessage)
            response.text ?: "No response generated."
        } ?: buildErrorMessage()
        // NOTE: previously fell back to generateResponse(userMessage) here, which
        // re-ran executeWithFallback and looped through every key a SECOND time.
        // That doubled (and, combined with generateSessionTitle, could triple)
        // the requests burned per message — exhausting all keys' quota almost
        // immediately even when some keys had fresh quota available.

        return cleanPlainText(raw)
    }

    suspend fun sendChatMessage(userMessage: String): String = sendChatMessage(emptyList(), userMessage)

    suspend fun simplifyNote(content: String): String {
        val prompt = "Extract and explain the key study points and core takeaways from these notes in clean plain text without markdown symbols:\n\n$content"
        return generateResponse(prompt)
    }

    suspend fun generateFlashcards(content: String): List<Flashcard> {
        val prompt = """
            Based on the following notes, generate 4-6 high-yield study flashcards.
            Respond ONLY with a valid JSON array of objects with keys "question" and "answer". Do not use any markdown formatting or markdown backticks in the text values.
            Notes: $content
        """.trimIndent()

        val rawText = executeWithFallback("generateFlashcards") { model ->
            model.generateContent(prompt).text ?: ""
        } ?: ""

        return try {
            val cleaned = rawText.substringAfter("```json").substringBefore("```").trim()
            val list = jsonParser.decodeFromString<List<Flashcard>>(cleaned)
            list.map { it.copy(question = cleanPlainText(it.question), answer = cleanPlainText(it.answer)) }
        } catch (_: Exception) {
            try {
                val list = jsonParser.decodeFromString<List<Flashcard>>(rawText.trim())
                list.map { it.copy(question = cleanPlainText(it.question), answer = cleanPlainText(it.answer)) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun generateMindmap(content: String): MindmapData {
        val prompt = """
            Based on the following notes, generate a structured mindmap hierarchy.
            Respond ONLY with a valid JSON object with keys "rootTitle" (string) and "branches" (array of objects with "title" string and "subItems" array of strings).
            Notes: $content
        """.trimIndent()

        val rawText = executeWithFallback("generateMindmap") { model ->
            model.generateContent(prompt).text ?: ""
        } ?: ""

        return try {
            val cleaned = rawText.substringAfter("```json").substringBefore("```").trim()
            val data = jsonParser.decodeFromString<MindmapData>(cleaned)
            MindmapData(
                rootTitle = cleanPlainText(data.rootTitle),
                branches = data.branches.map { b ->
                    MindmapBranch(
                        title = cleanPlainText(b.title),
                        subItems = b.subItems.map { cleanPlainText(it) }
                    )
                }
            )
        } catch (_: Exception) {
            try {
                val data = jsonParser.decodeFromString<MindmapData>(rawText.trim())
                MindmapData(
                    rootTitle = cleanPlainText(data.rootTitle),
                    branches = data.branches.map { b ->
                        MindmapBranch(
                            title = cleanPlainText(b.title),
                            subItems = b.subItems.map { cleanPlainText(it) }
                        )
                    }
                )
            } catch (_: Exception) {
                MindmapData("Core Concept", listOf(MindmapBranch("Overview", listOf("Study notes processed"))))
            }
        }
    }

    suspend fun generateQuiz(content: String): List<QuizQuestion> {
        val prompt = """
            Based on the following notes, generate 3 multiple-choice quiz questions.
            Respond ONLY with a valid JSON array of objects with keys "question", "options" (array of 4 strings), "correctAnswer" (exact string matching one option), and "explanation".
            Notes: $content
        """.trimIndent()

        val rawText = executeWithFallback("generateQuiz") { model ->
            model.generateContent(prompt).text ?: ""
        } ?: ""

        return try {
            val cleaned = rawText.substringAfter("```json").substringBefore("```").trim()
            val list = jsonParser.decodeFromString<List<QuizQuestion>>(cleaned)
            list.map { q ->
                QuizQuestion(
                    question = cleanPlainText(q.question),
                    options = q.options.map { cleanPlainText(it) },
                    correctAnswer = cleanPlainText(q.correctAnswer),
                    explanation = cleanPlainText(q.explanation)
                )
            }
        } catch (_: Exception) {
            try {
                val list = jsonParser.decodeFromString<List<QuizQuestion>>(rawText.trim())
                list.map { q ->
                    QuizQuestion(
                        question = cleanPlainText(q.question),
                        options = q.options.map { cleanPlainText(it) },
                        correctAnswer = cleanPlainText(q.correctAnswer),
                        explanation = cleanPlainText(q.explanation)
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}