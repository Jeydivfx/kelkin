package com.example.kelkin

import com.example.kelkin.utils.openMenuDestination
import androidx.lifecycle.lifecycleScope
import com.example.kelkin.utils.UserManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.kelkin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isSidebarExpanded = mutableStateOf(false)
    private var sidebarNavigationPending = false

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (BuildConfig.IS_TV && ::binding.isInitialized &&
            event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && binding.composeSidebar.hasFocus()) {
            val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            val home = host?.childFragmentManager?.primaryNavigationFragment as? com.example.kelkin.Fragments.HomeFragment
            if (home != null) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    collapseSidebar()
                    home.restoreFocusFromSidebar()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (BuildConfig.IS_TV) {
            sidebarNavigationPending = true
            binding.composeSidebar.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            binding.composeSidebar.clearFocus()
        }
        isSidebarExpanded.value = false
        val inAdmin = destination.id in setOf(R.id.adminDashboardFragment, R.id.movieListFragment, R.id.seriesListFragment, R.id.channelListFragment, R.id.adminHeroFragment)
        binding.composeSidebar.visibility = if (!BuildConfig.IS_TV || inAdmin) android.view.View.GONE else android.view.View.VISIBLE
        val detail = !BuildConfig.IS_TV || destination.id in setOf(R.id.movieDetailFragment, R.id.seriesDetailsFragment)
        if (!BuildConfig.IS_TV) {
            findViewById<android.widget.LinearLayout>(R.id.mobile_navigation)?.let { bar ->
                val selected = when (destination.id) {
                    R.id.movieDetailFragment -> R.id.moviesFragment
                    R.id.seriesDetailsFragment -> R.id.seriesFragment
                    R.id.adminDashboardFragment, R.id.movieListFragment, R.id.seriesListFragment, R.id.channelListFragment, R.id.adminHeroFragment -> R.id.settingsFragment
                    else -> destination.id
                }
                val rootPage = destination.id in setOf(R.id.homeFragment, R.id.moviesFragment, R.id.seriesFragment, R.id.tvFragment)
                val keyboard = androidx.core.view.ViewCompat.getRootWindowInsets(binding.root)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                bar.visibility = if (rootPage && !keyboard) android.view.View.VISIBLE else android.view.View.GONE
                findViewById<android.view.View>(R.id.mobile_back)?.visibility = if (rootPage) android.view.View.GONE else android.view.View.VISIBLE
                val utilityVisibility = if (rootPage) android.view.View.VISIBLE else android.view.View.GONE
                findViewById<android.view.View>(R.id.mobile_search)?.visibility = utilityVisibility
                findViewById<android.view.View>(R.id.mobile_settings)?.visibility = utilityVisibility
                findViewById<android.view.View>(R.id.mobile_brand)?.visibility = utilityVisibility
                for (index in 0 until bar.childCount) bar.getChildAt(index).apply {
                    isActivated = tag == selected
                    isSelected = isActivated
                }
            }
        }
        binding.navHostFragment.layoutParams = (binding.navHostFragment.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
            endToStart = if (detail) androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET else R.id.compose_sidebar
            endToEnd = if (detail) androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID else androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
        }
    }
    private val navController: NavController by lazy {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // The sidebar is transparent; extend each destination's background beneath it.
        val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        fun syncPageBackground(view: android.view.View) {
            val background = view.background?.constantState?.newDrawable(resources)?.mutate()
            if (background != null) binding.root.background = background
            insetDetailContent(view)
        }
        host.childFragmentManager.registerFragmentLifecycleCallbacks(
            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: androidx.fragment.app.FragmentManager,
                    f: androidx.fragment.app.Fragment,
                    v: android.view.View,
                    savedInstanceState: Bundle?
                ) {
                    if (f.parentFragment === host) syncPageBackground(v)
                }
                override fun onFragmentResumed(fm: androidx.fragment.app.FragmentManager, f: androidx.fragment.app.Fragment) {
                    if (f.parentFragment === host) f.view?.let { page ->
                        syncPageBackground(page)
                        page.post {
                            if (BuildConfig.IS_TV && f.isResumed && f === host.childFragmentManager.primaryNavigationFragment) {
                                if (!page.hasFocus()) page.requestFocus(android.view.View.FOCUS_FORWARD)
                                binding.composeSidebar.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
                                sidebarNavigationPending = false
                            }
                        }
                    }
                }
            }, false
        )
        host.childFragmentManager.primaryNavigationFragment?.let { fragment ->
            fragment.view?.let { syncPageBackground(it) }
        }
        binding.composeSidebar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.composeSidebar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            host.childFragmentManager.primaryNavigationFragment?.view?.let { insetDetailContent(it) }
        }

        navController.addOnDestinationChangedListener(destinationListener)


        binding.navHostFragment.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isSidebarExpanded.value) {
                isSidebarExpanded.value = false
            }
        }

        if (BuildConfig.IS_TV) findViewById<ComposeView>(R.id.compose_sidebar).setContent {
            Sidebar(
                navController = navController,
                onMenuSelected = { route ->
                    handleNavigation(route)
                },
                isExpanded = isSidebarExpanded.value,
                onExpandedChange = { expanded ->
                    if (!expanded || !sidebarNavigationPending) isSidebarExpanded.value = expanded
                }
            )
        }

        if (!BuildConfig.IS_TV) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            setupMobileNavigation()
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                val rootPage = navController.currentDestination?.id in setOf(R.id.homeFragment, R.id.moviesFragment, R.id.seriesFragment, R.id.tvFragment)
                findViewById<android.view.View>(R.id.mobile_navigation)?.visibility =
                    if (rootPage && !insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) android.view.View.VISIBLE else android.view.View.GONE
                insets
            }
        }
        setupBackPressHandler()
    }

    private fun setupMobileNavigation() {
        val bar = findViewById<android.widget.LinearLayout>(R.id.mobile_navigation) ?: return
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        bar.layoutParams.height = (dp(72) * maxOf(1f, resources.configuration.fontScale * 0.8f)).toInt()
        val tabs = listOf(
            Triple("خانه", "home", R.drawable.ic_menu_home),
            Triple("فیلم", "movies", R.drawable.ic_menu_movie),
            Triple("سریال", "series", R.drawable.ic_series),
            Triple("تلویزیون", "tv", R.drawable.ic_menu_tv)
        )
        val destinations = listOf(R.id.homeFragment, R.id.moviesFragment, R.id.seriesFragment, R.id.tvFragment)
        val tint = androidx.core.content.ContextCompat.getColorStateList(this, R.color.mobile_nav_text)
        tabs.forEachIndexed { index, (label, route, icon) ->
            val tab = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                tag = destinations[index]
                contentDescription = label
                isClickable = true
                isFocusable = true
                setPadding(dp(4), dp(4), dp(4), dp(4))
                background = null
                addView(android.widget.ImageView(context).apply {
                    setImageResource(icon)
                    imageTintList = tint
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    isDuplicateParentStateEnabled = true
                    setPadding(dp(16), dp(5), dp(16), dp(5))
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, android.widget.LinearLayout.LayoutParams(dp(56), dp(32)))
                addView(android.widget.TextView(context).apply {
                    text = label
                    textSize = 11f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.mobile_nav_label))
                    isDuplicateParentStateEnabled = true
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.yekanbakh_regular)
                }, android.widget.LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
                setOnClickListener { handleNavigation(route) }
            }
            bar.addView(tab, android.widget.LinearLayout.LayoutParams(0, -1, 1f).apply {
                marginStart = dp(4); marginEnd = dp(4)
            })
            tab.isActivated = navController.currentDestination?.id == destinations[index]
            tab.isSelected = tab.isActivated
        }
        fun openUtility(id: Int) {
            currentFocus?.let {
                (getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .hideSoftInputFromWindow(it.windowToken, 0)
                it.clearFocus()
            }
            if (navController.currentDestination?.id != id) navController.navigate(id, null,
                androidx.navigation.navOptions { launchSingleTop = true })
        }
        findViewById<android.view.View>(R.id.mobile_search).setOnClickListener { openUtility(R.id.searchFragment) }
        findViewById<android.view.View>(R.id.mobile_settings).setOnClickListener { openUtility(R.id.settingsFragment) }
        findViewById<android.view.View>(R.id.mobile_back).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun insetDetailContent(view: android.view.View) {
        val content = view.findViewById<android.view.View>(R.id.detail_content) ?: return
        if (!BuildConfig.IS_TV) return
        val gutter = (56 * resources.displayMetrics.density).toInt()
        val menuGap = resources.getDimensionPixelSize(R.dimen.tv_content_menu_gap)
        content.setPadding(gutter, content.paddingTop, menuGap + binding.composeSidebar.width, content.paddingBottom)
    }

    private fun handleNavigation(route: String) {
        val destinationId = when (route) {
            "home" -> R.id.homeFragment
            "movies" -> R.id.moviesFragment
            "series" -> R.id.seriesFragment
            "tv" -> R.id.tvFragment
            "search" -> R.id.searchFragment
            "settings" -> R.id.settingsFragment
            else -> R.id.homeFragment
        }

        navController.openMenuDestination(destinationId)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDest = navController.currentDestination?.id

                isSidebarExpanded.value = false

                if (currentDest == R.id.homeFragment) {
                    if (BuildConfig.IS_TV) showExitDialog() else finish()
                } else {
                    if (!navController.popBackStack() && !BuildConfig.IS_TV) handleNavigation("home")
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
            findViewById<Button>(R.id.btnExitNo).requestFocus()
        }
    }

    fun collapseSidebar() {
        isSidebarExpanded.value = false
    }

    private var activationListener: com.google.firebase.database.ValueEventListener? = null
    private var activationRef: com.google.firebase.database.DatabaseReference? = null
    override fun onStart() {
        super.onStart()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { openLogin(); return }
        val ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid).child("isActive")
        activationRef = ref
        activationListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) != true) openLogin()
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.widget.Toast.makeText(this@MainActivity, "بررسی حساب انجام نشد؛ اتصال را بررسی کنید", android.widget.Toast.LENGTH_LONG).show()
            }
        }.also { ref.addValueEventListener(it) }
    }
    private fun openLogin() {
        startActivity(android.content.Intent(this, LoginActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
    override fun onStop() {
        activationListener?.let { activationRef?.removeEventListener(it) }
        activationListener = null
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        navController.removeOnDestinationChangedListener(destinationListener)
    }
}
