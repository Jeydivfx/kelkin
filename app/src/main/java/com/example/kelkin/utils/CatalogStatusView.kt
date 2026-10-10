package com.example.kelkin.utils

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class CatalogStatusView(parent: ViewGroup, retry: () -> Unit) {
    private val panel = LinearLayout(parent.context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(24, 24, 24, 24)
        visibility = View.GONE
    }
    private val message = TextView(parent.context).apply {
        textSize = 18f
        gravity = Gravity.CENTER
        setTextColor(android.graphics.Color.WHITE)
    }
    private val retryButton = Button(parent.context).apply {
        text = "تلاش دوباره"
        backgroundTintList = null
        setBackgroundResource(com.example.kelkin.R.drawable.sl_button_high_contrast)
        setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, com.example.kelkin.R.color.sl_text_high_contrast))
        setOnClickListener { retry() }
    }
    init {
        panel.addView(message)
        panel.addView(retryButton)
        val params = ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        parent.addView(panel, params)
    }
    fun show(empty: Boolean, loading: Boolean, error: String?) {
        panel.visibility = if (empty && !loading) View.VISIBLE else View.GONE
        message.text = error ?: "محتوایی برای این انتخاب پیدا نشد."
        retryButton.visibility = if (error != null) View.VISIBLE else View.GONE
    }
}
