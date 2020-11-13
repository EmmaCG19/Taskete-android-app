package com.example.taskete

import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem


class SelectionActionMode(
        private val listener: TaskSelection
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.delete_item_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_delete -> {
                listener.showDeleteConfirmationDialog()
                listener.disableSelection()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        listener.disableSelection()
    }
}

interface TaskSelection {
    fun showDeleteConfirmationDialog()
    fun disableSelection()
}
