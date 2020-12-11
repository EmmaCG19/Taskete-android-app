package com.example.taskete

import android.app.AlarmManager
import android.app.NotificationManager
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
import com.example.taskete.data.Priority
import com.example.taskete.data.Task
import com.example.taskete.data.User
import com.example.taskete.db.TasksDAO
import com.example.taskete.extensions.stringFromDate
import com.example.taskete.helpers.KeyboardUtil
import com.example.taskete.helpers.UIManager
import com.example.taskete.notifications.TaskReminderReceiver
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
private const val REMINDER_TIME_IN_MINUTES = 1
private const val DEFAULT_REQUEST_CODE = 1000
const val REMINDER_INFO = "TaskInfo"

class TaskFormActivity : AppCompatActivity() {

    //TODO Use ViewBinding to reduce instantiations
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
    private lateinit var channelId: String
    private val compositeDisposable = CompositeDisposable()
    private var selectedDate: Date?
    private var selectedTask: Task?
    private var tasks: List<Task>
    private var flagDateSeleccionada: Boolean
    private var defaultUser: User

    private val dao: TasksDAO by lazy {
        TasksDAO(this@TaskFormActivity.applicationContext)
    }

    private val taskRetrieved: Task? by lazy {
        val bundle: Bundle? = intent.extras
        bundle?.getParcelable<Task>(TASK_SELECTED)
    }

    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    init {
        selectedDate = null
        selectedTask = null
        tasks = emptyList<Task>()
        flagDateSeleccionada = false
        defaultUser = User(1, "Test", "test@gmail.com", "1234", null, tasks)
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
            if (taskRetrieved != null) {
                editTask()
            } else {
                createTask()
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

    private fun hideDateSelection() {
        cardDate.visibility = View.GONE
        selectedDate = null
        flagDateSeleccionada = false
        cancelReminder()
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
        dateTimeFragment.set24HoursMode(false)

        calendar = GregorianCalendar()

        //We need to get actual datetime??
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
                    val currentTime = calendar.timeInMillis

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


    private fun setReminder() {
        if (flagDateSeleccionada && selectedDate != null) {
            val intent = createReminder()

            //Definir una alarma 1 minuto antes de la fecha limite
            selectedDate?.let {
                calendar.timeInMillis = it.time
                calendar.add(Calendar.MINUTE, -REMINDER_TIME_IN_MINUTES)
            }

            val reminderTime = calendar.timeInMillis

            //Setteo la alarma
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

    private fun createReminder(): PendingIntent {
        val taskId = selectedTask?.id

        val notifIntent = Intent(this, TaskReminderReceiver::class.java).also { i ->
            i.putExtra("TAREA", taskId)
            i.putExtra("Test", "Esta llegando el valor")
            i.putExtra(TASK_SELECTED, selectedTask)
        }

        Log.d(TAG_ACTIVITY, "Selected task: ${selectedTask?.id} | ${selectedTask?.title}")

        return PendingIntent.getBroadcast(
                this, selectedTask?.id
                ?: DEFAULT_REQUEST_CODE, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
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
                generateTask().also { task ->
                    selectedTask = task
                    dao.updateTask(task).subscribe()
                }

                setReminder()

            } catch (e: SQLException) {
                UIManager.showMessage(this, "There was an error when updating the task")
            } finally {
                finish()
            }
        } else {
            showErrorAlert()
        }
    }

    private fun createTask() {
        if (inputIsValid()) {
            try {
                val task = generateTask()
                dao.addTask(task).subscribe()

                getLastTask().also { task ->
                    selectedTask = task

                    if (task != null) {
                        setReminder()
                    }
                }

            } catch (e: SQLException) {
                UIManager.showMessage(this, "There was an error when creating the task")
            } finally {
                finish()
            }
        } else {
            showErrorAlert()
        }
    }

    private fun getTask(id: Int) {
        dao.getTask(id)
                .subscribe(object : SingleObserver<Task?> {
                    override fun onSubscribe(d: Disposable?) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(task: Task?) {
                        selectedTask = task
                    }

                    override fun onError(e: Throwable?) {
                        Log.d(TAG_ACTIVITY, "Error when getting task ", e)
                    }

                })
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
                    }

                    override fun onError(e: Throwable) {
                        Log.d(TAG_ACTIVITY, "Error when getting tasks ", e)
                    }

                })
    }

    private fun getLastTask(): Task? {
        getTasks()

        return tasks.firstOrNull { t ->
            t.title == getText(etTitle) &&
                    t.description == getText(etDesc) &&
                    t.priority == setPriority(rgPriorities.checkedRadioButtonId) &&
                    t.dueDate == selectedDate
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
                defaultUser
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

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}

