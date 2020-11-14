package com.example.taskete.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.taskete.data.Task
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils


class DBHelper(context: Context) : OrmLiteSqliteOpenHelper(context, DB_NAME, null, DB_VERSION){
    override fun onCreate(database: SQLiteDatabase?, connectionSource: ConnectionSource?) {
        TableUtils.createTableIfNotExists(connectionSource, Task::class.java)
    }

    override fun onUpgrade(
            database: SQLiteDatabase?,
            connectionSource: ConnectionSource?,
            oldVersion: Int,
            newVersion: Int
    ) {

        onCreate(database, connectionSource)
    }


}