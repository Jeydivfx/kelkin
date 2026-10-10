package com.example.kelkin.utils

import android.app.Dialog
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.kelkin.R

/** Shared TV-friendly sort picker; selected order and keyboard focus are distinct. */
fun Fragment.showSortPicker(anchor: View, selected: Int, television: Boolean, onSelect: (Int) -> Unit) {
    val context = requireContext()
    val owner = viewLifecycleOwner
    val density = resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()
    val dialog = Dialog(context).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
    val panel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(24), dp(24), dp(24), dp(24))
        setBackgroundResource(R.drawable.bg_dialog)
    }
    fun text(value: String, size: Float, bold: Boolean = false) = TextView(context).apply {
        this.text = value; textSize = size
        setTextColor(ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
        typeface = ResourcesCompat.getFont(context, if (bold) R.font.yekanbakh_bold else R.font.yekanbakh_regular)
        isDuplicateParentStateEnabled = true
    }
    panel.addView(text("مرتب‌سازی", 24f, true))
    panel.addView(text("ترتیب نمایش را انتخاب کنید؛ گزینهٔ فعلی با ✓ مشخص است.", 14f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(24) })
    val labels = if (television) listOf("ترتیب پیش‌فرض", "عنوان (الفبا)") else listOf("تازه اضافه‌شده", "عنوان (الفبا)")
    val descriptions = listOf(if (television) "بر اساس ترتیب ثبت کانال‌ها" else "جدیدترین موارد آرشیو در ابتدا", "از الف تا ی، بر اساس نام فارسی")
    val rows = labels.mapIndexed { index, label ->
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true; isClickable = true
            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            backgroundTintList = null
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(text(if (index == selected) "$label   ✓" else label, 19f, true))
            addView(text(descriptions[index], 13f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            contentDescription = "$label، ${descriptions[index]}" + if (index == selected) "، انتخاب فعلی" else ""
            setOnClickListener { onSelect(index); dialog.dismiss() }
            panel.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
    }
    panel.addView(text("با OK انتخاب کنید؛ با Back به فهرست برگردید.", 12f), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) { dialog.dismiss() }
    }
    owner.lifecycle.addObserver(observer)
    dialog.setOnDismissListener {
        owner.lifecycle.removeObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && anchor.isAttachedToWindow) anchor.requestFocus()
    }
    dialog.setContentView(panel)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.show()
    dialog.window?.setLayout(minOf(dp(460), resources.displayMetrics.widthPixels - dp(64)), -2)
    rows[selected.coerceIn(0, rows.lastIndex)].requestFocus()
}
