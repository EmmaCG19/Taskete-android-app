package com.example.taskete

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import com.example.taskete.data.Priority
import com.example.taskete.data.Task
import com.example.taskete.db.TasksDAO
import com.example.taskete.extensions.stringFromDate
import com.example.taskete.helpers.KeyboardUtil
import com.example.taskete.helpers.UIManager
import com.example.taskete.notifications.TASK_NOTIFICATION
import com.example.taskete.notifications.TASK_NOTIFICATION_ID
import com.example.taskete.notifications.TaskNotifChannelManager
import com.example.taskete.notifications.TaskReminderReceiver
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment
import java.sql.SQLException
import java.util.*

private const val TAG_DATETIME_FRAGMENT = "DatetimeFragment"
private const val REMINDER_TIME = 3600

class TaskFormActivity : AppCompatActivity() {
    private lateinit var inputTitle: TextInputLayout
    private lateinit var inputDesc: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var rgPriorities: RadioGroup
    private lateinit var btnSave: ImageButton
    private lateinit var btnDatePicker: MaterialButton
    private lateinit var switchDateTime: SwitchDateTimeDialogFragment
    private lateinit var txtSelectedDate: TextView
    private lateinit var cardDate: CardView
    private lateinit var btnClearDate: ImageButton
    private lateinit var calendar: GregorianCalendar
    private var flagEdit: Boolean = false
    private var flagDateSeleccionada = false
    private val dao: TasksDAO by lazy {
        TasksDAO(this@TaskFormActivity.applicationContext)
    }

    private var selectedDate: Date? = null
    private val taskRetrieved: Task? by lazy {
        intent.extras?.getParcelable<Task>(TASK_SELECTED)
    }

    private lateinit var channelId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_form)
        setupUI()
    }

    private fun setupUI() {
        inputTitle = findViewById(R.id.inputTitle)
        inputDesc = findViewById(R.id.inputDesc)
        etTitle = findViewById(R.id.etTitle)
        etDesc = findViewById(R.id.etDesc)
        rgPriorities = findViewById(R.id.rgPriorities)
        btnSave = findViewById(R.id.btnSave)

        btnDatePicker = findViewById(R.id.btnDatePicker)
        txtSelectedDate = findViewById(R.id.txtSelectedDate)
        cardDate = findViewById(R.id.cardDate)
        btnClearDate = findViewById(R.id.btnClearDate)

        hideDateSelection()
        updateFields()
        setListeners()
    }

    private fun setListeners() {

        btnSave.setOnClickListener {
            if (taskRetrieved != null) {
                flagEdit = true
                editTask()
                setReminder(taskRetrieved)
            } else {
                createTask()
            }
        }

        btnClearDate.setOnClickListener {
            hideDateSelection()
        }

        btnDatePicker.setOnClickListener {
            KeyboardUtil.hideKeyboard(this)
            showDateTimePicker()
        }
    }

    private fun hideDateSelection() {
        cardDate.visibility = View.GONE
        selectedDate = null
    }

    private fun showDateSelection(date: Date?) {
        selectedDate = date
        cardDate.visibility = View.VISIBLE
        txtSelectedDate.text = selectedDate?.stringFromDate()
    }

    private fun showDateTimePicker() {

        //Construct datetime picker fragment
        var dateTimeFragment =
                (supportFragmentManager.findFragmentByTag(TAG_DATETIME_FRAGMENT)
                        ?: SwitchDateTimeDialogFragment.newInstance(
                                resources.getText(R.string.due_time_title).toString(),
                                resources.getText(R.string.due_time_ok).toString(),
                                resources.getText(R.string.due_time_cancel).toString()
                        )) as SwitchDateTimeDialogFragment


        dateTimeFragment.setTimeZone(TimeZone.getDefault())
        dateTimeFragment.set24HoursMode(true)

        calendar = GregorianCalendar()

        calendar.set(2020, Calendar.JANUARY, 1)
        dateTimeFragment.minimumDateTime = calendar.time
        calendar.set(2030, Calendar.JANUARY, 1)
        dateTimeFragment.maximumDateTime = calendar.time

        dateTimeFragment.setOnButtonClickListener(object :
                SwitchDateTimeDialogFragment.OnButtonClickListener {

            override fun onPositiveButtonClick(date: Date?) {
                showDateSelection(date)
                flagDateSeleccionada = true
            }

            override fun onNegativeButtonClick(date: Date?) {
            }
        })

        dateTimeFragment.show(supportFragmentManager, TAG_DATETIME_FRAGMENT)

    }

    private fun createReminder(task: Task?): Notification {
        channelId = TaskNotifChannelManager.createNotificationReminderChannel(this)

        return NotificationCompat.Builder(this, channelId)
                .setContentTitle(task?.title)
                .setContentText("Time to do this task")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setChannelId(channelId)
                .setAutoCancel(true)
                .build()
    }


    private fun setReminder(task: Task?) {
        if (flagDateSeleccionada && selectedDate != null) {
            val notification = createReminder(task)

            val intent = Intent(this, TaskReminderReceiver::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(TASK_NOTIFICATION_ID, task?.id)
                putExtra(TASK_NOTIFICATION, notification)
            }

            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val reminderTime = selectedDate?.time?.minus(REMINDER_TIME)
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime!!, pendingIntent)
        }
    }

    private fun updateFields() {
        etTitle.setText(taskRetrieved?.title)
        etDesc.setText(taskRetrieved?.description)
        val rbSelected = getCheckedPriority(taskRetrieved?.priority)
        rgPriorities.check(rbSelected)

        if (taskRetrieved?.dueDate != null)
            showDateSelection(taskRetrieved?.dueDate)
    }

    private fun editTask() {
        if (inputIsValid()) {
            try {
                dao.updateTask(generateTask())
                finish()
            } catch (e: SQLException) {
                UIManager.showMessage(this, "There was an error when updating the task")
                finish()
            }
        } else {
            showErrorAlert()
        }
    }

    private fun createTask() {
        if (inputIsValid()) {
            try {
                //Creo la tarea
                dao.addTask(generateTask())
                //Obtener su id
/*                val lastId = dao.getLastTaskId()
                setReminder(task)*/
                finish()
            } catch (e: SQLException) {
                UIManager.showMessage(this,"There was an error when creating the task")
                finish()
            }
        } else {
            showErrorAlert()
        }
    }

    private fun generateTask(): Task {
        return Task(
                if (flagEdit) taskRetrieved?.id else null,
                getText(etTitle),
                getText(etDesc),
                setPriority(rgPriorities.checkedRadioButtonId),
                if (flagEdit) taskRetrieved!!.isDone else false,
                selectedDate
        )
    }

    private fun inputIsValid(): Boolean {
        return if (getText(etTitle).trim().isNullOrEmpty()) {
            inputTitle.error = "You must complete this field"
            false
        } else {
            true
        }
    }

    private fun setPriority(checked: Int): Priority {
        return when (checked) {
            R.id.rbHighPriority -> Priority.HIGH
            R.id.rbMediumPriority -> Priority.MEDIUM
            R.id.rbLowPriority -> Priority.LOW
            else -> Priority.NOTASSIGNED
        }
    }

    private fun getCheckedPriority(priority: Priority?): Int {
        return when (priority) {
            Priority.HIGH -> R.id.rbHighPriority
            Priority.MEDIUM -> R.id.rbMediumPriority
            Priority.LOW -> R.id.rbLowPriority
            else -> R.id.rbNotPriority
        }

    }

    private fun getText(editText: EditText) = editText.text.toString()

    private fun showErrorAlert() {
        UIManager.showMessage(this, "One or more errors have ocurred")
    }
}
