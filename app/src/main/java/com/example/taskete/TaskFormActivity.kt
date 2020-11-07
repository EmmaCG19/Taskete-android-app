package com.example.taskete

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

private const val TAG_ACTIVITY = "TaskFormActivity"

class TaskFormActivity : AppCompatActivity() {
    private lateinit var inputTitle: TextInputLayout
    private lateinit var inputDesc: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var rgPriorities: RadioGroup
    private lateinit var btnSave: ImageButton
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

        updateFields()

        btnSave.setOnClickListener {
            if (taskRetrieved != null) {
                //TODO: Si el intent del MainActivity no es nulo, se guarda la task
                editTask()
            } else {
                //TODO: Si el intent del MainActivity es nulo, se crea una task
                createTask()
            }
        }

    }

    private fun updateFields() {
        etTitle.setText(taskRetrieved?.title)
        etDesc.setText(taskRetrieved?.description)
        rgPriorities.check(updatePriority(taskRetrieved?.priority?: Priority.NOTASSIGNED))
    }

    //TODO: Update task info
    private fun editTask() {
        Log.d(TAG_ACTIVITY, "La tarea se va a editar")

        if (inputIsValid()) {
            taskRetrieved?.title = getText(etTitle)
            taskRetrieved?.description = getText(etDesc)
            taskRetrieved?.priority = getPriority(rgPriorities.checkedRadioButtonId)
            TasksProvider.editTask(taskRetrieved!!)

            finish()
        } else {
            showMessage("One or more errors have ocurred")
        }
    }

    //TODO: Generate a new task
    private fun createTask() {
        Log.d(TAG_ACTIVITY, "La tarea se va a crear")

        if (inputIsValid()) {
            val newTask = generateTask()
            TasksProvider.addTask(newTask)
            finish()
        } else {
            showMessage("One or more errors have ocurred")
        }
    }

    private fun generateTask(): Task {
        val title = getText(etTitle)
        val priority = getPriority(rgPriorities.checkedRadioButtonId)
        val description = getText(etDesc)
        return Task(0, title, description, priority, false)
    }

    //TODO: Validate input
    private fun inputIsValid(): Boolean {
        return if (getText(etTitle).isNullOrEmpty()) {
            inputTitle.error = "You must complete this field"
            false;
        } else {
            true;
        }

    }

    private fun getPriority(checked: Int): Priority {
        return when (checked) {
            R.id.rbHighPriority -> Priority.HIGH
            R.id.rbMediumPriority -> Priority.MEDIUM
            R.id.rbLowPriority -> Priority.LOW
            else -> Priority.NOTASSIGNED
        }
    }

    private fun updatePriority(priority: Priority): Int {
        return when (priority) {
            Priority.HIGH -> R.id.rbHighPriority
            Priority.MEDIUM -> R.id.rbMediumPriority
            Priority.LOW -> R.id.rbLowPriority
            else -> R.id.rbNotPriority
        }

    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getText(editText: EditText) = editText.text.toString()
}