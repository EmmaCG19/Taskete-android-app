package com.example.taskete

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
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
    private lateinit var loadingCircle: ProgressBar
    private lateinit var loadingBar: ProgressBar
    private lateinit var taskLayout: Group
    private var showAddFab: Boolean
    private var showCompletedTasks: Boolean
    private var tasks: ArrayList<Task>
    private var actionMode: ActionMode? = null
    private var isMultiSelect: Boolean = false
    private val compositeDisposable = CompositeDisposable()
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

        //Logged user wont change until session logout
        currentUser = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()

    }

    override fun onResume() {
        getLoggedUser()
        resetSelection()
        super.onResume()
    }

    private fun setupUI() {
        fabAddTask = findViewById(R.id.fabAddTask)
        drawerLayout = findViewById(R.id.drawer_layout_main)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.adapter = tasksAdapter
        iconNoTasks = findViewById(R.id.iconNoTasks)
        loadingCircle = findViewById(R.id.loadingCircle)
        loadingBar = findViewById(R.id.loadingBar)
        taskLayout = findViewById(R.id.taskLayout)


        fabAddTask.setOnClickListener {
            launchTaskActivity(null)
        }

        setupToolbar()
        setupCAB()
        setupNavbar()

    }

    private fun getLoggedUser() {
        //Obtener el id del usuario logueado con el Session Manager
        val userId = SessionManager.restoreLoggedUser()

//         val userId= 1;

        //Obtengo al usuario con ID=1 //Hardcoded for testing purposes
        if (userId != SessionManager.DEFAULT_USER_ID) {
            usersDAO.getUser(userId)
                    .subscribe(object : SingleObserver<User?> {
                        override fun onSubscribe(d: Disposable?) {
                            compositeDisposable.add(d)

                            //Show progress bar
                            UIManager.hide(drawerLayout)
                            UIManager.show(loadingCircle)

                        }

                        override fun onSuccess(t: User?) {
                            Log.d(TAG_ACTIVITY, "Showing current user info: ${t?.mail} | ${t?.username}")
                            currentUser = t
                            loadUserProfile()
                        }

                        override fun onError(e: Throwable?) {
                            Log.d(TAG_ACTIVITY, "Error retrieving user: ${e?.message}")
                        }
                    })
        } else {
            //Verificar el id del usuario logueado con la guardada por el sistema
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

            true
        }
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

        //IF FIRST TIME, get tasks from user
        tasks = ArrayList(currentUser?.tasks)

        //Hide progress bar
        UIManager.hide(loadingCircle)
        UIManager.show(drawerLayout)
    }

    private fun closeDrawer() {
        navDrawer.closeDrawer(GravityCompat.START)
    }

    private fun openDrawer() {
        navDrawer.openDrawer(GravityCompat.START)
    }

    private fun showLogoutDialog() {
        Log.d(TAG_ACTIVITY, "Logout selected")
        SessionManager.saveLoggedUser(null)
        finish()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
        supportActionBar?.setHomeAsUpIndicator(null)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupCAB() {
        rvTasks.addOnItemTouchListener(
                RecyclerItemClickListener(this, rvTasks, this)
        )
    }

    private fun checkSettings() {
        preferences.subscribe(object : SingleObserver<SharedPreferences> {
            override fun onSubscribe(d: Disposable?) {
                showProgressBar()
                compositeDisposable.add(d)
            }

            override fun onSuccess(t: SharedPreferences) {
                showAddFab = t.getBoolean(PREFERENCE_ADDTASK, true)
                showCompletedTasks = t.getBoolean(PREFERENCE_SHOWCOMPLETE, true)

                handleShowCompletedTasks()
                hideProgressBar()

                Handler().postDelayed({
                    handleFabAddVisibility()
                }, 1200)
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

        showTasks(tasks)
    }

    private fun getPendingTasks() = tasks.filter { t ->
        !t.isDone
    } as ArrayList<Task>

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

    override fun onBackPressed() {
        if (navDrawer.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            //Asks user if he wants to leave
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> openDrawer()
//            R.id.searchBarItem ->
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
        //Si el showCompletedTasks es false, se deben ir ocultando las tareas completadas


        if (!showCompletedTasks) {
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
        resetSelection()
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

    override fun onPause() {
        closeDrawer()
        super.onPause()
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

}
//TODO: Logout AlertDialog
//TODO: Refresh user's tasks list
//TODO: FabAdd preference must be shown after while progress bar is reloading
