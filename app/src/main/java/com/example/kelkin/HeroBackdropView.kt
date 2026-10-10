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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val fadeStart = (h - 280 * resources.displayMetrics.density).coerceAtLeast(h * 0.30f)
        mask.shader = LinearGradient(0f, fadeStart, 0f, h.toFloat(),
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val rightFadeWidth = (180 * resources.displayMetrics.density).coerceAtMost(w * 0.35f)
        rightMask.shader = LinearGradient(w - rightFadeWidth, 0f, w.toFloat(), 0f,
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        shade.shader = LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(0x22000000, 0x44000000, 0xCC000000.toInt()),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mask)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), rightMask)
        canvas.restoreToCount(layer)
    }
}
