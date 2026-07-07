package com.example.kelkin

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private var isNavigated = false
    private var exoPlayer: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var isVideoPrepared = false
    private var isVideoPlaying = false
    private var isDataLoaded = false
    private var dataLoadAttempts = 0

    companion object {
        private const val SPLASH_TIMEOUT = 15000L
        private const val VIDEO_DURATION = 10000L
        private const val DATA_TIMEOUT = 8000L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val versionText = findViewById<TextView>(R.id.txtVersion)
        val versionNumber = getAppVersionName()
        val persianVersion = convertToPersianNumbers(versionNumber)
        versionText?.text = "نسخه کلکین : $persianVersion"

        playerView = findViewById(R.id.playerView)
        playerView.visibility = View.GONE

        setupExoPlayer()

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        observeData()

        lifecycleScope.launch {
            delay(DATA_TIMEOUT)
            if (!isDataLoaded && !isNavigated) {
                navigateToMain()
            }
        }

        lifecycleScope.launch {
            delay(SPLASH_TIMEOUT)
            if (!isNavigated) {
                navigateToMain()
            }
        }
    }

    private fun convertToPersianNumbers(text: String): String {
        val persianDigits = mapOf(
            '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
            '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
        )
        return text.map { persianDigits[it] ?: it }.joinToString("")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    private fun setupExoPlayer() {
        try {
            val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.splash_animation}")

            try {
                resources.openRawResource(R.raw.splash_animation).close()
            } catch (e: Exception) {
                showFallbackLogo()
                return
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            exoPlayer = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .build()
                .also { player ->
                    player.volume = 1.0f
                    player.setAudioAttributes(audioAttributes, true)

                    playerView.player = player
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    playerView.useController = false

                    val mediaItem = MediaItem.fromUri(videoUri)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                    player.repeatMode = Player.REPEAT_MODE_OFF

                    var videoDuration = 0L

                    player.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    videoDuration = player.duration
                                    val waitTime = maxOf(videoDuration, VIDEO_DURATION)

                                    isVideoPrepared = true
                                    runOnUiThread {
                                        playerView.visibility = View.VISIBLE
                                    }

                                    if (!isVideoPlaying) {
                                        player.play()
                                        isVideoPlaying = true

                                        if (!isNavigated) {
                                            lifecycleScope.launch {
                                                delay(waitTime)
                                                if (!isNavigated && isVideoPlaying) {
                                                    navigateToMain()
                                                }
                                            }
                                        }
                                    }
                                }
                                Player.STATE_BUFFERING -> {}
                                Player.STATE_ENDED -> {
                                    if (!isNavigated) {
                                        navigateToMain()
                                    }
                                }
                            }
                        }

                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            runOnUiThread {
                                playerView.visibility = View.GONE
                                showFallbackLogo()
                            }
                            if (!isNavigated) {
                                lifecycleScope.launch {
                                    delay(2000)
                                    navigateToMain()
                                }
                            }
                        }
                    })
                }

        } catch (e: Exception) {
            playerView.visibility = View.GONE
            showFallbackLogo()
            if (!isNavigated) {
                lifecycleScope.launch {
                    delay(2000)
                    navigateToMain()
                }
            }
        }
    }

    private fun observeData() {
        viewModel.moviesList.observe(this) { movies ->
            if (!movies.isNullOrEmpty()) {
                isDataLoaded = true
                dataLoadAttempts++
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFallbackLogo() {
        val container = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mainContainer)

        val txtLogo = TextView(this).apply {
            text = "K E L K I N"
            textSize = 70f
            setTextColor(resources.getColor(android.R.color.holo_orange_dark, theme))
            typeface = resources.getFont(R.font.bebas_neue_rgular)
        }

        container.addView(txtLogo)

        val params = txtLogo.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        txtLogo.layoutParams = params

        if (!isNavigated) {
            lifecycleScope.launch {
                delay(2000)
                navigateToMain()
            }
        }
    }

    private fun getAppVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }

    private fun navigateToMain() {
        if (isNavigated) return
        isNavigated = true

        try {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {}

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            UserManager.checkUserActivationStatus { isActive ->
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

    override fun onPause() {
        super.onPause()
        try {
            exoPlayer?.pause()
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try {
            if (isVideoPrepared && !isNavigated) {
                exoPlayer?.play()
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {}
    }
}