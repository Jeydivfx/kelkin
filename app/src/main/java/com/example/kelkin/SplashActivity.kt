package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kelkin.ViewModels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var txtLogo: TextView
    private lateinit var txtVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        txtLogo = findViewById(R.id.txtLogo)
        txtVersion = findViewById(R.id.txtVersion)

        displayAppVersion()

        // شروع عملیات دریافت دیتا
        viewModel.fetchData()
        observeData()
    }

    private fun displayAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            txtVersion.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            txtVersion.text = "Version 1.0.0"
        }
    }

    private fun observeData() {
        viewModel.moviesList.observe(this) { movies ->
            if (!movies.isNullOrEmpty()) {
                navigateToMain()
            }
        }

        lifecycleScope.launch {
            // سقف انتظار برای لود شدن دیتا
            delay(12000)
            if (viewModel.moviesList.value.isNullOrEmpty()) {
                Log.e("SplashError", "دیتایی دریافت نشد یا اینترنت وصل نیست")
                // اینجا می‌تونی یک پیام خطا به کاربر نشون بدی
            }
        }
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            // یک مکث بسیار کوتاه برای اینکه کاربر لوگو رو ببینه و بعد انتقال
            delay(500)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            // استفاده از انیمیشن استاندارد سیستمی برای جابجایی نرم
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}