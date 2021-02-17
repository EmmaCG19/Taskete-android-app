package com.example.taskete.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.taskete.TASK_SELECTED
import com.example.taskete.data.Task


//DEBUGGING
private const val TAG_ACTIVITY = "TaskReminderReceiver"

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        /* Older version
        val task = intent?.extras?.getParcelable<Task>(TASK_SELECTED)

        val newIntent = Intent(context, NotificationService::class.java).apply {
            putExtra(TASK_SELECTED, task)
            context?.startService(intent)
        }
        */

        Log.d(TAG_ACTIVITY, "Receiver is intercepting the broadcast message")

        if (context != null && intent != null) {
            /****************************Debugging************************************/
            val test = intent.extras?.getString("Test")
            val taskId = intent.extras?.getInt("TAREA")
            val task = intent.extras?.getParcelable<Task?>(TASK_SELECTED)

            Log.d(TAG_ACTIVITY, "Test received: $test")
            Log.d(TAG_ACTIVITY, "Task received: $taskId")
            Log.d(TAG_ACTIVITY, "Task received: ${task?.id} | ${task?.title}")

            Log.d(TAG_ACTIVITY, "We are enqueuing the intent to a notification service")
            /***********************************************************************/

            NotificationService.enqueueWork(context, intent)
        }
    }
}