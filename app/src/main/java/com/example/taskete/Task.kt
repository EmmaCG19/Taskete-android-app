package com.example.taskete

import java.util.*
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Task(
        var id: Int,
        var title: String,
        var description: String,
        var priority: Priority,
        var isDone: Boolean,
        var dueDate: Date?
) : Parcelable {
    //Default constructor that ORMLite needs
    constructor():this(0, "","", Priority.NOTASSIGNED,false, null)
}

enum class Priority {
    NOTASSIGNED,
    LOW,
    MEDIUM,
    HIGH
}