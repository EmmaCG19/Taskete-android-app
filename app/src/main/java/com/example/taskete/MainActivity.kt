package com.example.taskete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

const val TASK_SELECTED = "Task_selected"
private const val TAG_ACTIVITY = "MainActivity"


class MainActivity : AppCompatActivity(), RecyclerItemClickListener.OnItemClickListener, TaskSelection {
    private lateinit var rvTasks: RecyclerView
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var toolbar: Toolbar
    private lateinit var fabAddTask: FloatingActionButton
    private val taskAdapter: TaskAdapter by lazy {
        TaskAdapter(this)
    }

    //CAB
    private lateinit var selectedTasks: MutableList<Task>
    private var actionMode: ActionMode? = null
    private var isMultiSelect: Boolean = false

    private val tasks: List<Task> by lazy {
        TasksProvider.getTasks()
    }


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
        rvTasks.adapter = taskAdapter

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
        taskAdapter.updateTasks(tasks)
    }
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
        supportActionBar?.subtitle = "Insert a subtitle"

    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //Settings + Search bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.searchItem -> showMessage("Search icon was pressed")
            //TODO show/hide addBtn
            //TODO show/hide completedTasks
        }

        return super.onOptionsItemSelected(item)
    }

    //Que hacer cuando se hace click en la tarea
    override fun onItemClick(view: View?, position: Int) {
        if (isMultiSelect)
            multiSelect(position)
        else {
            launchTaskActivity(tasks[position])
        }
    }

    //Se hace un longPress click para inicializar la seleccion
    override fun onItemLongClick(view: View?, position: Int) {

        //Se habilita la seleccion de items y se inicializa una lista de tasks
        if (!isMultiSelect) {
            isMultiSelect = true
            selectedTasks = mutableListOf<Task>()

            //Ejecutar el CAB
            if (actionMode == null) {
                actionMode = toolbar.startActionMode(SelectionActionMode(this))
            }
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

            taskAdapter.getSelectedTasks(selectedTasks)

            //Mostrar la cantidad de elementos seleccionados
            actionMode?.title = if (selectedTasks.size > 0) "${selectedTasks.size}" else ""

            //Si la cantidad de elementos seleccionados es 0 o se hace click en el cancel, hay que matar al actionMode y limpiar la lista de elementos seleccionados
            if (selectedTasks.size == 0) {
                disableSelection()
            }
        }
    }

    override fun disableSelection() {
        isMultiSelect = false
        actionMode?.finish()
        actionMode = null
    }

    override fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.deleteDialogTitle) //Utilizar recursos
                .setMessage(R.string.deleteDialogDesc)
                .setPositiveButton(R.string.deleteDialogOK, { _, _ ->
                    deleteSelectedTasks()
                    //TODO: Dar aviso que las tareas fueron eliminadas
                })
                .setNegativeButton(R.string.deleteDialogNO, { _, _ ->
                    disableSelection()
                })
                .setCancelable(false) //No se puede salir del alert dialog antes que selecciones una opcion, no se puede usar back
                .show()
    }

    private fun deleteSelectedTasks() {
        TasksProvider.deleteTasks(selectedTasks)
        retrieveTasks()
    }


}
