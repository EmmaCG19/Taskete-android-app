package com.example.taskete

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.XmlResourceParser
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.CalendarContract
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.taskete.data.Priority
import com.example.taskete.data.Task
import com.example.taskete.data.User
import com.example.taskete.db.TasksDAO
import com.example.taskete.extensions.stringFromDate
import com.example.taskete.helpers.KeyboardUtil
import com.example.taskete.helpers.UIManager
import com.example.taskete.notifications.TaskReminderReceiver
import com.example.taskete.preferences.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.sql.SQLException
import java.util.*

//DEBUGGING
private const val TAG_ACTIVITY = "TaskFormActivity"

private const val TAG_DATETIME_FRAGMENT = "DatetimeFragment"
private const val DEFAULT_REQUEST_CODE = 1000
private const val REMINDER_TIME_IN_MINUTES = 1
const val REMINDER_INFO = "TaskInfo"
const val CUSTOM_ACTION = "SET_REMINDER"


class TaskFormActivity : AppCompatActivity() {

    //TODO Use ViewBinding to reduce instantiations
    private lateinit var inputTitle: TextInputLayout
    private lateinit var inputDesc: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var rgPriorities: RadioGroup
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDatePicker: MaterialButton
    private lateinit var txtSelectedDate: TextView
    private lateinit var cardDate: CardView
    private lateinit var btnClearDate: ImageButton
    private var calendar: GregorianCalendar
    private var selectedDate: Date?
    private var selectedTask: Task?
    private var tasks: List<Task>
    private var flagDateSeleccionada: Boolean
    private val compositeDisposable = CompositeDisposable()

    private val dao: TasksDAO by lazy {
        TasksDAO(this@TaskFormActivity.applicationContext)
    }

    private val taskRetrieved: Task? by lazy {
        intent.extras?.getParcelable(TASK_SELECTED)
    }

    private val currentUser: User? by lazy {
        intent.extras?.getParcelable(LOGGED_USER)
    }

    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    init {
        tasks = emptyList()
        selectedTask = null
        selectedDate = null
        flagDateSeleccionada = false
        calendar = GregorianCalendar()
    }

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
            if(SessionManager.isTrialMode()){
                showSaveBtnDisabled()
            }
            else{
                if (taskRetrieved != null) {
                    editTask()
                } else {
                    createTask()
                }
            }

        }

        btnClearDate.setOnClickListener {
            hideDateSelection()
            cancelReminder()
        }

        btnDatePicker.setOnClickListener {
            KeyboardUtil.hideKeyboard(this)
            showDateTimePicker()
        }
    }

    private fun showSaveBtnDisabled() {
        btnSave.setBackgroundColor(resources.getColor(R.color.colorTextDisabled, null))
        UIManager.showDisabledFeature(this, "Save feature")
    }

    private fun hideDateSelection() {
        UIManager.hide(cardDate)
        selectedDate = null
        flagDateSeleccionada = false
    }



    private fun showDateSelection(date: Date?) {
        date?.let {
            selectedDate = it
            UIManager.show(cardDate)
            txtSelectedDate.text = selectedDate?.stringFromDate()
        }
    }

    private fun showDateTimePicker() {
        //Construct datetime picker fragment
        val dateTimeFragment =
                (supportFragmentManager.findFragmentByTag(TAG_DATETIME_FRAGMENT)
                        ?: SwitchDateTimeDialogFragment.newInstance(
                                resources.getText(R.string.due_time_title).toString(),
                                resources.getText(R.string.due_time_ok).toString(),
                                resources.getText(R.string.due_time_cancel).toString()
                        )) as SwitchDateTimeDialogFragment


        dateTimeFragment.setTimeZone(TimeZone.getDefault())
        dateTimeFragment.set24HoursMode(false)

        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.HOUR, -1)
        dateTimeFragment.minimumDateTime = calendar.time

        calendar.set(2030, Calendar.JANUARY, 1)
        dateTimeFragment.maximumDateTime = calendar.time

        dateTimeFragment.setOnButtonClickListener(object :
                SwitchDateTimeDialogFragment.OnButtonClickListener {

            override fun onPositiveButtonClick(date: Date?) {
                //Debo verificar que el usuario no elija una fecha y hora menor a la actual
                date?.let {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.MINUTE, -1) //Give the user one minute to choose

                    flagDateSeleccionada = if (it.time >= calendar.timeInMillis) {
                        showDateSelection(date)
                        true
                    } else {
                        UIManager.showMessage(
                                this@TaskFormActivity,
                                "The selected due time can't be lower than the current time"
                        )
                        false
                    }
                }
            }

            override fun onNegativeButtonClick(date: Date?) {
                //Nothing happens here
            }
        })

        dateTimeFragment.show(supportFragmentManager, TAG_DATETIME_FRAGMENT)

    }

    private fun createReminder(): PendingIntent {
        val notifIntent = Intent(this, TaskReminderReceiver::class.java).also { i ->
            i.putExtra(TASK_SELECTED, selectedTask?.id)
            i.putExtra(LOGGED_USER, currentUser?.id)
        }

        return PendingIntent.getBroadcast(
                this, selectedTask?.id
                ?: DEFAULT_REQUEST_CODE, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private fun setReminder() {
        if (flagDateSeleccionada && selectedDate != null) {
            val intent = createReminder()

            selectedDate?.let {
                calendar.timeInMillis = it.time
                calendar.add(Calendar.MINUTE, -REMINDER_TIME_IN_MINUTES)
            }

            val reminderTime = calendar.timeInMillis

            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, intent)
        }
    }

    private fun cancelReminder() {
        val intent = createReminder()
        alarmManager.cancel(intent)
        intent.cancel()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        selectedTask?.id?.let {
            notificationManager.cancel(it)
            Log.d(TAG_ACTIVITY, "Notification was cancelled")
        }
    }

    private fun updateFields() {
        etTitle.setText(taskRetrieved?.title)
        etDesc.setText(taskRetrieved?.description)
        val rbSelected = getCheckedPriority(taskRetrieved?.priority)
        rgPriorities.check(rbSelected)
        showDateSelection(taskRetrieved?.dueDate)
    }

    private fun updateTask(task: Task) {
        dao.updateTask(task).subscribe()
    }

    private fun addTask(task: Task) {
        dao.addTask(task).subscribe(object : SingleObserver<Int> {
            override fun onSubscribe(d: Disposable?) {
                compositeDisposable.add(d)
            }

            override fun onSuccess(t: Int?) {
                Log.d(TAG_ACTIVITY, "Task was created in DB")
                getTasks()
            }

            override fun onError(e: Throwable?) {
                Log.d(TAG_ACTIVITY, "Error when creating task in DB, because ${e?.message}")
            }

        })
    }


    private fun editTask() {
        if (inputIsValid()) {
            try {
                generateTask().also {
                    selectedTask = it
                    //Actualizo la tarea
                    updateTask(it)
                }

                setReminder()

            } catch (e: SQLException) {
                UIManager.showMessage(this, "There was an error when updating the task")
            } finally {
                finishActivity()
            }
        } else {
            showErrorAlert()
        }
    }

    private fun createTask() {
        if (inputIsValid()) {
            try {
                //Genero tarea
                generateTask().also {
                    addTask(it)
                }

            } catch (e: SQLException) {
                UIManager.showMessage(this, "There was an error when creating the task")
            } finally {
                finishActivity()
            }
        } else {
            showErrorAlert()
        }
    }

    private fun finishActivity() {
        Handler(mainLooper).postDelayed({
            finish()
        }, 5000)
    }

    private fun getTasks() {
        dao.getTasks()
                .subscribe(object : SingleObserver<List<Task>> {
                    override fun onSubscribe(d: Disposable?) {
                        Log.d(TAG_ACTIVITY, "An Observer subscribed to an observable")
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(listOfTasks: List<Task>) {
                        Log.d(TAG_ACTIVITY, "The tasks were retrieved successfully")
                        tasks = listOfTasks
                        selectedTask = getLastTask()
                        setReminder()
                    }

                    override fun onError(e: Throwable) {
                        Log.d(TAG_ACTIVITY, "Error when getting tasks ", e)
                    }

                })
    }


    private fun getLastTask(): Task? {
        return tasks.firstOrNull { t ->
            t.title == getText(etTitle) &&
                    t.description == getText(etDesc) &&
                    t.priority == setPriority(rgPriorities.checkedRadioButtonId) &&
                    t.dueDate == selectedDate &&
                    t.user?.id == currentUser?.id
        }
    }

    private fun generateTask(): Task {
        return Task(
                taskRetrieved?.id,
                getText(etTitle),
                getText(etDesc),
                setPriority(rgPriorities.checkedRadioButtonId),
                taskRetrieved?.isDone ?: false,
                selectedDate,
                currentUser
        )
    }

    private fun inputIsValid(): Boolean {
        return if (getText(etTitle).trim().isEmpty()) {
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

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
}

