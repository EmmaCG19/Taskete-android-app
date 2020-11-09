package com.example.taskete

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_task_form.view.*
import kotlinx.android.synthetic.main.item_card.view.*
import kotlinx.android.synthetic.main.item_card.view.priorityIcon


class TaskAdapter(val listener: TaskListener) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    var tasks: List<Task> = emptyList<Task>()

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskCard: CardView = itemView.findViewById(R.id.taskCardView)
        val txtTitle: TextView = itemView.findViewById(R.id.etTitle)
        val chkIsDone: CheckBox = itemView.findViewById(R.id.chkIsDone)
        val imgPriority: ImageView = itemView.findViewById(R.id.priorityIcon)
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
            setPriorityColor(this, tasks[position])
            strikeText(this, tasks[position])

            //Fix to checkbox not holding view state
            chkIsDone.setOnCheckedChangeListener(null)

            chkIsDone.isChecked = tasks[position].isDone

            chkIsDone.setOnCheckedChangeListener { _, isChecked ->
                tasks[position].isDone = isChecked
                strikeText(this, tasks[position])
            }

            taskCard.setOnClickListener {
                listener.onTaskClicked(tasks[position])
            }
        }


    }

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
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

    private fun setPriorityColor(holder: TaskViewHolder, task: Task) {
        val context = holder.itemView.context
        val icon = holder.imgPriority

        val highColor = ContextCompat.getColor(context, R.color.colorPriorityHigh);
        val mediumColor = ContextCompat.getColor(context, R.color.colorPriorityMedium);
        val lowColor = ContextCompat.getColor(context, R.color.colorPriorityLow);
        val noColor = ContextCompat.getColor(context, R.color.colorPriorityNotAssigned);

        when (task.priority) {
            Priority.LOW -> icon.setColorFilter(lowColor, PorterDuff.Mode.SRC_IN)
            Priority.MEDIUM -> icon.setColorFilter(mediumColor, PorterDuff.Mode.SRC_IN)
            Priority.HIGH -> icon.setColorFilter(highColor, PorterDuff.Mode.SRC_IN)
            else -> icon.setColorFilter(noColor, PorterDuff.Mode.SRC_IN)

        }
    }
}

interface TaskListener {
    fun onTaskClicked(task: Task)
}