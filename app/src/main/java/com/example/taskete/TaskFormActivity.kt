package com.example.taskete

import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import com.example.taskete.helpers.KeyboardUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment

private const val TAG_ACTIVITY = "TaskFormActivity"

class TaskFormActivity : AppCompatActivity() {
    private val TAG_DATETIME_FRAGMENT = "TAG_DATETIME_FRAGMENT"
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
    private val dateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("d MMM yyyy HH:mm")
    }

    private var selectedDate: Date? = null

    private val taskRetrieved: Task? by lazy {
        intent.extras?.getParcelable<Task>(TASK_SELECTED)
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

        //Date section
        btnDatePicker = findViewById(R.id.btnDatePicker)
        txtSelectedDate = findViewById(R.id.txtSelectedDate)
        cardDate = findViewById(R.id.cardDate)
        btnClearDate = findViewById(R.id.btnClearDate)
        hideDateSelection()

        updateFields()

        btnSave.setOnClickListener {
            if (taskRetrieved != null) {
                editTask()
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
        txtSelectedDate.text = dateFormat.format(date)
    }

    private fun showDateTimePicker() {

        //Construct datetime picker fragment
        var dateTimeFragment: SwitchDateTimeDialogFragment =
                (supportFragmentManager.findFragmentByTag(TAG_DATETIME_FRAGMENT)
                        ?: SwitchDateTimeDialogFragment.newInstance(
                                resources.getText(R.string.due_time_title).toString(),
                                resources.getText(R.string.due_time_ok).toString(),
                                resources.getText(R.string.due_time_cancel).toString()
                        )) as SwitchDateTimeDialogFragment


        //Set picker options
        dateTimeFragment.setTimeZone(TimeZone.getDefault())
        dateTimeFragment.set24HoursMode(true)

        //Set date limits
        val calendar: GregorianCalendar = GregorianCalendar()

        //TODO: Utilizar la fecha actual como limite para poder seleccionar
        calendar.set(2020, Calendar.JANUARY, 1)
        dateTimeFragment.minimumDateTime = calendar.time
        calendar.set(2030, Calendar.JANUARY, 1)
        dateTimeFragment.maximumDateTime = calendar.time

        //Set picker listener
        dateTimeFragment.setOnButtonClickListener(object :
                SwitchDateTimeDialogFragment.OnButtonClickListener {

            override fun onPositiveButtonClick(date: Date?) {

                showDateSelection(date)
            }

            override fun onNegativeButtonClick(date: Date?) {
                //Nothing here
            }
        })

        //Show fragment
        dateTimeFragment.show(supportFragmentManager, TAG_DATETIME_FRAGMENT)

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
        Log.d(TAG_ACTIVITY, "La tarea se va a editar")

        if (inputIsValid()) {
            taskRetrieved?.title = getText(etTitle)
            taskRetrieved?.description = getText(etDesc)
            taskRetrieved?.priority = setPriority(rgPriorities.checkedRadioButtonId)
            taskRetrieved?.dueDate = selectedDate
            TasksProvider.editTask(taskRetrieved!!)
            finish()
        } else {
            showErrorAlert()
        }
    }

    private fun createTask() {
        if (inputIsValid()) {
            val newTask = generateTask()
            TasksProvider.addTask(newTask)
            finish()
        } else {
            showErrorAlert()
        }
    }

    private fun generateTask(): Task {
        val title = getText(etTitle)
        val description = getText(etDesc)
        val priority = setPriority(rgPriorities.checkedRadioButtonId)
        val date = selectedDate
        return Task(0, title, description, priority, false, date)
    }

    private fun inputIsValid(): Boolean {
        return if (getText(etTitle).isNullOrEmpty()) {
            inputTitle.error = "You must complete this field"
            false;
        } else {
            true;
        }
    }

    private fun showErrorAlert() {
        KeyboardUtil.hideKeyboard(this)
        showMessage("One or more errors have ocurred")
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

    private fun showMessage(message: String) {
        //TODO: Use a Snackbar component instead of Toast
//        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getText(editText: EditText) = editText.text.toString()
}