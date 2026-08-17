package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

class ChatRepository {
    private val client = SupabaseProvider.client

    // ---- Sessions (chat_sessions) ----

    /** Read: list sessions, pinned first then most recently active; optional title search. */
    suspend fun getSessions(searchQuery: String = ""): List<ChatSession> {
        return client.from("chat_sessions").select {
            filter {
                if (searchQuery.isNotBlank()) {
                    ilike("title", "%$searchQuery%")
                }
            }
            order(column = "is_pinned", order = Order.DESCENDING)
            order(column = "updated_at", order = Order.DESCENDING)
        }.decodeList<ChatSession>()
    }

    /** Create: start a new chat thread. */
    suspend fun createSession(title: String): ChatSession {
        return client.from("chat_sessions").insert(ChatSession(title = title)) {
            select()
        }.decodeSingle<ChatSession>()
    }

    /** Update: rename a session. */
    suspend fun renameSession(id: String, title: String) {
        client.from("chat_sessions").update(
            {
                set("title", title)
            }
        ) {
            filter {
                eq("id", id)
            }
        }
    }

    /** Update: star / pin a session so it stays at the top of history. */
    suspend fun toggleSessionPin(id: String, isPinned: Boolean) {
        client.from("chat_sessions").update(
            {
                set("is_pinned", isPinned)
            }
        ) {
            filter {
                eq("id", id)
            }
        }
    }

    /** Bumps updated_at so the session resurfaces at the top of a "most recent" sort. */
    suspend fun touchSession(id: String) {
        client.from("chat_sessions").update(
            {
                set("updated_at", Instant.now().toString())
            }
        ) {
            filter {
                eq("id", id)
            }
        }
    }

    /** Delete: remove an entire chat thread (session + its messages). */
    suspend fun deleteSession(id: String) {
        client.from("chat_messages").delete {
            filter {
                eq("session_id", id)
            }
        }
        client.from("chat_sessions").delete {
            filter {
                eq("id", id)
            }
        }
    }

    // ---- Messages (chat_messages) ----

    /** Read: full conversation for a session, oldest first. */
    suspend fun getMessages(sessionId: String): List<ChatMessage> {
        return client.from("chat_messages").select {
            filter {
                eq("session_id", sessionId)
            }
            order(column = "created_at", order = Order.ASCENDING)
        }.decodeList<ChatMessage>()
    }

    /** Create: persist a single turn (user prompt or model reply). */
    suspend fun createMessage(message: ChatMessage): ChatMessage {
        return client.from("chat_messages").insert(message) {
            select()
        }.decodeSingle<ChatMessage>()
    }
}