package com.example.kelkin

import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.kelkin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navController: NavController by lazy {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController
    }

    private val navItems by lazy {
        listOf(
            binding.containerHome, binding.containerMovies,
            binding.containerTv, binding.containerRadio,
            binding.containerSearch, binding.containerSettings
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupNavigation()
        setupBackPressHandler()
        setupSidebarNavigation()

        binding.containerHome.isSelected = true
        binding.containerHome.requestFocus()
    }

    private fun setupSidebarNavigation() {
        navItems.forEach { container ->
            container.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    // انیمیشن Scale برای دکمه‌ها سبکه و مشکلی نداره
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()

                    // تغییر عرض به صورت آنی
                    updateSidebarState(isExpanded = true)
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()

                    // چک کردن اینکه آیا هنوز فوکوس در سایدبار هست یا نه
                    v.postDelayed({
                        if (!binding.sidebar.hasFocus()) {
                            updateSidebarState(isExpanded = false)
                        }
                    }, 10) // تاخیر رو کم کردیم برای سرعت بیشتر
                }
            }
        }
    }

    private fun updateSidebarState(isExpanded: Boolean) {
        val targetWidthDp = if (isExpanded) 180 else 60
        val targetAlpha = if (isExpanded) 1f else 0f
        val targetTranslationX = if (isExpanded) -80f else 0f // مقدار منفی برای جابجایی به سمت چپ در RTL

        // ۱. تغییر عرض به صورت آنی (بدون انیمیشن سنگین)
        val params = binding.sidebar.layoutParams
        params.width = (targetWidthDp * resources.displayMetrics.density).toInt()
        binding.sidebar.layoutParams = params

        // ۲. انیمیشن آلفا برای متن‌ها (این انیمیشن سبکه چون Layout رو بهم نمیریزه)
        navItems.forEach { container ->
            for (i in 0 until (container as ViewGroup).childCount) {
                val child = container.getChildAt(i)
                if (child is TextView) {
                    child.animate()
                        .alpha(targetAlpha)
                        .translationX(targetTranslationX)
                        .setDuration(100) // زمان خیلی کوتاه برای حس سرعت
                        .withEndAction {
                            child.visibility = if (isExpanded) View.VISIBLE else View.INVISIBLE
                        }
                        .start()
                }
            }
        }
    }



    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val current = currentFocus ?: return super.dispatchKeyEvent(event)
            val isInSidebar = isInsideSidebar(current)

            when (event.keyCode) {
                // مدیریت کلید راست: رفتن به منو
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!isInSidebar) {
                        val nextFocus = FocusFinder.getInstance().findNextFocus(
                            binding.root as ViewGroup,
                            current,
                            View.FOCUS_RIGHT
                        )

                        // اگر سمت راست ویوی دیگری نیست یا ویوی بعدی داخل سایدبار است
                        if (nextFocus == null || nextFocus == current || isInsideSidebar(nextFocus)) {

                            // جادوی اصلی اینجاست:
                            // بگرد بین آیتم‌های منو و اونی که isSelected هست رو پیدا کن
                            val selectedMenuItem = navItems.find { it.isSelected }

                            if (selectedMenuItem != null) {
                                selectedMenuItem.requestFocus() // فوکوس رو بفرست روی همون صفحه فعلی
                            } else {
                                binding.containerHome.requestFocus() // اگه چیزی نبود برو هوم
                            }
                            return true
                        }
                    }
                }

                // مدیریت کلید چپ: قفل کردن لبه لیست (جلوگیری از پریدن به ردیف بالا/پایین)
                KeyEvent.KEYCODE_DPAD_LEFT ->
                {
                    if (!isInSidebar) {
                        val nextFocus = FocusFinder.getInstance().findNextFocus(
                            binding.root as ViewGroup,
                            current,
                            View.FOCUS_LEFT
                        )

                        if (nextFocus == null || nextFocus == current) {
                            return true // قفل کن، نذار جایی بره
                        }

                        // چک کردن تراز عمودی (بسیار مهم برای NestedScrollView)
                        // اگر ویوی بعدی پیدا شده، از نظر ارتفاع خیلی با ویوی فعلی اختلاف داره، یعنی داره می‌پره ردیف دیگه
                        val locationCurrent = IntArray(2)
                        val locationNext = IntArray(2)
                        current.getLocationOnScreen(locationCurrent)
                        nextFocus.getLocationOnScreen(locationNext)

                        val verticalDiff = Math.abs(locationCurrent[1] - locationNext[1])
                        if (verticalDiff > current.height / 2) {
                            // اگر اختلاف ارتفاع زیاد بود، یعنی داره می‌پره به ردیف بالا یا پایین، پس قفلش کن
                            return true
                        }
                    }

                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (isInSidebar) {
                        val nextFocus = FocusFinder.getInstance().findNextFocus(binding.sidebar, current, View.FOCUS_UP)
                        if (nextFocus == null || nextFocus == current) {
                            return true // قفل لبه بالای منو (نمی‌پره روی دکمه پخش)
                        }
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (isInSidebar) {
                        val nextFocus = FocusFinder.getInstance().findNextFocus(binding.sidebar, current, View.FOCUS_DOWN)
                        if (nextFocus == null || nextFocus == current) {
                            return true // قفل لبه پایین منو
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // تابع کمکی برای تشخیص سایدبار
    private fun isInsideSidebar(view: View): Boolean {
        if (view.id == R.id.sidebar || view in navItems) return true
        var parent = view.parent
        while (parent != null && parent is ViewGroup) {
            if (parent.id == R.id.sidebar) return true
            parent = parent.parent
        }
        return false
    }

    private fun setupNavigation() {
        val navDestinations = mapOf(
            R.id.container_home to R.id.homeFragment,
            R.id.container_movies to R.id.moviesFragment,
            R.id.container_tv to R.id.tvFragment,
            R.id.container_radio to R.id.radioFragment,
            R.id.container_settings to R.id.settingsFragment
        )

        navItems.forEach { container ->
            container.setOnClickListener { view ->
                navDestinations[view.id]?.let { destId ->
                    if (navController.currentDestination?.id != destId) {
                        navController.navigate(destId)

                        // مدیریت وضعیت Selected: همه رو غیرفعال و این یکی رو فعال کن
                        navItems.forEach { it.isSelected = false }
                        view.isSelected = true
                    }
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDest = navController.currentDestination?.id

                if (currentDest == R.id.movieDetailFragment) {
                    // ۱. مسدود کردن موقت فوکوس سایدبار قبل از برگشت
                    binding.sidebar.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    navController.popBackStack()

                    // ۲. آزاد کردن سایدبار با تاخیر، تا فوکوس وقت داشته باشه بشینه روی لیست
                    binding.sidebar.postDelayed({
                        binding.sidebar.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                    }, 1000)
                    return
                }

                if (currentDest == R.id.homeFragment) {
                    showExitDialog()
                } else {
                    navController.popBackStack(R.id.homeFragment, false)
                    navItems.forEach { it.isSelected = (it.id == R.id.container_home) }
                    binding.containerHome.requestFocus()
                }
            }
        })
    }

    private fun showExitDialog() {
        Dialog(this).apply {
            setContentView(R.layout.dialog_exit)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            findViewById<Button>(R.id.btnExitNo).setOnClickListener { dismiss() }
            findViewById<Button>(R.id.btnExitYes).setOnClickListener { finishAffinity() }
            show()
        }
    }

}