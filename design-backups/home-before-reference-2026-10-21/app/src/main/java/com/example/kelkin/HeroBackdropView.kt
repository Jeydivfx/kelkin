package com.example.kelkin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/** Fade artwork to transparency so the actual page gradient remains visible. */
class HeroBackdropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val topCropMatrix = android.graphics.Matrix()
    private val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val rightMask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val readingShade = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val fadeStart = (h - 280 * resources.displayMetrics.density).coerceAtLeast(h * 0.30f)
        mask.shader = if (BuildConfig.IS_TV) {
            // Sample a smooth curve so the artwork has no abrupt fade boundary.
            val stops = FloatArray(17) { it / 16f }
            val colors = IntArray(stops.size) { index ->
                val t = stops[index]
                val opacity = 1f - t * t * (3f - 2f * t)
                Color.argb((opacity * 255).toInt(), 255, 255, 255)
            }
            LinearGradient(0f, h * 0.36f, 0f, h.toFloat(), colors, stops, Shader.TileMode.CLAMP)
        } else LinearGradient(0f, fadeStart, 0f, h.toFloat(),
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val rightFadeWidth = (180 * resources.displayMetrics.density).coerceAtMost(w * 0.35f)
        rightMask.shader = LinearGradient(w - rightFadeWidth, 0f, w.toFloat(), 0f,
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        shade.shader = LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(0x22000000, 0x44000000, 0xCC000000.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        if (BuildConfig.IS_TV) {
            // A broad, feathered scrim behind the RTL text, without a panel boundary.
            readingShade.shader = android.graphics.RadialGradient(0f, 0f, 1f,
                intArrayOf(0x580D1118, 0x450D1118, 0x200D1118, 0x000D1118),
                floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP).apply {
                setLocalMatrix(android.graphics.Matrix().apply {
                    setScale(w * 0.5f, h * 0.42f)
                    postTranslate(w * 0.78f, h * 0.43f)
                })
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        drawable?.let { image ->
            if (image.intrinsicWidth > 0 && image.intrinsicHeight > 0) {
                val scale = maxOf(width.toFloat() / image.intrinsicWidth, height.toFloat() / image.intrinsicHeight)
                topCropMatrix.reset()
                topCropMatrix.setScale(scale, scale)
                topCropMatrix.postTranslate((width - image.intrinsicWidth * scale) / 2f, 0f)
                imageMatrix = topCropMatrix
            }
        }
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
        if (BuildConfig.IS_TV) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), readingShade)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mask)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), rightMask)
        canvas.restoreToCount(layer)
    }
}
