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

    fun getUserId(): String {
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: SupabaseProvider.getLocalUserId()
    }

    suspend fun getSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        try {
            client.from("chat_sessions").select {
                filter { eq("user_id", currentUid) }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<ChatSession>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting sessions: ${e.message}", e)
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
            client.from("chat_sessions").insert(session)
            session
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating session in Supabase: ${e.message}", e)
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

    /**
     * Inserts a chat message into Supabase. This used to swallow every exception and
     * hand back the message as if it had been saved, so a message that only ever made
     * it into the in-memory UI cache looked identical - to both the UI and the logs -
     * to one that was actually persisted. The only symptom was messages vanishing the
     * next time the session was loaded from Supabase (e.g. after relaunching the app).
     * This now rethrows so the caller (ChatViewModel) knows the save failed and can
     * surface that to the user instead of silently pretending it worked.
     */
    suspend fun sendMessage(message: ChatMessage): ChatMessage = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        val msgId = if (message.id.isBlank()) UUID.randomUUID().toString() else message.id
        val timeNow = if (message.createdAt.isBlank()) Instant.now().toString() else message.createdAt
        val text = message.message.ifBlank { message.content }

        val msg = ChatMessage(
            id = msgId,
            sessionId = message.sessionId,
            userId = if (message.userId.isBlank()) currentUid else message.userId,
            sender = message.sender,
            message = text,
            content = text,
            createdAt = timeNow
        )

        try {
            client.from("chat_messages").insert(msg)
            msg
        } catch (e: Exception) {
            Log.e(
                "ChatRepository",
                "Error inserting chat message (sessionId=${msg.sessionId}, userId=${msg.userId}): ${e.message}",
                e
            )
            throw e
        }
    }

    suspend fun addMessage(message: ChatMessage): ChatMessage = sendMessage(message)
    suspend fun saveMessage(message: ChatMessage): ChatMessage = sendMessage(message)
}