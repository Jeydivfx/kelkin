package com.example.kelkin

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat

class VirtualMouseView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val pointer: ImageView
    private val speed = 30f // سرعت حرکت موس
    private val currentPos = PointF(640f, 360f)

    // تیک‌ها و تایمر برای مخفی‌سازی خودکار
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        // غیب کردن موس با انیمیشن نرم Fade-out
        pointer.animate().alpha(0f).setDuration(300).start()
    }
    private val hideDelay = 5000L // مدت زمان ماندگاری (۵ ثانیه)

    init {
        pointer = ImageView(context)
        val pointerDrawable = ContextCompat.getDrawable(context, R.drawable.virtual_mouse_pointer)
        pointer.setImageDrawable(pointerDrawable)

        val sizeInPx = (24 * resources.displayMetrics.density).toInt()
        val params = LayoutParams(sizeInPx, sizeInPx)
        pointer.layoutParams = params

        pointer.translationX = currentPos.x
        pointer.translationY = currentPos.y

        addView(pointer)

        isFocusable = true
        isFocusableInTouchMode = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS

        // شروع تایمر اولیه به محض ساخته شدن لایه
        resetHideTimer()
    }

    // تابع برای ظاهر کردن موس و ریست کردن تایمر ۵ ثانیه‌ای
    private fun resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)

        // اگر موس غیب یا نیمه‌غیب شده، فوراً بدون تاخیر ظاهرش کن (Fade-in)
        if (pointer.alpha < 1f) {
            pointer.animate().alpha(1f).setDuration(100).start()
        }

        // برنامه‌ریزی برای مخفی شدن پس از ۵ ثانیه عدم فعالیت
        hideHandler.postDelayed(hideRunnable, hideDelay)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val activity = context as? PlayerActivity
            if (activity?.isVkVideo == false) return super.dispatchKeyEvent(event)

            // با فشردن هر کلیدی، اول موس را ظاهر کن و تایمر را تمدید کن
            resetHideTimer()

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                    moveRelative(-speed, 0f)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                    moveRelative(speed, 0f)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                    moveRelative(0f, -speed)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                    moveRelative(0f, speed)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                    activity?.let { click(it.binding.vkWebView) }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun moveRelative(dx: Float, dy: Float) {
        val newX = currentPos.x + dx
        val newY = currentPos.y + dy

        val maxX = width.toFloat() - pointer.width
        val maxY = height.toFloat() - pointer.height

        currentPos.x = newX.coerceIn(0f, maxX)
        currentPos.y = newY.coerceIn(0f, maxY)

        pointer.translationX = currentPos.x
        pointer.translationY = currentPos.y
    }

    fun click(targetWebView: WebView) {
        val location = IntArray(2)
        pointer.getLocationOnScreen(location)
        val clickX = location[0].toFloat() + (pointer.width / 2)
        val clickY = location[1].toFloat() + (pointer.height / 2)
        simulateTouch(clickX, clickY, targetWebView)
    }

    private fun simulateTouch(x: Float, y: Float, targetWebView: WebView) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, x, y, 0)

        targetWebView.dispatchTouchEvent(downEvent)
        targetWebView.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
    }

    // لغو تایمر در صورت جدا شدن لایه از صفحه (برای جلوگیری از Memory Leak)
    override fun onDetachedFromWindow() {
        hideHandler.removeCallbacks(hideRunnable)
        super.onDetachedFromWindow()
    }
}