package com.example.taskete

object TasksProvider {
    fun getTasks(): List<Task> = listOf<Task>(
            Task(1, "This is a template title", "This is a template description", Priority.NOTASSIGNED, true),
            Task(2, "This is a template title", "This is a template description", Priority.LOW, true),
            Task(3, "This is a template title", "This is a template description", Priority.MEDIUM, false),
    )
}

