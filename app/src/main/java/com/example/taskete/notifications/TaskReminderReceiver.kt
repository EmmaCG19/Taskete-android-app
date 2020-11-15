package com.example.taskete.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

const val TASK_NOTIFICATION_ID = "Notification-id"
const val TASK_NOTIFICATION = "Notification"
const val DEFAULT_ID = 1000

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle = intent?.extras
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = bundle?.getParcelable<Notification>(TASK_NOTIFICATION)
        val notificationId = bundle?.getInt(TASK_NOTIFICATION_ID) ?: DEFAULT_ID

        notificationManager.notify(notificationId, notification)
    }

}