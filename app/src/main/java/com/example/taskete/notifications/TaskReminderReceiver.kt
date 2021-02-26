package com.example.taskete.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.taskete.LOGGED_USER
import com.example.taskete.TASK_SELECTED
import com.example.taskete.data.Task
import com.example.taskete.data.User
import com.example.taskete.helpers.ParcelableUtil


//DEBUGGING
private const val TAG_ACTIVITY = "TaskReminderReceiver"

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        context?.let {
            val taskExtra = intent?.extras?.getInt(TASK_SELECTED)
            val userExtra = intent?.extras?.getInt(LOGGED_USER)

            if (taskExtra != null && userExtra != null) {
                NotificationService.enqueueWork(it, intent)
            } else
                Log.d(TAG_ACTIVITY, "TASK/USER DATA NOT FOUND")
        }
    }
}