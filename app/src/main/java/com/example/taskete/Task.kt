package com.example.taskete

import java.util.*

class Task(
    var id: Int, //Autogenerado
    var title: String, //TextInputlayout
    var description: String, //TextInputlayout
    var priority: Priority, //Radio button
    var isDone: Boolean, //Not in form
    var dueDate: Date? = Date() //Datepicker
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