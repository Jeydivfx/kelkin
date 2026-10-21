package com.example.kelkin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/** Reference detail experiment: broad RTL reading scrim, clear artwork on the left. */
class DetailBackdropView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sideShade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    init { imageAlpha = 255 }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        shade.shader = LinearGradient(0f, h.toFloat(), 0f, 0f,
            intArrayOf(0xF2101218.toInt(), 0x66101218, 0x00101218),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        val shadeStops = FloatArray(25) { it / 24f }
        val shadeColors = IntArray(shadeStops.size) {
            val t = shadeStops[it]
            val eased = t * t * (3 - 2 * t)
            Color.argb((242 * eased).toInt(), 16, 18, 24)
        }
        sideShade.shader = LinearGradient(w * 0.08f, 0f, w * 0.88f, 0f,
            shadeColors, shadeStops, Shader.TileMode.CLAMP)
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
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sideShade)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mask)
        canvas.restoreToCount(layer)
    }
}
