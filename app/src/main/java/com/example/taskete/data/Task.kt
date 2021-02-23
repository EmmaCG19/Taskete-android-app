package com.example.taskete.data

import java.util.*
import android.os.Parcelable
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import kotlinx.android.parcel.Parcelize

@Parcelize
@DatabaseTable(tableName = "Tasks")
class Task(
        @DatabaseField(generatedId = true)
        var id: Int? = null,
        @DatabaseField
        var title: String,
        @DatabaseField
        var description: String,
        @DatabaseField
        var priority: Priority,
        @DatabaseField
        var isDone: Boolean,
        @DatabaseField
        var dueDate: Date?,
        @DatabaseField(columnName = USER_COL, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true, canBeNull = false)
        var user: User?
) : Parcelable {

    companion object {
        const val USER_COL:String = "userId"
    }


    //Default constructor that ORMLite needs
    constructor() : this(null, "", "", Priority.NOTASSIGNED, false, null, null)
}

enum class Priority {
    NOTASSIGNED,
    LOW,
    MEDIUM,
    HIGH
}
