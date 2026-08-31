package com.example.edupathai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileModel(
    val id: String? = null,
    val username: String = "Student",
    val email: String = "",
    val bio: String? = null,
    @SerialName("learning_style") val learningStyle: String = "Visual",
    @SerialName("created_at") val createdAt: String? = null
)

typealias UserProfile = ProfileModel
typealias Profile = ProfileModel