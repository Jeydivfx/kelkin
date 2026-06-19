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

        // ۱. نمایش نسخه دینامیک (با ID که در لایوت داری)
        findViewById<TextView>(R.id.txtVersion)?.text = "Version ${getAppVersionName()}"

        // پروگرس‌بار شما به صورت خودکار به دلیل indeterminate="true" در XML فعال است

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        observeData()

        // ۲. تایم‌اوت محافظتی (۵ ثانیه)
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
        // ۳. ناوبری هوشمند به محضِ آماده شدنِ لیستِ فیلم‌ها
        viewModel.moviesList.observe(this) { movies ->
            if (!movies.isNullOrEmpty()) {
                Log.d("Splash", "Movies loaded, navigating...")
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
                    // اینجا به جای اینکه به ویومدل تکیه کنیم، مستقیم می‌رویم
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // اگر اکتیو نیست
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}