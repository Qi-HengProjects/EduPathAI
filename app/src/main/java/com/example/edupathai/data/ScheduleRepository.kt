package com.example.edupathai.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ScheduleRepository {
    private val client = SupabaseProvider.client

    private fun getUserId(): String {
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id
            ?: SupabaseProvider.getLocalUserId()
    }

    suspend fun getTasks(): List<ScheduleTask> = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        try {
            client.from("schedule_tasks").select {
                filter { eq("user_id", currentUid) }
                order(column = "created_at", order = Order.ASCENDING)
            }.decodeList<ScheduleTask>()
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error fetching tasks: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createTask(task: ScheduleTask): ScheduleTask = withContext(Dispatchers.IO) {
        val currentUid = getUserId()
        val taskWithUser = task.copy(
            id = if (task.id.isNullOrBlank()) UUID.randomUUID().toString() else task.id,
            userId = if (task.userId.isBlank()) currentUid else task.userId
        )
        try {
            client.from("schedule_tasks").insert(taskWithUser)
            taskWithUser
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error creating task: ${e.message}", e)
            taskWithUser
        }
    }

    suspend fun updateTask(task: ScheduleTask) = withContext(Dispatchers.IO) {
        val taskId = task.id ?: return@withContext
        try {
            client.from("schedule_tasks").update(task) {
                filter { eq("id", taskId) }
            }
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error updating task: ${e.message}", e)
        }
    }

    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        try {
            client.from("schedule_tasks").update(
                {
                    set("is_completed", isCompleted)
                }
            ) {
                filter { eq("id", taskId) }
            }
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error updating completion: ${e.message}", e)
        }
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        try {
            client.from("schedule_tasks").delete {
                filter { eq("id", taskId) }
            }
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error deleting task: ${e.message}", e)
        }
    }
}