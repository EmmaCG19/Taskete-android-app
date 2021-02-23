package com.example.taskete

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskete.data.Task
import com.example.taskete.data.User
import com.example.taskete.db.TasksDAO
import com.example.taskete.db.UsersDAO
import com.example.taskete.helpers.UIManager
import com.example.taskete.preferences.PreferencesActivity
import com.example.taskete.preferences.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
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
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navDrawer: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var iconNoTasks: ImageView
    private lateinit var selectedTasks: MutableList<Task>
    private lateinit var loadingCircle: CircularProgressIndicator
    private lateinit var loadingBar: LinearProgressIndicator
    private lateinit var tasksGroup: Group
    private lateinit var userGroup: Group
    private var userRetrieved: Boolean
    private var showAddFab: Boolean
    private var showCompletedTasks: Boolean
    private var tasks: ArrayList<Task>
    private var actionMode: ActionMode? = null
    private var isMultiSelect: Boolean = false
    private val compositeDisposable = CompositeDisposable()

    private val userId: Int by lazy {
        SessionManager.restoreLoggedUser()
    }

    private val preferences: Single<SharedPreferences> by lazy {
        Single.fromCallable { PreferenceManager.getDefaultSharedPreferences(this) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
    private val tasksDAO: TasksDAO by lazy {
        TasksDAO(this@MainActivity.applicationContext)
    }

    private val usersDAO: UsersDAO by lazy {
        UsersDAO(this@MainActivity.applicationContext)
    }

    private val tasksAdapter: TasksAdapter by lazy {
        TasksAdapter(this)
    }

    private var currentUser: User?

    init {
        tasks = arrayListOf()
        showAddFab = false
        showCompletedTasks = false
        userRetrieved = false

        //Logged user wont change until session logout
        currentUser = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG_ACTIVITY, "Activity status: onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()

    }

    private fun setupUI() {
        fabAddTask = findViewById(R.id.fabAddTask)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.adapter = tasksAdapter
        iconNoTasks = findViewById(R.id.iconNoTasks)

        drawerLayout = findViewById(R.id.drawer_layout_main)
        tasksGroup = findViewById(R.id.tasksGroup)
        userGroup = findViewById(R.id.userGroup)
        loadingBar = findViewById(R.id.loadingBar)
        loadingCircle = findViewById(R.id.loadingCircle)

        fabAddTask.setOnClickListener {
            launchTaskActivity(null)
        }

        setupToolbar()
        setupCAB()
        setupNavbar()

    }

    //Define a flag when we need to get user info
    private fun getLoggedUser() {
        if (userId != SessionManager.DEFAULT_USER_ID) {
            usersDAO.getUser(userId)
                    .subscribe(object : SingleObserver<User?> {
                        override fun onSubscribe(d: Disposable?) {
                            compositeDisposable.add(d)
                        }

                        override fun onSuccess(t: User?) {
                            Log.d(TAG_ACTIVITY, "Showing current user info: ${t?.mail} | ${t?.username}")
                            currentUser = t
                            userRetrieved = true
                            loadUserProfile()
                        }

                        override fun onError(e: Throwable?) {
                            //Return to LoginActivity
                            Log.d(TAG_ACTIVITY, "Error retrieving user: ${e?.message}")
                        }
                    })
        } else {
            // Volver a la pantalla de login
            finish()
        }
    }

    private fun setupNavbar() {
        navDrawer = findViewById(R.id.drawer_layout_main)
        navView = findViewById(R.id.drawer_nav_view)

        val toggle = ActionBarDrawerToggle(this, navDrawer, R.string.nav_open_desc, R.string.nav_close_desc)
        navDrawer.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> showLogoutDialog()
                R.id.nav_settings -> launchSettingsActivity()
            }

            false
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
        supportActionBar?.setHomeAsUpIndicator(null)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    //The only way to reach the MainActivity is by the intent generated by the LoginActivity
    private fun loadUserProfile() {
        val navHeader = navView.getHeaderView(0)
        val txtUsername = navHeader.findViewById<TextView>(R.id.txtUsername)
        val txtUsermail = navHeader.findViewById<TextView>(R.id.txtUserMail)

        txtUsername.text = currentUser?.username
                ?: resources.getString(R.string.nav_header_default_username)
        txtUsermail.text = currentUser?.mail
                ?: resources.getString(R.string.nav_header_default_usermail)

    }

    private fun getUserTasks() {
        tasksDAO.getUserTasks(userId).subscribe(object : SingleObserver<List<Task>> {
            override fun onSubscribe(d: Disposable?) {
                compositeDisposable.add(d)
            }

            override fun onSuccess(t: List<Task>?) {
                Log.d(TAG_ACTIVITY, "Tasks from user $userId could be retrieved")
                tasks = ArrayList(t)
            }

            override fun onError(e: Throwable?) {
                Log.d(TAG_ACTIVITY, "Tasks from user $userId couldn't be retrieved")
                //Show an alert dialog?
            }

        })
    }

    private fun closeDrawer() {
        navDrawer.closeDrawer(GravityCompat.START)
    }

    private fun openDrawer() {
        navDrawer.openDrawer(GravityCompat.START)
    }

    private fun showLogoutDialog() {
        closeDrawer()

        AlertDialog.Builder(this)
                .setTitle(R.string.logoutDialogTitle)
                .setMessage(R.string.logoutDialogDesc)
                .setPositiveButton(R.string.deleteDialogOK) { _, _ ->
                    //Logout OK
                    SessionManager.saveLoggedUser(null)
                    finish()
                }
                .setNegativeButton(R.string.deleteDialogNO) { _, _ ->
                    //Go back to Menu
                }
                .setCancelable(false)
                .show()
    }

    private fun setupCAB() {
        rvTasks.addOnItemTouchListener(
                RecyclerItemClickListener(this, rvTasks, this)
        )
    }

    private fun checkSettings() {
        preferences.subscribe(object : SingleObserver<SharedPreferences> {
            override fun onSubscribe(d: Disposable?) {
                compositeDisposable.add(d)
            }

            override fun onSuccess(t: SharedPreferences) {
                showAddFab = t.getBoolean(PREFERENCE_ADDTASK, true)
                showCompletedTasks = t.getBoolean(PREFERENCE_SHOWCOMPLETE, true)
            }

            override fun onError(e: Throwable?) {
                Log.d(TAG_ACTIVITY, "The general preferences couldn't be retrieved", e)
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
        if (!showCompletedTasks) {
            tasks = getPendingTasks()
        }

        tasksAdapter.updateTasks(tasks)

        if (tasks.isEmpty()) {
            UIManager.hide(rvTasks)
            UIManager.show(iconNoTasks)
        } else {
            UIManager.hide(iconNoTasks)
            UIManager.show(rvTasks)
        }
    }

    private fun getPendingTasks() = tasks.filter { t ->
        !t.isDone
    } as ArrayList<Task>

    override fun onBackPressed() {
        if (navDrawer.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            //Asks user if he wants to leave
            showLogoutDialog()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> openDrawer()
            R.id.searchBarItem -> searchTask()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun searchTask() {
        //https://developer.android.com/guide/topics/search/search-dialog
        //Enable Search widget
        //Define a regex to show results while we are typing the query
        //Search will be filtered by task name
        //If found, load task/s result in the TasksAdapter
        //If not found, show icon of tasks not found
        //Disable search-bar at anytime, Clear search bar and restore adapter
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


    //Si el showCompletedTasks es false, se deben ir ocultando las tareas completadas
    override fun onItemCheck(view: View?, position: Int) {
        if (!showCompletedTasks) {
            getUserTasks()
            Handler(mainLooper).postDelayed({
                handleShowCompletedTasks()
            }, 500)

            Log.d(TAG_ACTIVITY, "El item de la pos ${position} se oculta!")
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
            tasksDAO.deleteTask(t).subscribe()
        }

    }

    override fun resetSelection() {
        checkSettings()
        disableSelection()
    }

    override fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.deleteDialogTitle)
                .setMessage(R.string.deleteDialogDesc)
                .setPositiveButton(R.string.deleteDialogOK) { _, _ ->
                    deleteSelectedTasks()
                    UIManager.showMessage(this, buildDeleteMessage())
                }
                .setNegativeButton(R.string.deleteDialogNO) { _, _ ->
                    //Nothing for now
                }
                .setCancelable(false)
                .show()

        resetSelection()
    }

    private fun buildDeleteMessage(): String {
        val numberOfItems = selectedTasks.size
        return resources.getQuantityString(R.plurals.numberOfDeletedTasks, numberOfItems, numberOfItems)
    }

    //SHOW/HIDE VIEWS methods
    private fun hideAll() {
        UIManager.hide(userGroup)
        UIManager.hide(navView)
        UIManager.hide(loadingBar)
        UIManager.hide(loadingCircle)
    }

    //Loading after logging
    private fun showLoadingScreen() {
        hideAll()
        UIManager.show(loadingCircle)
    }

    private fun hideLoadingScreen() {
        UIManager.show(toolbar)
        UIManager.show(navView)
        UIManager.hide(loadingCircle)
    }

    //Loading after updating tasks/settings
    private fun showTasksLoading() {
        UIManager.hide(tasksGroup)
        UIManager.show(loadingBar)
    }

    private fun hideTasksLoading() {
        loadSettings()
        UIManager.hide(loadingBar)
    }

    private fun loadSettings() {
        handleShowCompletedTasks()
        handleFabAddVisibility()
    }

    private fun loadTasks() {
        showTasksLoading()
        checkSettings()
        getUserTasks()

        Handler(mainLooper).postDelayed({
            hideTasksLoading()
        }, 1000)
    }

    override fun onResume() {
        Log.d(TAG_ACTIVITY, "Activity status: onResume()")

        //Only get user if it was not retrieved or there was a change in its profile

        //1- GET USER ALL
        if (!userRetrieved) {
            showLoadingScreen()
            getLoggedUser()

            Handler(mainLooper).postDelayed({
                hideLoadingScreen()
                loadTasks()
            }, 1000)

        } else {
            //2- GET USER TASKS AND SETTINGS ONLY
            loadTasks()
        }

        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG_ACTIVITY, "Activity status: onPause()")
        closeDrawer()
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG_ACTIVITY, "Activity status: onStop()")
        compositeDisposable.clear()
        super.onStop()
    }

}
//TODO: Refresh user's tasks list first time and when task is checked
//TODO: Fix Tasks selection when deleted
//TODO: Refactor onResume()

//Dummy tasks
//INSERT INTO Tasks(title, description, priority, isDone, dueDate,userId) VALUES
//('Task1', 'This is a task', 'HIGH', FALSE, NULL, 1),
//('Task2', 'This is a task', 'LOW', FALSE, NULL, 1),
//('Task3', 'This is a task', 'LOW', FALSE, NULL, 1),
//('Task4', 'This is a task', 'NOTASSIGNED', FALSE, NULL, 1);

//Helper log
//Log.d(TAG_ACTIVITY, "Activity status: onCreate()")
