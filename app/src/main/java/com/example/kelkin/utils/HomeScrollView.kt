package com.example.kelkin.utils

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/** A sidebar width change must not move the user's vertical reading position. */
class HomeScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : NestedScrollView(context, attrs, defStyleAttr) {
    private var previousWidth = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (h != oldh || oldw == 0) super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val preservePosition = previousWidth > 0 && previousWidth != right - left
        val position = scrollY
        super.onLayout(changed, left, top, right, bottom)
        previousWidth = right - left
        if (preservePosition) scrollTo(scrollX, position)
    }
}
