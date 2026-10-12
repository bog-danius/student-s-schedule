package com.example.lab4_ui_components.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.lab4_ui_components.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object Dialogs {
    fun confirmDeleteTask(context: Context, onConfirm: () -> Unit) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Удалить задачу?")
            .setMessage("Вы действительно хотите удалить эту задачу? Отменить это действие будет невозможно.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ -> onConfirm() }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(context.getColor(R.color.red))
    }
}
