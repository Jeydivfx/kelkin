package com.example.kelkin

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViewById<ComposeView>(R.id.compose_sidebar).setContent {
            // پاس دادن کنترلر به کامپوز
            Sidebar(navController = navController, onMenuSelected = { route ->
                handleNavigation(route)
            })
        }


        setupBackPressHandler()
    }

    private fun handleNavigation(route: String) {
        val destinationId = when (route) {
            "home" -> R.id.homeFragment
            "movies" -> R.id.moviesFragment
            "tv" -> R.id.tvFragment
            "search" -> R.id.searchFragment
            "settings" -> R.id.settingsFragment
            "series" -> {
                android.widget.Toast.makeText(this, "به زودی اضافه می‌شود", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            else -> R.id.homeFragment
        }

        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDest = navController.currentDestination?.id

                if (currentDest == R.id.homeFragment) {
                    showExitDialog()
                } else {
                    navController.popBackStack(R.id.homeFragment, false)
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