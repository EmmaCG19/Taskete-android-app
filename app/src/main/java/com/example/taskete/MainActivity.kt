package com.example.taskete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.taskete.data.Task
import com.example.taskete.db.TasksDAO
import com.example.taskete.helpers.UIManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

const val TASK_SELECTED = "Task_selected"
private const val TAG_ACTIVITY = "MainActivity"


class MainActivity : AppCompatActivity(), RecyclerItemClickListener.OnItemClickListener, TaskSelection {
    private lateinit var rvTasks: RecyclerView
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var toolbar: Toolbar
    private lateinit var fabAddTask: FloatingActionButton
    private var tasks: List<Task>
    private val dao: TasksDAO by lazy {
        TasksDAO(this@MainActivity.applicationContext)
    }

    private val tasksAdapter: TasksAdapter by lazy {
        TasksAdapter(this)
    }

    init {
        tasks = emptyList()
    }

    //CAB
    private lateinit var selectedTasks: MutableList<Task>
    private var actionMode: ActionMode? = null
    private var isMultiSelect: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
    }

    override fun onResume() {
        disableSelection()
        retrieveTasks()
        super.onResume()
    }

    private fun setupUI() {
        fabAddTask = findViewById(R.id.fabAddTask)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.adapter = tasksAdapter

        fabAddTask.setOnClickListener {
            launchTaskActivity(null)
        }

        setupToolbar()
        setupCAB()
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

    private fun retrieveTasks() {
        Log.d(TAG_ACTIVITY, "Remaining tasks: ${tasks.size}")
        tasks = dao.getTasks()
        tasksAdapter.updateTasks(tasks)
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
        supportActionBar?.subtitle = "Insert a subtitle"

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //Settings + Search bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settingsItem -> UIManager.showMessage(this, "Settings icon was pressed")
            R.id.searchItem -> {

            }

        }

        return super.onOptionsItemSelected(item)
    }

    private fun deleteSelectedTasks() {
        selectedTasks.forEach { t ->
            dao.deleteTask(t)
        }
        retrieveTasks()
    }

    //Que hacer cuando se hace click en la tarea
    override fun onItemClick(view: View?, position: Int) {
        if (isMultiSelect) {
            multiSelect(position)
        } else {
            launchTaskActivity(tasks[position])
        }

    }

    //Se hace un longPress click para inicializar la seleccion
    override fun onItemLongClick(view: View?, position: Int) {

        //Se habilita la seleccion de items y se inicializa una lista de tasks
        if (!isMultiSelect) {
            enableSelection()
        }

        //Guardar la posicion seleccionada en la nueva lista
        multiSelect(position)
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

            //Mostrar la cantidad de elementos seleccionados
            actionMode?.title = if (selectedTasks.size > 0) "${selectedTasks.size}" else ""

            //Si la cantidad de elementos seleccionados es 0 o se hace click en el cancel, hay que matar al actionMode y limpiar la lista de elementos seleccionados
            if (selectedTasks.size == 0) {
                disableSelection()

            }
        }
    }

    override fun enableSelection() {
        isMultiSelect = true
        selectedTasks = mutableListOf<Task>()
        actionMode = actionMode ?: toolbar.startActionMode(SelectionActionMode(this))
        UIManager.hideWidget(fabAddTask)
    }

    override fun disableSelection() {
        isMultiSelect = false
        actionMode?.finish()
        actionMode = null
        UIManager.showWidget(fabAddTask)
    }

    override fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.deleteDialogTitle) //Utilizar recursos
                .setMessage(R.string.deleteDialogDesc)
                .setPositiveButton(R.string.deleteDialogOK, { _, _ ->
                    deleteSelectedTasks()
                    //TODO: Dar aviso que las tareas fueron eliminadas
                })
                .setNegativeButton(R.string.deleteDialogNO, { _, _ ->
                    onResume()
                })
                .setCancelable(false) //No se puede salir del alert dialog antes que selecciones una opcion, no se puede usar back
                .show()
    }


}
