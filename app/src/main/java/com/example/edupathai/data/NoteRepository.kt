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

    suspend fun deleteFolder(folderId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
        try {
            // Clean up notes inside the folder first to prevent orphaned records in database
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
            false
        }
    }

    suspend fun getNotesForFolder(folderId: String): List<Note> = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
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

    /**
     * Creates a brand-new note record and returns the persisted row with its server-generated ID.
     */
    suspend fun createNote(folderId: String, title: String = "Untitled Note", content: String = ""): Note? = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            val newEntry = Note(
                id = null,
                userId = userId,
                folderId = folderId,
                title = title,
                content = content
            )
            client.from("notes").insert(newEntry) {
                select()
            }.decodeSingle<Note>()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Updates an existing note or creates one if no ID is present.
     */
    suspend fun saveNote(note: Note): Note? = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            val noteWithUser = note.copy(userId = userId)
            if (note.id.isNullOrBlank()) {
                createNote(note.folderId, note.title, note.content)
            } else {
                client.from("notes").upsert(noteWithUser) {
                    select()
                }.decodeSingle<Note>()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun addNote(note: Note): Note? = saveNote(note)

    suspend fun deleteNote(noteId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
        try {
            client.from("notes").delete {
                filter {
                    eq("id", noteId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}