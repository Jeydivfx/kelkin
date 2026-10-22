package com.example.kelkin.utils

import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextUtils
import android.widget.TextView
import com.example.kelkin.BuildConfig

internal fun TextView.bindDetailSynopsis(description: String, fullLines: Int = 2) {
    text = description
    if (!BuildConfig.IS_TV) return
    maxLines = fullLines + 1
    val source = description.replace(Regex("\\s+"), " ").trim()
    fun format() {
        val available = width - paddingLeft - paddingRight
        if (available <= 0 || source.isEmpty()) return
        val lines = StaticLayout.Builder.obtain(source, 0, source.length, paint, available)
            .setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build()
        text = if (lines.lineCount <= fullLines) source else {
            val start = lines.getLineStart(fullLines)
            source.substring(0, start).trimEnd() + "\n" + TextUtils.ellipsize(
                source.substring(start), paint, available * 0.55f, TextUtils.TruncateAt.END)
        }
    }
    addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
        if (right - left != oldRight - oldLeft) format()
    }
    post { format() }
}
