package com.example.taskete

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


class TaskAdapter(val listener: RecyclerItemClickListener.OnItemClickListener) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private var tasks: List<Task> = emptyList<Task>()
    private var selectedTasks: List<Task> = emptyList<Task>()
    private var defaultCardBg: Drawable? = null

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
            resetViewHolder(this)
            txtTitle.text = tasks[position].title
            setPriorityColor(this, tasks[position])
            strikeText(this, tasks[position])

            //Fix to checkbox not holding view state
            chkIsDone.apply {
                setOnCheckedChangeListener(null)
                setOnCheckedChangeListener { _, isChecked ->
                    tasks[position].isDone = isChecked
                    strikeText(holder, tasks[position])
                }
                isChecked = tasks[position].isDone
            }

            //Highlight view
            taskCard.setOnClickListener {
                showSelectedTasks()
                if (selectedTasks.contains(tasks[position]))
                    taskCard.background = itemView.resources.getDrawable(R.drawable.bg_list_row, null)
                else
                    taskCard.background = defaultCardBg
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun resetViewHolder(holder: TaskViewHolder) {
        if (defaultCardBg == null) {
            defaultCardBg = holder.taskCard.background
        } else {
            holder.taskCard.background = defaultCardBg
        }
    }

    private fun showSelectedTasks() {
        for (task in selectedTasks) {
            Log.d("SELECTED_TASK", "ID: ${task.id}")
        }
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        selectedTasks = emptyList()
        notifyDataSetChanged()
    }

    fun getSelectedTasks(tasks: List<Task>) {
        selectedTasks = tasks
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
