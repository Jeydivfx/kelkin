package com.example.kelkin

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import com.example.kelkin.utils.openMenuDestination

/** Production sidebar beside native fragment views; no authentication or server access. */
@android.annotation.SuppressLint("ResourceType")
class SidebarHarnessActivity : AppCompatActivity() {
    lateinit var nav: NavController
    private val expanded = mutableStateOf(false)
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val container = FragmentContainerView(this).apply { id = 0x72001 }
        val menu = ComposeView(this)
        root.addView(container, LinearLayout.LayoutParams(0, -1, 1f))
        root.addView(menu, LinearLayout.LayoutParams(-2, -1))
        setContentView(root)
        val host = NavHostFragment()
        supportFragmentManager.beginTransaction().replace(container.id, host).setPrimaryNavigationFragment(host).commitNow()
        nav = host.navController
        val navigator = nav.navigatorProvider.getNavigator(FragmentNavigator::class.java)
        nav.graph = NavGraph(NavGraphNavigator(nav.navigatorProvider)).apply {
            id = 0x72002
            for (id in listOf(R.id.homeFragment, R.id.moviesFragment, R.id.seriesFragment, R.id.tvFragment, R.id.searchFragment, R.id.settingsFragment, R.id.movieDetailFragment, R.id.seriesDetailsFragment, R.id.movieListFragment, R.id.seriesListFragment, R.id.channelListFragment)) {
                addDestination(navigator.createDestination().apply { this.id = id; setClassName(SidebarContentFragment::class.java.name) })
            }
            setStartDestination(R.id.moviesFragment)
        }
        nav.addOnDestinationChangedListener { _, _, _ -> expanded.value = false }
        menu.setContent {
            Sidebar(nav, { route ->
                nav.openMenuDestination(when (route) {
                    "home" -> R.id.homeFragment; "movies" -> R.id.moviesFragment
                    "series" -> R.id.seriesFragment; "tv" -> R.id.tvFragment
                    "search" -> R.id.searchFragment; else -> R.id.settingsFragment
                })
            }, expanded.value, { expanded.value = it })
        }
    }
}
@android.annotation.SuppressLint("ResourceType")
class SidebarContentFragment : Fragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, parent: ViewGroup?, state: Bundle?): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            repeat(5) { index ->
                addView(Button(context).apply { id = 0x72010 + index; text = "Content $index" }, LinearLayout.LayoutParams(-1, 0, 1f))
            }
        }
}
