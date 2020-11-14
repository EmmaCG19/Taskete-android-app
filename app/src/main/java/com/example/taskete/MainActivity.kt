package com.example.taskete

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskete.data.Task
import com.example.taskete.db.TasksDAO
import com.example.taskete.helpers.UIManager
import com.example.taskete.preferences.PreferencesActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

const val TASK_SELECTED = "Task_selected"
private const val PREFERENCE_ADDTASK = "swShowAddBtn"
private const val PREFERENCE_SHOWCOMPLETE = "swPrefShowCompletedTasks"

class MainActivity :
        AppCompatActivity(),
        RecyclerItemClickListener.OnItemClickListener,
        TaskSelection {
    private lateinit var rvTasks: RecyclerView
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var toolbar: Toolbar
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var iconNoTasks: ImageView
    private var showAddFab: Boolean
    private var showCompletedTasks: Boolean
    private var tasks: List<Task>
    private lateinit var selectedTasks: MutableList<Task>
    private var actionMode: ActionMode? = null
    private var isMultiSelect: Boolean = false
    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val dao: TasksDAO by lazy {
        TasksDAO(this@MainActivity.applicationContext)
    }

    private val tasksAdapter: TasksAdapter by lazy {
        TasksAdapter(this)
    }

    init {
        tasks = emptyList()
        showAddFab = false
        showCompletedTasks = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
    }

    override fun onResume() {
        checkPreferences()
        disableSelection()
        super.onResume()
    }

    private fun setupUI() {
        fabAddTask = findViewById(R.id.fabAddTask)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.adapter = tasksAdapter
        iconNoTasks = findViewById(R.id.iconNoTasks)

        fabAddTask.setOnClickListener {
            launchTaskActivity(null)
        }

        setupToolbar()
        setupCAB()
    }

    private fun checkPreferences() {
        handleFabAddVisibility()
        handleShowCompletedTasks()
    }

    private fun handleFabAddVisibility() {
        showAddFab = preferences.getBoolean(PREFERENCE_ADDTASK, true)

        if (showAddFab) {
            UIManager.show(fabAddTask)
        } else {
            UIManager.hide(fabAddTask)
        }
    }

    private fun handleShowCompletedTasks() {
        showCompletedTasks = preferences.getBoolean(PREFERENCE_SHOWCOMPLETE, true)
        tasks = dao.getTasks()

        if (showCompletedTasks) {
            showAllTasks()
        } else {
            showPendingTasks()
        }

    }

    private fun showPendingTasks() {
        tasks = tasks.filter { t ->
            !t.isDone
        }
        showTasks(tasks)
    }

    private fun showAllTasks() {
        showTasks(tasks)
    }

    private fun showTasks(tasks: List<Task>) {
        tasksAdapter.updateTasks(tasks)

        if (tasks.size == 0) {
            UIManager.hide(rvTasks)
            UIManager.show(iconNoTasks)
        } else {
            UIManager.hide(iconNoTasks)
            UIManager.show(rvTasks)
        }
    }

    private fun setupCAB() {
        rvTasks.addOnItemTouchListener(
                RecyclerItemClickListener(this, rvTasks, this)
        )
    }

    private fun launchTaskActivity(task: Task?) {
        Intent(this, TaskFormActivity::class.java).apply {
            putExtra(TASK_SELECTED, task)
            startActivity(this)
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //Settings + Search bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settingsItem -> launchSettingsActivity()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun launchSettingsActivity() {
        Intent(this, PreferencesActivity::class.java).apply {
            startActivity(this)
        }
    }

    override fun onItemTouch(view: View?, position: Int) {

        if (isMultiSelect) {
            multiSelect(position)
        }
    }

    override fun onItemLongPress(view: View?, position: Int) {
        if (!isMultiSelect) {
            enableSelection()
        }

        multiSelect(position)
    }

    override fun onItemDoubleTouch(view: View?, position: Int) {
        launchTaskActivity(tasks[position])
    }

    private fun multiSelect(position: Int) {
        if (actionMode != null) {
            val taskSelected = tasks[position]

            if (selectedTasks.contains(taskSelected)) {
                selectedTasks.remove(taskSelected)
            } else {
                selectedTasks.add(taskSelected)
            }

            tasksAdapter.getSelectedTasks(selectedTasks)

            actionMode?.title = if (selectedTasks.size > 0) "${selectedTasks.size}" else ""

            if (selectedTasks.size == 0) {
                disableSelection()

            }
        }
    }

    private fun enableSelection() {
        isMultiSelect = true
        selectedTasks = mutableListOf<Task>()
        actionMode = actionMode ?: toolbar.startActionMode(TaskSelectionMode(this))

        if (showAddFab)
            UIManager.hide(fabAddTask)
    }

    private fun disableSelection() {
        isMultiSelect = false
        actionMode?.finish()
        actionMode = null

        if (showAddFab)
            UIManager.show(fabAddTask)
    }

    private fun deleteSelectedTasks() {
        selectedTasks.forEach { t ->
            dao.deleteTask(t)
        }
        resetSelection()
    }

    override fun resetSelection() {
        onResume()
    }

    override fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.deleteDialogTitle)
                .setMessage(R.string.deleteDialogDesc)
                .setPositiveButton(R.string.deleteDialogOK, { _, _ ->
                    deleteSelectedTasks()
                    UIManager.showMessage(this, "${selectedTasks.size} tasks were deleted")
                })
                .setNegativeButton(R.string.deleteDialogNO, { _, _ ->
                    resetSelection()
                })
                .setCancelable(false)
                .show()
    }


}
