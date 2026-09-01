package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository {
    private val client = SupabaseProvider.client

    suspend fun getSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("chat_sessions").select {
                filter { eq("user_id", userId) }
                order(column = "is_pinned", order = Order.DESCENDING)
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<ChatSession>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createSession(title: String = "New Conversation", isPinned: Boolean = false): ChatSession = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        val session = ChatSession(id = UUID.randomUUID().toString(), userId = userId, title = title, isPinned = isPinned)
        try {
            client.from("chat_sessions").insert(session) {
                select()
            }.decodeSingle<ChatSession>()
        } catch (_: Exception) {
            session
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("chat_sessions").update(
                {
                    set("title", newTitle)
                }
            ) {
                filter {
                    eq("id", sessionId)
                    eq("user_id", userId)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun togglePinSession(sessionId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("chat_sessions").update(
                {
                    set("is_pinned", isPinned)
                }
            ) {
                filter {
                    eq("id", sessionId)
                    eq("user_id", userId)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("chat_sessions").delete {
                filter { eq("id", sessionId) }
            }
        } catch (_: Exception) {}
    }

    suspend fun getMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            client.from("chat_messages").select {
                filter { eq("session_id", sessionId) }
                order(column = "created_at", order = Order.ASCENDING)
            }.decodeList<ChatMessage>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun sendMessage(message: ChatMessage): ChatMessage = withContext(Dispatchers.IO) {
        val msg = message.copy(id = message.id ?: UUID.randomUUID().toString())
        try {
            client.from("chat_messages").insert(msg) {
                select()
            }.decodeSingle<ChatMessage>()
        } catch (_: Exception) {
            msg
        }
    }

    suspend fun addMessage(message: ChatMessage): ChatMessage = sendMessage(message)
    suspend fun saveMessage(message: ChatMessage): ChatMessage = sendMessage(message)
}