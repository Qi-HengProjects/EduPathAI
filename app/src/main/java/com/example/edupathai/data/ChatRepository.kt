package com.example.edupathai.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<ChatSession>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching sessions: ${e.message}")
            emptyList()
        }
    }

    suspend fun createSession(title: String = "New Conversation", isPinned: Boolean = false): ChatSession = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val sessionId = UUID.randomUUID().toString()
        val timeNow = Instant.now().toString()
        val session = ChatSession(
            id = sessionId,
            userId = userId,
            title = title,
            isPinned = isPinned,
            createdAt = timeNow
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
        val msgId = if (message.id.isNullOrBlank()) UUID.randomUUID().toString() else message.id
        val timeNow = message.createdAt ?: Instant.now().toString()
        val msg = message.copy(
            id = msgId,
            userId = if (message.userId.isNullOrBlank()) userId else message.userId,
            createdAt = timeNow
        )
        try {
            client.from("chat_messages").insert(msg)
            msg
        } catch (e: Exception) {
            Log.e("ChatRepository", "Standard insert failed: ${e.message}")
            try {
                @Serializable
                data class FallbackMessagePayload(
                    val id: String,
                    @SerialName("session_id") val sessionId: String,
                    val sender: String,
                    val message: String,
                    @SerialName("created_at") val createdAt: String
                )
                client.from("chat_messages").insert(
                    FallbackMessagePayload(
                        id = msgId,
                        sessionId = message.sessionId,
                        sender = message.sender,
                        message = message.message,
                        createdAt = timeNow
                    )
                )
            } catch (e2: Exception) {
                Log.e("ChatRepository", "Fallback insert failed: ${e2.message}")
            }
            msg
        }
    }

    suspend fun addMessage(message: ChatMessage): ChatMessage = sendMessage(message)
    suspend fun saveMessage(message: ChatMessage): ChatMessage = sendMessage(message)
}