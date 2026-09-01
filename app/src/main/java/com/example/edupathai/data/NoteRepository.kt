package com.example.edupathai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteRepository {
    private val client = SupabaseProvider.client

    suspend fun getFolders(): List<NoteFolder> = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("note_folders").select {
                filter { eq("user_id", userId) }
                order(column = "created_at", order = Order.DESCENDING)
            }.decodeList<NoteFolder>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createFolder(name: String, colorHex: String): NoteFolder? = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            val newFolder = NoteFolder(userId = userId, name = name, colorHex = colorHex)
            client.from("note_folders").insert(newFolder) {
                select()
            }.decodeSingle<NoteFolder>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun addFolder(name: String, colorHex: String): NoteFolder? = createFolder(name, colorHex)

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("note_folders").delete {
                filter { eq("id", folderId) }
            }
        } catch (_: Exception) {}
    }

    suspend fun getNotesForFolder(folderId: String): List<NoteBookEntry> = withContext(Dispatchers.IO) {
        try {
            client.from("notes").select {
                filter { eq("folder_id", folderId) }
                order(column = "updated_at", order = Order.DESCENDING)
            }.decodeList<NoteBookEntry>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getNotes(folderId: String): List<NoteBookEntry> = getNotesForFolder(folderId)

    suspend fun saveNote(note: NoteBookEntry): NoteBookEntry? = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            val noteWithUser = note.copy(userId = userId)
            client.from("notes").upsert(noteWithUser) {
                select()
            }.decodeSingle<NoteBookEntry>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun addNote(note: NoteBookEntry): NoteBookEntry? = saveNote(note)

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("notes").delete {
                filter { eq("id", noteId) }
            }
        } catch (_: Exception) {}
    }
}