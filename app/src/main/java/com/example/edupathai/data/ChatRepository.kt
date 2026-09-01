package com.example.edupathai.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ChatRepository {
    private val client = SupabaseProvider.client

    private fun getUserId(): String {
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: SupabaseProvider.getLocalUserId()
    }

    suspend fun getSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        val userId = getUserId()
        try {
            client.from("chat_sessions").select {
                filter { eq("user_id", userId) }
                order(column = "is_pinned", order = Order.DESCENDING)
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<ChatSession>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching sessions: ${e.message}")
            emptyList()
        }
    }

    suspend fun createSession(title: String = "New Conversation", isPinned: Boolean = false): ChatSession = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            isPinned = isPinned,
            createdAt = Instant.now().toString()
        )
        try {
            client.from("chat_sessions").insert(session)
            session
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating session: ${e.message}")
            session
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").update(
                {
                    set("title", newTitle)
                }
            ) {
                filter {
                    eq("id", sessionId)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error renaming session: ${e.message}")
        }
    }

    suspend fun togglePinSession(sessionId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").update(
                {
                    set("is_pinned", isPinned)
                }
            ) {
                filter {
                    eq("id", sessionId)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error pinning session: ${e.message}")
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_messages").delete {
                filter { eq("session_id", sessionId) }
            }
            client.from("chat_sessions").delete {
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting session: ${e.message}")
        }
    }

    suspend fun getMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        if (sessionId.isBlank() || sessionId == "NEW") return@withContext emptyList()
        try {
            client.from("chat_messages").select {
                filter { eq("session_id", sessionId) }
                order(column = "created_at", order = Order.ASCENDING)
            }.decodeList<ChatMessage>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching messages: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendMessage(message: ChatMessage): ChatMessage = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val msg = message.copy(
            id = if (message.id.isNullOrBlank()) UUID.randomUUID().toString() else message.id,
            userId = if (message.userId.isNullOrBlank()) userId else message.userId,
            createdAt = message.createdAt ?: Instant.now().toString()
        )
        try {
            client.from("chat_messages").insert(msg)
            msg
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error inserting message: ${e.message}")
            msg
        }
    }

    suspend fun addMessage(message: ChatMessage): ChatMessage = sendMessage(message)
    suspend fun saveMessage(message: ChatMessage): ChatMessage = sendMessage(message)
}