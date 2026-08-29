package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from

class NoteRepository {
    private val client = SupabaseProvider.client

    // Folders
    suspend fun getFolders(): List<SubjectFolder> {
        return client.from("subject_folders").select().decodeList<SubjectFolder>()
    }

    suspend fun createFolder(folder: SubjectFolder) {
        client.from("subject_folders").insert(folder)
    }

    // Notes
    suspend fun getNotesByFolder(folderId: String): List<NoteBookEntry> {
        return client.from("notebook_entries").select {
            filter {
                eq("folder_id", folderId)
            }
        }.decodeList<NoteBookEntry>()
    }

    suspend fun createNoteEntry(entry: NoteBookEntry): NoteBookEntry {
        return client.from("notebook_entries").insert(entry) {
            select()
        }.decodeSingle<NoteBookEntry>()
    }

    suspend fun saveNote(entry: NoteBookEntry): NoteBookEntry {
        return if (entry.id == null) {
            createNoteEntry(entry)
        } else {
            updateNoteEntry(entry.id, entry.title, entry.contentMarkdown)
            entry
        }
    }

    suspend fun updateNoteEntry(id: String, title: String, contentMarkdown: String) {
        client.from("notebook_entries").update(
            {
                set("title", title)
                set("content_markdown", contentMarkdown)
            }
        ) {
            filter {
                eq("id", id)
            }
        }
    }

    suspend fun deleteNoteEntry(id: String) {
        client.from("notebook_entries").delete {
            filter {
                eq("id", id)
            }
        }
    }
}