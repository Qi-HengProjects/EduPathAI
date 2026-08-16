package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class ScheduleRepository {
    private val client = SupabaseProvider.client

    suspend fun getTasks(): List<ScheduleTask> {
        return client.from("schedule_tasks").select {
            order(column = "start_time", order = Order.ASCENDING)
        }.decodeList<ScheduleTask>()
    }

    suspend fun createTask(task: ScheduleTask) {
        client.from("schedule_tasks").insert(task)
    }

    suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        client.from("schedule_tasks").update(
            {
                set("is_completed", isCompleted)
            }
        ) {
            filter {
                eq("id", taskId)
            }
        }
    }

    suspend fun deleteTask(taskId: String) {
        client.from("schedule_tasks").delete {
            filter {
                eq("id", taskId)
            }
        }
    }
}