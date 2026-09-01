package com.example.edupathai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleRepository {
    private val client = SupabaseProvider.client

    suspend fun getTasks(): List<ScheduleTask> = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("schedule_tasks").select {
                filter { eq("user_id", currentUserId) }
                order(column = "start_time", order = Order.ASCENDING)
            }.decodeList<ScheduleTask>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createTask(task: ScheduleTask): ScheduleTask? = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User session expired. Please log in again.")
        val taskWithUser = task.copy(userId = currentUserId)
        try {
            client.from("schedule_tasks").insert(taskWithUser) {
                select()
            }.decodeSingle<ScheduleTask>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addTask(task: ScheduleTask): ScheduleTask? = createTask(task)

    suspend fun updateTask(task: ScheduleTask) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        val taskWithUser = task.copy(userId = currentUserId)
        try {
            client.from("schedule_tasks").upsert(taskWithUser)
        } catch (_: Exception) {}
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return@withContext
        try {
            client.from("schedule_tasks").delete {
                filter {
                    eq("id", taskId)
                    eq("user_id", currentUserId)
                }
            }
        } catch (_: Exception) {}
    }
}