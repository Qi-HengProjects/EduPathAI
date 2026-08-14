package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectFolder(
    val id: String? = null,

    @SerialName("subject_name")
    val subjectName: String,

    @SerialName("note_count")
    val noteCount: Int = 0,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class NoteBookEntry(
    val id: String? = null,

    @SerialName("folder_id")
    val folderId: String,

    val title: String,

    @SerialName("content_markdown")
    val contentMarkdown: String = "",

    @SerialName("created_at")
    val createdAt: String? = null
)