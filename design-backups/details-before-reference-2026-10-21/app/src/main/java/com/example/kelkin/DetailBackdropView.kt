package com.example.kelkin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.widget.AppCompatImageView

/** Fade both artwork and its scrim into the same page gradient used by Home. */
class DetailBackdropView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val synopsisShade = Paint(Paint.ANTI_ALIAS_FLAG)
    private var synopsis: View? = null
    private val textLocation = IntArray(2)
    private val backdropLocation = IntArray(2)
    private var lastSynopsisTop = Int.MIN_VALUE
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { invalidate() }
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { invalidate() }
    private val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    init { imageAlpha = 102 }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        synopsis = rootView.findViewById(R.id.txtDescription)
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }
    override fun onDetachedFromWindow() {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnScrollChangedListener(scrollListener)
            viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        synopsis = null
        super.onDetachedFromWindow()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        lastSynopsisTop = Int.MIN_VALUE
        shade.shader = LinearGradient(0f, h.toFloat(), 0f, 0f,
            intArrayOf(0xDD0F0F0F.toInt(), 0x88141414.toInt(), 0x00090909),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        val menuGutter = 60 * resources.displayMetrics.density
        val fadeWidth = (240 * resources.displayMetrics.density).coerceAtMost(w * 0.45f)
        val stops = FloatArray(17) { it / 16f }
        val colors = IntArray(stops.size) {
            val t = stops[it]
            Color.argb(((1 - t * t * (3 - 2 * t)) * 255).toInt(), 255, 255, 255)
        }
        val end = (w - menuGutter).coerceAtLeast(w * 0.75f)
        mask.shader = LinearGradient((end - fadeWidth).coerceAtLeast(0f), 0f, end, 0f,
            colors, stops, Shader.TileMode.CLAMP)
    }
    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        synopsis?.let { text ->
            text.getLocationInWindow(textLocation)
            getLocationInWindow(backdropLocation)
            val top = textLocation[1] - backdropLocation[1]
            if (top != lastSynopsisTop) {
                lastSynopsisTop = top
                val density = resources.displayMetrics.density
                // Ease into the shade before the first line, without a visible panel edge.
                val stops = FloatArray(25) { it / 24f }
                val colors = IntArray(stops.size) {
                    val t = stops[it]
                    Color.argb((140 * t * t * (3 - 2 * t)).toInt(), 10, 12, 16)
                }
                synopsisShade.shader = LinearGradient(0f, top - 140 * density,
                    0f, top + 60 * density, colors, stops, Shader.TileMode.CLAMP)
            }
        }
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
        if (synopsis != null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), synopsisShade)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mask)
        canvas.restoreToCount(layer)
    }
}
