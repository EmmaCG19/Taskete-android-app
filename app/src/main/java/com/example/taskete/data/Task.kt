package com.example.taskete.data

import java.util.*
import android.os.Parcelable
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import kotlinx.android.parcel.Parcelize

@Parcelize
@DatabaseTable(tableName= "Tasks")
class Task(
        @DatabaseField(id = true)
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
        var dueDate: Date?
) : Parcelable {

    //Default constructor that ORMLite needs
    constructor():this(null, "","", Priority.NOTASSIGNED,false, null)
}

enum class Priority {
    NOTASSIGNED,
    LOW,
    MEDIUM,
    HIGH
}