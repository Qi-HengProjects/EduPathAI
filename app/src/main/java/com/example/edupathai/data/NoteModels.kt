package com.example.edupathai.data
import kotlinx.serialization.Serializable

@Serializable
data class SubjectFolder(
    val id:String? = null,
    val subjectName: String,
    val noteCount: Int = 0)

@Serializable
data class NoteBookEntry(
    val id:String? = null,
    val folderId: String,
    val title: String,
    val contentMarkdown: String)