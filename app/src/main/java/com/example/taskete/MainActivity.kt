package com.example.taskete

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskete.data.Task
import com.example.taskete.db.TasksDAO
import com.example.taskete.helpers.UIManager
import com.example.taskete.preferences.PreferencesActivity
import com.example.taskete.preferences.SessionPreferencesManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

const val TASK_SELECTED = "Task_selected"
private const val TAG_ACTIVITY = "MainActivity"
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
    private lateinit var selectedTasks: MutableList<Task>
    private lateinit var loadingBar: ProgressBar
    private lateinit var taskLayout: Group
    private var showAddFab: Boolean
    private var showCompletedTasks: Boolean
    private var tasks: List<Task>
    private var actionMode: ActionMode? = null
    private var isMultiSelect: Boolean = false
    private val compositeDisposable = CompositeDisposable()
    private val preferences: Single<SharedPreferences> by lazy {
        Single.fromCallable { PreferenceManager.getDefaultSharedPreferences(this) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
        resetSelection()
        super.onResume()
    }

    private fun setupUI() {
        fabAddTask = findViewById(R.id.fabAddTask)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.adapter = tasksAdapter
        iconNoTasks = findViewById(R.id.iconNoTasks)
        loadingBar = findViewById(R.id.loadingBar)
        taskLayout = findViewById(R.id.taskLayout)

        fabAddTask.setOnClickListener {
            launchTaskActivity(null)
        }

        setupToolbar()
        setupCAB()
    }

    private fun checkPreferences() {
        preferences.subscribe(object : SingleObserver<SharedPreferences> {
            override fun onSubscribe(d: Disposable?) {
                showProgressBar()
                compositeDisposable.add(d)
            }

            override fun onSuccess(t: SharedPreferences) {
                showAddFab = t.getBoolean(PREFERENCE_ADDTASK, true)
                showCompletedTasks = t.getBoolean(PREFERENCE_SHOWCOMPLETE, true)

                loadTasks()

                //TODO: FabAdd preference must be shown after while progress bar is reloading
                Handler().postDelayed({
                    handleFabAddVisibility()
                }, 1200)
            }

            override fun onError(e: Throwable?) {
                Log.d(TAG_ACTIVITY, "The general preferences couldn't be retrieved", e)
            }

        })
    }


    private fun loadTasks() {
        dao.getTasks()
                .subscribeOn(Schedulers.io()) //Smart thread allocating
                .observeOn(AndroidSchedulers.mainThread()) //Always observe on the main thread
                .subscribe(object : SingleObserver<List<Task>> {
                    override fun onSubscribe(d: Disposable?) {
                        compositeDisposable.add(d)
                    }

                    override fun onSuccess(listOfTasks: List<Task>) {
                        tasks = listOfTasks
                        hideProgressBar()
                        handleShowCompletedTasks()
                    }

                    override fun onError(e: Throwable) {
                        Log.d(TAG_ACTIVITY, "Error when getting tasks ", e)
                    }

                })
    }


    private fun handleFabAddVisibility() {
        if (showAddFab) {
            UIManager.show(fabAddTask)
        } else {
            UIManager.hide(fabAddTask)
        }
    }


    private fun handleShowCompletedTasks() {
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

        if (tasks.isEmpty()) {
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

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settingsItem -> launchSettingsActivity()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun launchTaskActivity(task: Task?) {
        Intent(this, TaskFormActivity::class.java).apply {
            putExtra(TASK_SELECTED, task)
            startActivity(this)
        }
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

    override fun onItemCheck(view: View?, position: Int) {
        if (!showCompletedTasks) {
            loadTasks()
        }
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

            actionMode?.title = if (selectedTasks.isNotEmpty()) "${selectedTasks.size}" else ""

            if (selectedTasks.isEmpty()) {
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
            dao.deleteTask(t).subscribe()
        }
        resetSelection()
    }

    override fun resetSelection() {
        checkPreferences()
        disableSelection()
    }

    override fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.deleteDialogTitle)
                .setMessage(R.string.deleteDialogDesc)
                .setPositiveButton(R.string.deleteDialogOK) { _, _ ->
                    deleteSelectedTasks()
                    UIManager.showMessage(this, "${selectedTasks.size} tasks were deleted")
                }
                .setNegativeButton(R.string.deleteDialogNO) { _, _ ->
                    resetSelection()
                }
                .setCancelable(false)
                .show()
    }

    private fun showProgressBar() {
        UIManager.show(loadingBar)
        UIManager.hide(taskLayout)
    }

    private fun hideProgressBar() {
        UIManager.hide(loadingBar)
        UIManager.show(taskLayout)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
