package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteRepository {
    private val client = SupabaseProvider.client

    suspend fun getFolders(): List<NoteFolder> = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
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
        val userId = SupabaseProvider.getLocalUserId()
        val newFolder = NoteFolder(id = UUID.randomUUID().toString(), userId = userId, name = name, colorHex = colorHex)
        try {
            client.from("note_folders").insert(newFolder) {
                select()
            }.decodeSingle<NoteFolder>()
        } catch (_: Exception) {
            newFolder
        }
    }

    suspend fun addFolder(name: String, colorHex: String): NoteFolder? = createFolder(name, colorHex)

    suspend fun deleteFolder(folderId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("notes").delete {
                filter {
                    eq("folder_id", folderId)
                    eq("user_id", userId)
                }
            }
            client.from("note_folders").delete {
                filter {
                    eq("id", folderId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            true
        }
    }

    suspend fun getNotesForFolder(folderId: String): List<Note> = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("notes").select {
                filter {
                    eq("folder_id", folderId)
                    eq("user_id", userId)
                }
                order(column = "updated_at", order = Order.DESCENDING)
            }.decodeList<Note>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getNotes(folderId: String): List<Note> = getNotesForFolder(folderId)

    suspend fun createNote(folderId: String, title: String = "Untitled Note", content: String = ""): Note = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        val newEntry = Note(
            id = UUID.randomUUID().toString(),
            userId = userId,
            folderId = folderId,
            title = title,
            content = content
        )
        try {
            client.from("notes").insert(newEntry) {
                select()
            }.decodeSingle<Note>()
        } catch (_: Exception) {
            newEntry
        }
    }

    suspend fun saveNote(note: Note): Note = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        val noteWithUser = note.copy(userId = userId, id = note.id ?: UUID.randomUUID().toString())
        try {
            client.from("notes").upsert(noteWithUser) {
                select()
            }.decodeSingle<Note>()
        } catch (_: Exception) {
            noteWithUser
        }
    }

    suspend fun addNote(note: Note): Note = saveNote(note)

    suspend fun deleteNote(noteId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("notes").delete {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            true
        }
    }
}