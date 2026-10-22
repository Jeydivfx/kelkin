package com.example.kelkin.utils

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.example.kelkin.BuildConfig
import com.example.kelkin.R

/** A soft gold rim contained inside the card, so row clipping cannot cut off a glow. */
private class GoldFocusRim(private val radius: Float, private val density: Float) : android.graphics.drawable.Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
    }
    override fun draw(canvas: android.graphics.Canvas) {
        if (!isVisible) return
        val rect = android.graphics.RectF(bounds)
        if (rect.isEmpty) return
        // Translucent wide layers soften the edge without washing out the image or its blacks.
        for ((width, opacity) in listOf(6f to 22, 4f to 38, 2.5f to 65)) {
            paint.shader = null
            paint.color = android.graphics.Color.argb(opacity, 255, 210, 76)
            paint.strokeWidth = width * density
            val inset = paint.strokeWidth / 2f
            val rim = android.graphics.RectF(rect).apply { inset(inset, inset) }
            canvas.drawRoundRect(rim, (radius - inset).coerceAtLeast(0f), (radius - inset).coerceAtLeast(0f), paint)
        }
        paint.strokeWidth = 1.5f * density
        paint.alpha = 255
        paint.shader = android.graphics.LinearGradient(0f, 0f, rect.width(), rect.height(),
            intArrayOf(0xFFFFE69B.toInt(), 0xFFE8BC48.toInt(), 0xFFFFD66C.toInt()),
            floatArrayOf(0f, 0.55f, 1f), android.graphics.Shader.TileMode.CLAMP)
        val inset = paint.strokeWidth / 2f
        rect.inset(inset, inset)
        canvas.drawRoundRect(rect, (radius - inset).coerceAtLeast(0f), (radius - inset).coerceAtLeast(0f), paint)
    }
    override fun setAlpha(alpha: Int) { /* Layer opacity is defined per stroke. */ }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit
    @Suppress("DEPRECATION")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

private fun posterImage(view: View): ImageView? = listOf(R.id.imgMoviePoster, R.id.imgSeriesPoster,
    R.id.movie_poster, R.id.home_card_image, R.id.imgEpisodeBackdrop, R.id.channelLogo)
    .firstNotNullOfOrNull { view.findViewById<ImageView>(it) }

private fun posterTitle(view: View): TextView? = listOf(R.id.txtMovieTitleGrid, R.id.txtSeriesTitleGrid,
    R.id.home_reference_title, R.id.home_card_title, R.id.txtEpisodeTitle, R.id.txtChannelName)
    .firstNotNullOfOrNull { view.findViewById<TextView>(it) }

private class CardFocus(private val view: View) {
    private val density = view.resources.displayMetrics.density
    private val rim = GoldFocusRim(
        (view as? com.google.android.material.card.MaterialCardView)?.radius ?: 8f * density, density)
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        rim.setBounds(0, 0, view.width, view.height)
    }
    init {
        rim.setVisible(false, false)
        rim.setBounds(0, 0, view.width, view.height)
        view.overlay.add(rim)
        view.addOnLayoutChangeListener(layoutListener)
    }
    private val image = posterImage(view)
    private val title = posterTitle(view)
    private val white = androidx.core.content.ContextCompat.getColor(view.context, R.color.text_white)
    private val yellow = androidx.core.content.ContextCompat.getColor(view.context, R.color.yellow)
    fun apply(value: Float, animateZoom: Boolean = false) {
        rim.setVisible(value > 0f, false)
        rim.invalidateSelf()
        val scale = 1f + 0.10f * value
        view.animate().cancel()
        if (animateZoom) {
            view.animate().scaleX(scale).scaleY(scale).setDuration(110)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)).start()
        } else {
            view.scaleX = scale
            view.scaleY = scale
        }
        view.elevation = 0f
        view.translationZ = 0f
        title?.setTextColor(ColorUtils.blendARGB(white, yellow, value))
        image?.let {
            if (value == 0f) it.clearColorFilter()
            else {
                val b = 1f + 0.12f * value
                // Brighten the artwork without lifting black pixels into a pale gray strip.
                val lift = 0f
                it.colorFilter = android.graphics.ColorMatrixColorFilter(floatArrayOf(
                    b, 0f, 0f, 0f, lift, 0f, b, 0f, 0f, lift,
                    0f, 0f, b, 0f, lift, 0f, 0f, 0f, 1f, 0f))
            }
        }
    }
    fun focus(focused: Boolean) {
        // Color responds immediately; only the short zoom eases into place.
        apply(if (focused) 1f else 0f, animateZoom = true)
    }
    fun clear() {
        apply(0f)
        view.overlay.remove(rim)
        view.removeOnLayoutChangeListener(layoutListener)
    }
}

internal fun resetPosterFocus(view: View) {
    (view.getTag(R.id.tv_media_focus_controller) as? CardFocus)?.clear()
    view.setTag(R.id.tv_media_focus_controller, null)
    view.onFocusChangeListener = null
    view.animate().cancel()
    view.scaleX = 1f; view.scaleY = 1f
    view.elevation = 0f; view.translationZ = 0f
    posterImage(view)?.clearColorFilter()
}

internal fun bindPosterFocus(view: View) {
    if (!BuildConfig.IS_TV) { view.isFocusable = false; return }
    resetPosterFocus(view)
    view.stateListAnimator = null
    if (android.os.Build.VERSION.SDK_INT >= 26) {
        // The system focus overlay would otherwise sit on top of our custom image treatment.
        view.setDefaultFocusHighlightEnabled(false)
    }
    if (view is com.google.android.material.card.MaterialCardView) {
        view.strokeWidth = 0
        view.setCardForegroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
        view.rippleColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
    }
    if (android.os.Build.VERSION.SDK_INT >= 28) {
        view.outlineAmbientShadowColor = android.graphics.Color.TRANSPARENT
        view.outlineSpotShadowColor = android.graphics.Color.TRANSPARENT
    }
    val effect = CardFocus(view)
    view.setTag(R.id.tv_media_focus_controller, effect)
    effect.apply(if (view.hasFocus()) 1f else 0f)
    view.setOnFocusChangeListener { _, focused -> effect.focus(focused) }
}
