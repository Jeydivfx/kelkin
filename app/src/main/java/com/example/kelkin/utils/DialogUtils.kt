package com.example.kelkin.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import com.example.kelkin.R
import com.google.android.material.button.MaterialButton

object DialogUtils {

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        onYesClicked: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_exit) // استفاده از لایوت مشترک
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // پیدا کردن ویوها از همان لایوت dialog_exit
        val tvTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.dialogMessage)
        val btnYes = dialog.findViewById<MaterialButton>(R.id.btnExitYes)
        val btnNo = dialog.findViewById<MaterialButton>(R.id.btnExitNo)

        // ست کردن متن‌های داینامیک
        tvTitle.text = title
        tvMessage.text = message

        // دکمه‌های مشترک
        btnNo.setOnClickListener { dialog.dismiss() }
        btnYes.setOnClickListener {
            onYesClicked()
            dialog.dismiss()
        }

        dialog.show()
    }
}