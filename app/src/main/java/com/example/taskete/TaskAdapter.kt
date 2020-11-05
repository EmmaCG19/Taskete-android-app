package com.example.taskete

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView


class TaskAdapter(val listener: TaskListener) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    var tasks: List<Task> = emptyList<Task>()

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskCard: CardView = itemView.findViewById(R.id.taskCardView)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val chkIsDone: CheckBox = itemView.findViewById(R.id.chkIsDone)
        val priorityIcon: ImageView = itemView.findViewById(R.id.priorityIcon)
        //TODO falta agregar un icono para indicar la prioridad de la task
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_card, parent, false)

        return TaskViewHolder(itemView)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.apply {
            txtTitle.text = tasks[position].title
            chkIsDone.isChecked = tasks[position].isDone
            setPriorityColor(this, tasks[position])
            strikeText(this, tasks[position])

            chkIsDone.setOnCheckedChangeListener { _, isChecked ->
                tasks[position].isDone = isChecked
                strikeText(this, tasks[position])
            }

        }

    }

    private fun setPriorityColor(holder: TaskViewHolder, task: Task) {
        var priorityIcon = holder.priorityIcon

        when(task.priority){
//         Priority.LOW ->  priorityIcon.colorFilter =
        }

    }

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
    }

    private fun strikeText(holder: TaskViewHolder, task: Task) {

        var text = holder.txtTitle

        if (task.isDone) {
            text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            text.setTextColor(holder.itemView.resources.getColor(R.color.colorTextDisabled))
        } else {
            text.paintFlags = text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            text.setTextColor(holder.itemView.resources.getColor(R.color.colorTextEnabled))
        }
    }


}

interface TaskListener {
    fun onTaskClicked(task: Task)
}