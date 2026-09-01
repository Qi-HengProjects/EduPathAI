package com.example.edupathai.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ChatRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ChatRepository {
    private val client = SupabaseProvider.client

    fun getUserId(): String {
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id
            ?: SupabaseProvider.getLocalUserId()
    }

    suspend fun getSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        try {
            client.from("chat_sessions").select {
                filter { eq("user_id", currentUid) }
                order(column = "is_pinned", order = Order.DESCENDING)
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<ChatSession>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching sessions: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createSession(title: String = "New Conversation", isPinned: Boolean = false): ChatSession = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        val sessionId = UUID.randomUUID().toString()
        val timeNow = Instant.now().toString()
        val session = ChatSession(
            id = sessionId,
            userId = currentUid,
            title = title,
            isPinned = isPinned,
            createdAt = timeNow
        )
        try {
            client.from("chat_sessions").upsert(session)
            session
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating session: ${e.message}", e)
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
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error renaming session: ${e.message}", e)
        }
    }

    suspend fun togglePinSession(sessionId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").update(
                {
                    set("is_pinned", isPinned)
                }
            ) {
                filter { eq("id", sessionId) }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error pinning session: ${e.message}", e)
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
            Log.e("ChatRepository", "Error deleting session: ${e.message}", e)
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
            Log.e("ChatRepository", "Error fetching messages: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun sendMessage(message: ChatMessage): ChatMessage = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        val msgId = if (message.id.isBlank()) UUID.randomUUID().toString() else message.id
        val timeNow = if (message.createdAt.isBlank()) Instant.now().toString() else message.createdAt
        val text = message.message.ifBlank { message.content }

        // Ensure parent session exists before message insertion
        if (message.sessionId.isNotBlank() && message.sessionId != "NEW") {
            try {
                val session = ChatSession(
                    id = message.sessionId,
                    userId = currentUid,
                    title = if (text.length > 25) text.take(22) + "..." else text,
                    isPinned = false,
                    createdAt = timeNow
                )
                client.from("chat_sessions").upsert(session)
            } catch (e: Exception) {
                Log.w("ChatRepository", "Parent session upsert notice: ${e.message}")
            }
        }

        val msg = ChatMessage(
            id = msgId,
            sessionId = message.sessionId,
            userId = if (message.userId.isBlank()) currentUid else message.userId,
            sender = message.sender,
            message = text,
            createdAt = timeNow
        )

        try {
            client.from("chat_messages").insert(msg)
            msg
        } catch (e: Exception) {
            Log.e("ChatRepository", "Database insert failed: ${e.message}", e)
            throw ChatRepositoryException(e.localizedMessage ?: e.message ?: "Database insert error", e)
        }
    }

    suspend fun addMessage(message: ChatMessage): ChatMessage = sendMessage(message)
    suspend fun saveMessage(message: ChatMessage): ChatMessage = sendMessage(message)
}