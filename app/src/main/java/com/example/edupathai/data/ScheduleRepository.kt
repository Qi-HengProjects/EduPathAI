package com.example.edupathai.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleRepository {
    private val client = SupabaseProvider.client

    suspend fun getTasks(): List<ScheduleTask> = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        try {
            client.from("schedule_tasks").select {
                filter { eq("user_id", userId) }
                order(column = "start_time", order = Order.ASCENDING)
            }.decodeList<ScheduleTask>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createTask(task: ScheduleTask): ScheduleTask? = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            val taskWithUser = task.copy(userId = userId)
            client.from("schedule_tasks").insert(taskWithUser) {
                select()
            }.decodeSingle<ScheduleTask>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateTask(task: ScheduleTask): Boolean = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
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
                    if (task.taskDate != null) {
                        set("task_date", task.taskDate)
                    }
                }
            ) {
                filter {
                    eq("id", taskId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
        try {
            client.from("schedule_tasks").delete {
                filter {
                    eq("id", taskId)
                    eq("user_id", userId)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}