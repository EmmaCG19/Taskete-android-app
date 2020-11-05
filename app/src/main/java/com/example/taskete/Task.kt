package com.example.taskete

import java.util.*

class Task(
    var id: Int,
    var title: String,
    var description: String,
    var priority: Priority,
    var isDone: Boolean,
    var dueDate: Date? = Date()
) {
    //Default constructor that ORMLite needs
//    constructor():this(0, "", "", Priority.NOTASSIGNED,false)
}

enum class Priority {
    NOTASSIGNED,
    LOW,
    MEDIUM,
    HIGH
}