package com.example.edupathai.data

import com.example.edupathai.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    /**
     * Returns the active user ID if signed in, or a stable default ID for local/offline mode.
     */
    fun getLocalUserId(): String {
        return try {
            client.auth.currentUserOrNull()?.id ?: "00000000-0000-0000-0000-000000000001"
        } catch (_: Exception) {
            "00000000-0000-0000-0000-000000000001"
        }
    }
}