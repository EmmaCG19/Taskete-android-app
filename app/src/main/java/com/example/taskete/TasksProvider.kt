package com.example.taskete

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.*

object TasksProvider {
    private var tasks: MutableList<Task> = mutableListOf<Task>()

    init {

        for (i in 1..15) {
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
        Log.d("TASK_EDIT", "Old task: ${oldTask?.id} | ${oldTask?.title} | ${oldTask?.description} | ${oldTask?.priority} | ${oldTask?.dueDate} | ${oldTask?.isDone}")

        tasks[index].title = task.title
        tasks[index].description = task.description
        tasks[index].dueDate = task.dueDate
        tasks[index].priority = task.priority
        tasks[index].isDone = task.isDone

        Log.d("TASK_EDIT", "New task: ${tasks[index].id} | ${tasks[index].title} | ${tasks[index].description} | ${tasks[index].priority} | ${tasks[index].dueDate} | ${tasks[index].isDone}")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun deleteTask(task: Task) {
        tasks.removeIf { t -> t.id == task.id }
    }
}

