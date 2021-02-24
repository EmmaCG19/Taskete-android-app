package com.example.taskete.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.util.Log
import com.example.taskete.data.Task
import com.example.taskete.data.User
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.sql.SQLException

private const val DB_INFO = "DB_UPDATE"

class DBHelper(val context: Context) : OrmLiteSqliteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(database: SQLiteDatabase?, connectionSource: ConnectionSource?) {
        createUsersTable()
        createTasksTable()

        //TODO:DELETE THIS
        val queries = arrayListOf(
                "INSERT INTO `Tasks`" +
                        "(title, description, priority, isDone, dueDate, ${Task.USER_COL})" +
                        " VALUES" +
                        " ('Task1', 'This is a task', 'HIGH', 0, NULL, 1)," +
                        " ('Task2', 'This is a task', 'LOW', 0, NULL, 1)," +
                        " ('Task3', 'This is a task', 'LOW', 1, NULL, 1)," +
                        " ('Task4', 'This is a task', 'NOTASSIGNED', 1, NULL, 1)" +
                        ";"
        )
        insertTasksCustomQueries(queries)
    }

    override fun onUpgrade(
            database: SQLiteDatabase?,
            connectionSource: ConnectionSource?,
            oldVersion: Int,
            newVersion: Int
    ) {

        if (oldVersion < 2) {
            upgradeToVersion2()
        }
    }

    private fun upgradeToVersion2() {
        try {
            createUsersTable()
            addUserColumnInTasksTable()
            Log.d(DB_INFO, "The database upgrade to v2 was successful")

        } catch (e: SQLException) {
            Log.d(DB_INFO, "Error when upgrading database: ${e.message}")
        }
    }

    private fun createTasksTable() {
        TableUtils.createTableIfNotExists(connectionSource, Task::class.java)
    }

    private fun createUsersTable() {
        TableUtils.createTableIfNotExists(connectionSource, User::class.java)
        insertUsersCustomQuery(
                "INSERT INTO Users(username, mail, password, avatar) " +
                        "VALUES('Test', 'test@gmail.com', '1234', NULL);"
        )
    }

    private fun addUserColumnInTasksTable() {
        val queries = arrayListOf<String>()

        //TODO: Use StringBuilder instead of (+) to concat

        //Create new table with FK column
        queries.add(
                "CREATE TABLE `Tasks_new` " +
                        "(" +
                        "id INTEGER PRIMARY KEY," +
                        "title TEXT," +
                        "description TEXT," +
                        "priority INTEGER," +
                        "isDone INTEGER," +
                        "dueDate TEXT," +
                        "${Task.USER_COL} INTEGER," +
                        "FOREIGN KEY (userId) REFERENCES Users(id)" +
                        ");")

        //TODO: Check (possible generated_Id issue when copy )
        //Copy values from oldTable to the new
        queries.add(
                "INSERT INTO `Tasks_new`" +
                        "(id, title, description, priority, isDone, dueDate, ${Task.USER_COL}) " +
                        "SELECT " +
                        "id, " +
                        "title, " +
                        "description, " +
                        "priority, " +
                        "isDone, " +
                        "dueDate, " +
                        "1 " +
                        "FROM `Tasks`;"
        )

        //Drop old table
        queries.add("DROP TABLE `Tasks`;")

        //Rename new table
        queries.add("ALTER TABLE `Tasks_new` RENAME TO `Tasks`;")

        //TODO:DELETE THIS
        //Add some tasks entries
        queries.add(
                "INSERT INTO `Tasks`" +
                        "(title, description, priority, isDone, dueDate, ${Task.USER_COL})" +
                        " VALUES" +
                        " ('Task1', 'This is a task', 'HIGH', 0, NULL, 1)," +
                        " ('Task2', 'This is a task', 'LOW', 0, NULL, 1)," +
                        " ('Task3', 'This is a task', 'LOW', 1, NULL, 1)," +
                        " ('Task4', 'This is a task', 'NOTASSIGNED', 1, NULL, 1)" +
                        ";"
        )

        insertTasksCustomQueries(queries)
    }


    private fun insertTasksCustomQueries(queries: List<String>) {
        TasksDAO(context)
                .executeCustomQueries(queries)
                .subscribe(object : SingleObserver<Unit> {
                    override fun onSubscribe(d: Disposable?) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(t: Unit?) {
                        Log.d(DB_INFO, "The changes in Tasks table were successful")
                    }

                    override fun onError(e: Throwable) {
                        Log.d(DB_INFO, "The changes in Tasks couldn't be done because ${e.message}")
                    }
                })
    }

    private fun insertUsersCustomQuery(query: String) {
        UsersDAO(context)
                .executeCustomQuery(query)
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable?) {
                    }

                    override fun onSuccess(t: Int?) {
                        Log.d(DB_INFO, "The changes in Users table were successful")
                    }

                    override fun onError(e: Throwable) {
                        Log.d(DB_INFO, "The changes in Users couldn't be done because ${e.message}")
                    }
                })
    }

    //TODO: Revisar las suscripciones en los INSERT
}