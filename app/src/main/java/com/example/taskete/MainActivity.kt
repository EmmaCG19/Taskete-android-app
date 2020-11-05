package com.example.taskete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), TaskListener {
    private lateinit var rvTasks: RecyclerView
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var toolbar: Toolbar
    private lateinit var fabAddTask: FloatingActionButton
    private val taskAdapter: TaskAdapter by lazy{
        TaskAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
    }

    override fun onResume() {
        retrieveTasks()
        super.onResume()
    }

    private fun setupUI() {
        //TODO: Initialize components
        fabAddTask = findViewById(R.id.fabAddTask)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        rvTasks = findViewById(R.id.rvTasks)
        rvTasks.adapter = taskAdapter

        //TODO: Initialize toolbar
        setupToolbar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
          R.id.searchItem -> showMessage("Search icon was pressed")
            //TODO show/hide addBtn
            //TODO show/hide completedTasks
        }

        return super.onOptionsItemSelected(item)
    }

    private fun retrieveTasks() {
        val tasks = TasksProvider.getTasks()
        taskAdapter.updateTasks(tasks)
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getText(R.string.main_screen_title)
        supportActionBar?.subtitle = "Insert a subtitle"
    }

    override fun onTaskClicked(task: Task) {
        showMessage(task.description.toString())
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


}