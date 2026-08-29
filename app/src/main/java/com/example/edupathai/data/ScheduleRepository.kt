package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchTasks(): List<ScheduleTask> = withContext(Dispatchers.IO) {
        client.from("schedule_tasks").select().decodeList<ScheduleTask>()
    }

    suspend fun addTask(task: ScheduleTask) = withContext(Dispatchers.IO) {
        client.from("schedule_tasks").insert(task)
    }

    suspend fun updateTask(task: ScheduleTask) = withContext(Dispatchers.IO) {
        task.id?.let { id ->
            client.from("schedule_tasks").update(task) {
                filter { eq("id", id) }
            }
        }
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        client.from("schedule_tasks").delete {
            filter { eq("id", taskId) }
        }
    }
}