package com.example.edupathai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository {
    private val client = SupabaseProvider.client

    suspend fun createSession(title: String = "New Conversation", userId: String? = null): ChatSession? = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: client.auth.currentUserOrNull()?.id ?: return@withContext null
        val session = ChatSession(
            userId = currentUserId,
            title = title
        )
        try {
            client.from("chat_sessions").insert(session) {
                select()
            }.decodeSingle<ChatSession>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSessions(userId: String? = null): List<ChatSession> = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("chat_sessions").select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<ChatSession>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveMessage(message: ChatMessage): ChatMessage? = withContext(Dispatchers.IO) {
        val currentUserId = message.userId ?: client.auth.currentUserOrNull()?.id ?: return@withContext null
        val msgWithUser = message.copy(userId = currentUserId)
        try {
            client.from("chat_messages").insert(msgWithUser) {
                select()
            }.decodeSingle<ChatMessage>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMessages(sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("chat_messages").select {
                filter {
                    eq("session_id", sessionId)
                    eq("user_id", currentUserId)
                }
            }.decodeList<ChatMessage>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        try {
            client.from("chat_sessions").update({
                set("title", newTitle)
            }) {
                filter {
                    eq("id", sessionId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun toggleSessionPin(sessionId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        try {
            client.from("chat_sessions").update({
                set("is_pinned", isPinned)
            }) {
                filter {
                    eq("id", sessionId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun pinSession(sessionId: String, isPinned: Boolean) = toggleSessionPin(sessionId, isPinned)

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        try {
            client.from("chat_sessions").delete {
                filter {
                    eq("id", sessionId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (_: Exception) {}
    }
}