package com.example.edupathai.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class ScheduleRepository(
    private val geminiService: GeminiService = GeminiService()
) {
    private val client = SupabaseProvider.client

    suspend fun getTasks(): List<ScheduleTask> {
        return client.from("schedule_tasks").select {
            order(column = "start_time", order = Order.ASCENDING)
        }.decodeList<ScheduleTask>()
    }

    suspend fun createTask(task: ScheduleTask) {
        client.from("schedule_tasks").insert(task)
    }

    suspend fun updateTask(task: ScheduleTask) {
        task.id?.let { id ->
            client.from("schedule_tasks").update(task) {
                filter { eq("id", id) }
            }
        }
    }

    suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        client.from("schedule_tasks").update(
            { set("is_completed", isCompleted) }
        ) {
            filter { eq("id", taskId) }
        }
    }

    suspend fun deleteTask(taskId: String) {
        client.from("schedule_tasks").delete {
            filter { eq("id", taskId) }
        }
    }

    suspend fun generateAISchedule(prompt: String) {
        // 使用全新的 fetchStudySchedule 方法
        val generatedTasks = geminiService.fetchStudySchedule(prompt)
        for (task in generatedTasks) {
            createTask(task)
        }
    }
}