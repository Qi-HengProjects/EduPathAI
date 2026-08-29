package com.example.edupathai.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository {
    private val client = SupabaseProvider.client

    suspend fun createSession(title: String = "New Conversation"): ChatSession? = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions")
                .insert(ChatSession(title = title)) {
                    select()
                }
                .decodeSingle<ChatSession>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating chat session", e)
            null
        }
    }

    suspend fun fetchMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            client.from("chat_messages")
                .select {
                    filter { eq("session_id", sessionId) }
                }
                .decodeList<ChatMessage>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching messages", e)
            emptyList()
        }
    }

    suspend fun getSessions(query: String = ""): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions")
                .select {
                    if (query.isNotBlank()) {
                        filter { ilike("title", "%$query%") }
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<ChatSession>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting sessions", e)
            emptyList()
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").update({
                set("title", newTitle)
            }) {
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error renaming session", e)
        }
    }

    suspend fun toggleSessionPin(sessionId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").update({
                set("is_pinned", isPinned)
            }) {
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error toggling pin", e)
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").delete {
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting session", e)
        }
    }

    suspend fun touchSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").update({
                set("updated_at", java.time.OffsetDateTime.now().toString())
            }) {
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error touching session", e)
        }
    }

    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_messages").insert(message)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error saving message to Supabase", e)
        }
    }
}