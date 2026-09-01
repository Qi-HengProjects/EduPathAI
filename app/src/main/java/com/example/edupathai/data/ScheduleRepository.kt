package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ScheduleRepository {
    private val client = SupabaseProvider.client

    suspend fun getTasks(): List<ScheduleTask> = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("schedule_tasks").select {
                filter { eq("user_id", userId) }
                order(column = "start_time", order = Order.ASCENDING)
            }.decodeList<ScheduleTask>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createTask(task: ScheduleTask): ScheduleTask = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        val taskWithUser = task.copy(
            id = task.id ?: UUID.randomUUID().toString(),
            userId = userId
        )
        try {
            client.from("schedule_tasks").insert(taskWithUser) {
                select()
            }.decodeSingle<ScheduleTask>()
        } catch (_: Exception) {
            // If Supabase network/table is unavailable, return local task so UI continues working
            taskWithUser
        }
    }

    suspend fun updateTask(task: ScheduleTask): Boolean = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        val taskId = task.id ?: return@withContext false
        try {
            client.from("schedule_tasks").update(
                {
                    set("title", task.title)
                    set("start_time", task.startTime)
                    set("end_time", task.endTime)
                    set("energy_level", task.energyLevel)
                    set("task_type", task.taskType)
                    set("color_hex", task.colorHex)
                    set("is_completed", task.isCompleted)
                }
            ) {
                filter {
                    eq("id", taskId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            true // Allow local update
        }
    }

    suspend fun deleteTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = SupabaseProvider.getLocalUserId()
        try {
            client.from("schedule_tasks").delete {
                filter {
                    eq("id", taskId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            true // Allow local deletion
        }
    }
}