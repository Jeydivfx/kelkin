package com.example.kelkin.utils

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView

/** Center a newly entered TV shelf after the normal focus scrolling has finished. */
internal class HomeRowFocusScroll(
    private val scroll: NestedScrollView,
    private val rows: List<RecyclerView>
) {
    private var activeRow: RecyclerView? = null
    private val revealCard = Runnable {
        val focus = scroll.findFocus() ?: return@Runnable
        val row = rows.firstOrNull { it.findContainingItemView(focus) != null } ?: return@Runnable
        val card = row.findContainingItemView(focus) ?: return@Runnable
        if (row.isComputingLayout) { scheduleReveal(); return@Runnable }
        val bounds = Rect()
        card.getDrawingRect(bounds)
        row.offsetDescendantRectToMyCoords(card, bounds)
        // Include the full 4% focus scale and yellow stroke, not just the unscaled layout box.
        val extra = kotlin.math.ceil(card.width * 0.02f).toInt() + 2
        bounds.inset(-extra, 0)
        val left = row.paddingLeft
        val right = row.width - row.paddingRight
        val dx = when {
            bounds.left < left -> bounds.left - left
            bounds.right > right -> bounds.right - right
            else -> 0
        }
        if (dx != 0) row.smoothScrollBy(dx, 0)
    }
    private fun scheduleReveal() {
        scroll.removeCallbacks(revealCard)
        scroll.post(revealCard)
    }
    private val rowLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> scheduleReveal() }
    private val centerRow = Runnable {
        val row = activeRow ?: return@Runnable
        val focus = scroll.findFocus() ?: return@Runnable
        if (row.findContainingItemView(focus) == null || row.height == 0) return@Runnable
        val bounds = Rect()
        row.getDrawingRect(bounds)
        scroll.offsetDescendantRectToMyCoords(row, bounds)
        val viewport = scroll.height - scroll.paddingTop - scroll.paddingBottom
        val content = scroll.getChildAt(0) ?: return@Runnable
        val maximum = (content.height - viewport).coerceAtLeast(0)
        val target = ((bounds.top + bounds.bottom - viewport) / 2 - scroll.paddingTop)
            .coerceIn(0, maximum)
        scroll.smoothScrollTo(0, target)
    }
    private val listener = ViewTreeObserver.OnGlobalFocusChangeListener { _, focused ->
        val row = focused?.let { view -> rows.firstOrNull { it.findContainingItemView(view) != null } }
        if (row !== activeRow) {
            scroll.removeCallbacks(centerRow)
            activeRow = row
            if (row != null) scroll.post(centerRow)
        }
        if (row != null) scheduleReveal()
    }

    init {
        scroll.viewTreeObserver.addOnGlobalFocusChangeListener(listener)
        rows.forEach { it.addOnLayoutChangeListener(rowLayoutListener) }
    }

    fun dispose() {
        scroll.removeCallbacks(centerRow)
        scroll.removeCallbacks(revealCard)
        rows.forEach { it.removeOnLayoutChangeListener(rowLayoutListener) }
        if (scroll.viewTreeObserver.isAlive) {
            scroll.viewTreeObserver.removeOnGlobalFocusChangeListener(listener)
        }
        activeRow = null
    }
}
