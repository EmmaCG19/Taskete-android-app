package com.example.taskete

import android.util.Log
import com.example.taskete.data.Priority
import com.example.taskete.data.Task

object TasksProvider {
    private var tasks: MutableList<Task> = mutableListOf<Task>()


    init {

        for (i in 1..10) {
            tasks.add(
                    Task(
                            i,
                            "Task $i",
                            "This is a template description",
                            Priority.LOW,
                            false,
                            null
                    )
            )
        }
    }

    fun getTasks(): List<Task> = tasks

    fun addTask(task: Task) = tasks.add(task)

    fun editTask(task: Task) {
        val oldTask = tasks.firstOrNull { t -> t.id == task.id }
        val index = tasks.indexOf(oldTask)

        tasks[index].title = task.title
        tasks[index].description = task.description
        tasks[index].dueDate = task.dueDate
        tasks[index].priority = task.priority
        tasks[index].isDone = task.isDone

        Log.d("TASK_EDIT", "New task: ${tasks[index].id} | ${tasks[index].title} | ${tasks[index].description} | ${tasks[index].priority} | ${tasks[index].dueDate} | ${tasks[index].isDone}")
    }

    fun deleteTasks(newTasks: List<Task>) {
        tasks.removeAll(newTasks)
    }
}

