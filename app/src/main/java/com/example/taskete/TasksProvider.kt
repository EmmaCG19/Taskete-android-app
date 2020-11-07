package com.example.taskete

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

object TasksProvider {
    private var tasks: MutableList<Task> = mutableListOf<Task>()

    init {
        tasks.add(
            Task(
                1,
                "This is a template title",
                "This is a template description",
                Priority.NOTASSIGNED,
                true
            )
        )
        tasks.add(
            Task(
                2,
                "This is a template title",
                "This is a template description",
                Priority.LOW,
                true
            )
        )
        tasks.add(
            Task(
                3,
                "This is a template title with a long text to test the limit of characters",
                "This is a template description",
                Priority.MEDIUM,
                false
            )
        )
        tasks.add(
            Task(
                4,
                "This is a template title",
                "This is a template description",
                Priority.HIGH,
                false
            )
        )
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

//        oldTask?.title = task.title
//        oldTask?.description =  task.description
//        oldTask?.dueDate = task.dueDate
//        oldTask?.priority = task.priority
//        oldTask?.isDone = task.isDone
//        Log.d("Number of tasks:", "${this.getTasks().size}")

    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun deleteTask(task: Task) {
        tasks.removeIf { t -> t.id == task.id }
    }
}

