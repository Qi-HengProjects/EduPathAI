package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from

class NoteRepository {

    // 1. READ: Get all subject folders from Supabase
    suspend fun getFolders(): List<SubjectFolder> {
        return SupabaseProvider.client
            .from("subject_folders")
            .select()
            .decodeList<SubjectFolder>()
    }

    // 2. CREATE: Add a new subject folder (e.g., Biology, Physics, Math)
    suspend fun createFolder(folder: SubjectFolder) {
        SupabaseProvider.client
            .from("subject_folders")
            .insert(folder)
    }

    // 3. READ: Fetch all notes for a specific subject folder
    suspend fun getNotesForFolder(folderId: String): List<NoteBookEntry> {
        return SupabaseProvider.client
            .from("notebook_entries")
            .select {
                filter {
                    eq("folder_id", folderId)
                }
            }
            .decodeList<NoteBookEntry>()
    }

    // 4. CREATE: Save a new notebook entry
    suspend fun createNote(note: NoteBookEntry) {
        SupabaseProvider.client
            .from("notebook_entries")
            .insert(note)
    }

    // 5. DELETE: Remove a subject folder
    suspend fun deleteFolder(folderId: String) {
        SupabaseProvider.client
            .from("subject_folders")
            .delete {
                filter {
                    eq("id", folderId)
                }
            }
    }
}