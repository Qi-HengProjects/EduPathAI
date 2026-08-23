package com.example.edupathai.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = "https://fixaptybdongpdlpkgqx.supabase.co",
        supabaseKey = "sb_publishable_XsYujpkP0jyfomXakDheiQ_eOCkCOIN"
    ) {
        install(Auth)
        install(Postgrest)
    }

    val auth get() = client.auth
    val db get() = client.postgrest
}