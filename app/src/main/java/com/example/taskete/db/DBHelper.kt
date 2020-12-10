package com.example.taskete.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.taskete.data.Task
import com.example.taskete.data.User
import com.j256.ormlite.android.apptools.OrmLiteConfigUtil
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.sql.SQLException


class DBHelper(val context: Context) : OrmLiteSqliteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(database: SQLiteDatabase?, connectionSource: ConnectionSource?) {
        TableUtils.createTableIfNotExists(connectionSource, Task::class.java)
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
        } catch (e: SQLException) {
            Log.d("DB_UPDATE", "Error when updating database: ${e.message}")
        }
    }

    private fun createUsersTable() {
        //Add a Users table
        TableUtils.createTableIfNotExists(connectionSource, User::class.java)
        UsersDAO(context)
                .executeCustomQuery(
                        "INSERT INTO Users(username, mail, password, avatar) " +
                                "VALUES('Test', 'test@gmail.com', '1234', NULL);") //userId = 1 -> TestUser
                .subscribe(object : SingleObserver<Int> {
                    override fun onSubscribe(d: Disposable?) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(t: Int?) {
                        Log.d("DB_UPDATE", "The changes in Users table were successful")
                    }

                    override fun onError(e: Throwable?) {
                        Log.d("DB_UPDATE", "The changes in Users table couldn't be done because $e")
                    }

                })
    }

    private fun addUserColumnInTasksTable() {
        //TODO: Check if userId must be null or not

        //Synchronous
        val dao = TasksDAO(context)

        //Create new table with FK column
        dao.executeCustomQuery(
                "CREATE TABLE `Tasks_new` (" +
                        "id INTEGER PRIMARY KEY," +
                        "title TEXT," +
                        "description TEXT," +
                        "priority INTEGER," +
                        "isDone INTEGER," +
                        "dueDate TEXT," +
                        "userId INTEGER," +
                        "FOREIGN KEY (userId) REFERENCES Users(id)" +
                        ");"
        )

        //Copy values from oldTable to the new
        dao.executeCustomQuery(
                "INSERT INTO `Tasks_new`(id, title, description, priority, isDone, dueDate, userId) " +
                        "SELECT(" +
                        "id, " +
                        "title, " +
                        "description, " +
                        "priority, " +
                        "isDone, " +
                        "dueDate, " +
                        "1 ) "+
                        "FROM `Tasks`;"
        )

        //Drop old table
        dao.executeCustomQuery(
                "DROP TABLE `Tasks`;"
        )

        //Rename new table
        dao.executeCustomQuery("ALTER TABLE `Tasks_new` RENAME TO `Tasks`;")


//        TasksDAO(context)
//                .executeCustomQuery(
//                        "ALTER TABLE `Tasks` ADD COLUMN user_id INTEGER NOT NULL;" +
//                                "ALTER TABLE `Tasks` ADD CONSTRAINT " +
//                                "UPDATE Tasks SET user_id = 1;") //userId = 1 -> TestUser
//                .subscribe(object : SingleObserver<Int> {
//                    override fun onSubscribe(d: Disposable?) {
//                        compositeDisposable.add(d)
//                    }
//
//                    override fun onSuccess(t: Int?) {
//                        Log.d("DB_UPDATE", "The changes in Tasks table were successful")
//                    }
//
//                    override fun onError(e: Throwable?) {
//                        Log.d("DB_UPDATE", "The changes in Tasks table couldn't be done because $e")
//                    }
//
//                })
    }

}