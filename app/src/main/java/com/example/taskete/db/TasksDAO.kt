package com.example.taskete.db

import android.content.Context
import com.example.taskete.data.Task
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao

class TasksDAO(context: Context) {
    private lateinit var dao: Dao<Task, Int>

    init {
        val helper = OpenHelperManager.getHelper(context, DBHelper::class.java)
        dao = helper.getDao(Task::class.java)
    }

    fun getTasks(): List<Task> = dao.queryForAll()

    fun getTask(task: Task): Task = dao.queryForId(task.id) //throw SQL exception

    fun deleteTask(task: Task) = dao.delete(task) //throw SQL exception

    fun addTask(task: Task) = dao.create(task) //

    fun updateTask(task: Task) = dao.update(task)
}