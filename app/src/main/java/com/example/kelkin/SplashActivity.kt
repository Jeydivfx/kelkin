package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kelkin.ViewModels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private var isNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<TextView>(R.id.txtVersion)?.text = "Version ${getAppVersionName()}"
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        observeData()

        lifecycleScope.launch {
            delay(5000)
            if (!isNavigated) {
                Log.d("Splash", "Timeout reached. Navigating...")
                navigateToMain()
            }
        }
    }

    private fun getAppVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }

    private fun observeData() {
        viewModel.moviesList.observe(this) { movies ->
            if (!movies.isNullOrEmpty()) {
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        if (isNavigated) return
        isNavigated = true

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            com.example.kelkin.utils.UserManager.checkUserActivationStatus { isActive ->
                if (isActive) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}