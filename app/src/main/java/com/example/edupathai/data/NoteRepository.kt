package com.example.edupathai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteRepository {
    private val client = SupabaseProvider.client

    suspend fun getFolders(): List<NoteFolder> = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("note_folders").select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<NoteFolder>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addFolder(name: String, colorHex: String = "#3B82F6"): NoteFolder? = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        val newFolder = NoteFolder(
            userId = currentUserId,
            name = name,
            colorHex = colorHex
        )
        try {
            client.from("note_folders").insert(newFolder) {
                select()
            }.decodeSingle<NoteFolder>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        client.from("note_folders").delete {
            filter {
                eq("id", folderId)
                eq("user_id", currentUserId)
            }
        }
    }

    suspend fun getNotes(folderId: String): List<NoteBookEntry> = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("notes").select {
                filter {
                    eq("folder_id", folderId)
                    eq("user_id", currentUserId)
                }
            }.decodeList<NoteBookEntry>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveNote(note: NoteBookEntry) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        val noteWithUser = note.copy(userId = currentUserId)
        client.from("notes").upsert(noteWithUser)
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        client.from("notes").delete {
            filter {
                eq("id", noteId)
                eq("user_id", currentUserId)
            }
        }
    }
}