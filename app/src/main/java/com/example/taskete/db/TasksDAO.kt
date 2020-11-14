package com.example.taskete.db

import android.content.Context
import android.util.Log
import com.example.taskete.data.Task
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder

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

    fun getLastTaskId(): Int {
        val qb: QueryBuilder<Task, Int> = dao.queryBuilder()
        qb.selectRaw("MAX(id)")
        val results = dao.queryRaw(qb.prepareStatementString())
        return 0
    }
}