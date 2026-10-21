package com.example.kelkin.utils

import android.view.View
import android.widget.ImageView
import com.example.kelkin.BuildConfig
import com.example.kelkin.R

private fun posterImage(view: View): ImageView? = view.findViewById<ImageView>(R.id.imgMoviePoster)
    ?: view.findViewById(R.id.imgSeriesPoster)

internal fun resetPosterFocus(view: View) {
    view.animate().cancel()
    view.scaleX = 1f; view.scaleY = 1f
    view.elevation = 0f; view.translationZ = 0f
    posterImage(view)?.clearColorFilter()
}

internal fun bindPosterFocus(view: View) {
    view.setOnFocusChangeListener(null)
    resetPosterFocus(view)
    if (!BuildConfig.IS_TV) { view.isFocusable = false; return }
    fun applyFocus(focused: Boolean, animate: Boolean) {
        view.animate().cancel()
        val scale = if (focused) 1.04f else 1f
        if (animate) view.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
        else { view.scaleX = scale; view.scaleY = scale }
        view.translationZ = 0f
        posterImage(view)?.let { image ->
            if (!focused) image.clearColorFilter()
            else {
                val b = 1.08f
                image.colorFilter = android.graphics.ColorMatrixColorFilter(floatArrayOf(
                    b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
            }
        }
    }
    applyFocus(view.hasFocus(), false)
    view.setOnFocusChangeListener { _, focused -> applyFocus(focused, true) }
}
