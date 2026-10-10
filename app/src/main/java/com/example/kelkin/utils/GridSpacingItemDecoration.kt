package com.example.kelkin.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, 0)
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val columns = (parent.layoutManager as? androidx.recyclerview.widget.GridLayoutManager)?.spanCount ?: spanCount
        val rawColumn = position % columns
        val column = if (parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) columns - 1 - rawColumn else rawColumn

        if (includeEdge) {
            outRect.left = spacing - column * spacing / columns
            outRect.right = (column + 1) * spacing / columns
            if (position < columns) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / columns
            outRect.right = spacing - (column + 1) * spacing / columns
            if (position >= columns) outRect.top = spacing
        }
    }
}
