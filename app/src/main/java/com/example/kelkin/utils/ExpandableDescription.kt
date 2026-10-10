package com.example.kelkin.utils

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.kelkin.R

fun TextView.makeDescriptionExpandable(title: String) {
    isFocusable = text.isNotBlank()
    isClickable = isFocusable
    if (!isFocusable) return
    contentDescription = "توضیحات کامل؛ برای خواندن OK را بزنید"
    setOnClickListener {
        AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle(title).setMessage(text).setPositiveButton("بستن", null).show()
    }
}
