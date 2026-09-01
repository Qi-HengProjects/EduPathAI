package com.example.edupathai.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class NoteRepository {
    private val client = SupabaseProvider.client

    private fun getUserId(): String {
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id
            ?: SupabaseProvider.getLocalUserId()
    }

    suspend fun getFolders(): List<NoteFolder> = withContext(Dispatchers.IO) {
        val uid = getUserId()
        try {
            client.from("note_folders").select {
                filter { eq("user_id", uid) }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<NoteFolder>()
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error fetching folders: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createFolder(name: String, colorHex: String = "#3B82F6"): NoteFolder? = withContext(Dispatchers.IO) {
        val uid = getUserId()
        val folder = NoteFolder(
            id = UUID.randomUUID().toString(),
            userId = uid,
            name = name,
            colorHex = colorHex,
            createdAt = Instant.now().toString()
        )
        try {
            client.from("note_folders").insert(folder)
            folder
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error creating folder: ${e.message}", e)
            null
        }
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("notes").delete {
                filter { eq("folder_id", folderId) }
            }
            client.from("note_folders").delete {
                filter { eq("id", folderId) }
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error deleting folder: ${e.message}", e)
        }
    }

    suspend fun getNotesByFolder(folderId: String): List<Note> = withContext(Dispatchers.IO) {
        try {
            client.from("notes").select {
                filter { eq("folder_id", folderId) }
                order(column = "created_at", order = Order.ASCENDING)
            }.decodeList<Note>()
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error fetching notes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveNote(note: Note): Note = withContext(Dispatchers.IO) {
        val uid = getUserId()
        val validNote = note.copy(
            id = if (note.id.isNullOrBlank()) UUID.randomUUID().toString() else note.id,
            userId = if (note.userId.isBlank()) uid else note.userId,
            createdAt = if (note.createdAt.isNullOrBlank()) Instant.now().toString() else note.createdAt
        )
        try {
            client.from("notes").upsert(validNote)
            validNote
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error saving note: ${e.message}", e)
            validNote
        }
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        saveNote(note)
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("notes").delete {
                filter { eq("id", noteId) }
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error deleting note: ${e.message}", e)
        }
    }
}