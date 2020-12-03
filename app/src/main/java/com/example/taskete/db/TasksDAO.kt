package com.example.taskete.db

import android.content.Context
import android.util.Log
import com.example.taskete.data.Task
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder
import io.reactivex.rxjava3.core.Single

class TasksDAO(context: Context) {
    private lateinit var dao: Dao<Task, Int>

    init {
        val helper = OpenHelperManager.getHelper(context, DBHelper::class.java)
        dao = helper.getDao(Task::class.java)
    }

    fun getTasks(): Single<List<Task>> {
        return Single.just(dao.queryForAll())
    }

    fun getTask(taskId: Int): Single<Task?> {
        return Single.just(dao.queryForId(taskId))
    }

    fun deleteTask(task: Task) = dao.delete(task)

    fun addTask(task: Task) = dao.create(task)

    fun updateTask(task: Task) = dao.update(task)

}