package com.example.taskete.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import com.example.taskete.R
import com.example.taskete.TASK_SELECTED
import com.example.taskete.TaskFormActivity
import com.example.taskete.data.Task
import com.example.taskete.extensions.stringFromDate
import java.util.*

//DEBUGGING
private const val TAG_ACTIVITY = "NotificationService"

const val TASK_NOTIFICATION_ID = "Notification-id"
const val TASK_NOTIFICATION = "Notification"
const val JOB_ID = 100
const val DEFAULT_ID = 1000

class NotificationService : JobIntentService() {

    private val channelId: String by lazy {
        TaskNotifChannelManager.createNotificationReminderChannel(this)
    }

    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            //Older versions (before O) will use startService
            enqueueWork(context, NotificationService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val task = getSelectedTask(intent)
        val notification = createNotification(task)

        val intent = Intent(this, TaskFormActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
            putExtra(TASK_SELECTED, task)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        notification.contentIntent = pendingIntent


        /****************************Debugging************************************/
        Log.d(TAG_ACTIVITY, "Task to notify: ${task?.id} | ${task?.title}")
        notificationManager.notify(task?.id ?: DEFAULT_ID, notification)

        val calendar = GregorianCalendar()
        calendar.timeInMillis =  System.currentTimeMillis()
        val date = calendar.time.stringFromDate()
        Log.d(TAG_ACTIVITY, "Notification was sent at $date")
        /***********************************************************************/


    }

    private fun getSelectedTask(intent: Intent): Task? {
        return intent.extras?.getParcelable<Task>(TASK_SELECTED)
    }

    private fun createNotification(task: Task?): Notification {
        return NotificationCompat.Builder(this, channelId)
                .setContentTitle("Reminder time")
                .setContentText(task?.title)
                .setSmallIcon(R.drawable.ic_app_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setChannelId(channelId)
                .setAutoCancel(true)
                .build()
    }
}
