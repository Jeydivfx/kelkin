package com.example.kelkin.utils

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Size columns from the content area, after the TV sidebar and page padding. */
fun RecyclerView.configureCatalogGrid() {
    val density = resources.displayMetrics.density
    val manager = GridLayoutManager(context, if (com.example.kelkin.BuildConfig.IS_TV) 5 else catalogColumns(resources.configuration.screenWidthDp - 32, 1f))
    layoutManager = manager
    itemAnimator = null
    val tv = com.example.kelkin.BuildConfig.IS_TV
    addItemDecoration(GridSpacingItemDecoration(manager.spanCount, ((if (tv) 16 else 12) * density).toInt(), tv))
    addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
        if (right - left != oldRight - oldLeft) {
            val available = right - left - paddingLeft - paddingRight
            val columns = catalogColumns(available, density)
            if (manager.spanCount != columns) {
                manager.spanCount = columns
                invalidateItemDecorations()
            }
        }
    }
}

internal fun catalogColumns(available: Int, density: Float): Int =
    (available / (152 * density)).toInt().coerceIn(2, 6)
