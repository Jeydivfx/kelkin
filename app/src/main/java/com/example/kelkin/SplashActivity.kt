package com.example.kelkin

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

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
        private const val VIDEO_DURATION = 2000L
        private const val DATA_TIMEOUT = 8000L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.IS_TV) {
            navigateToMain()
            return
        }
        setContentView(R.layout.activity_splash)

        val versionText = findViewById<TextView>(R.id.txtVersion)
        val versionNumber = getAppVersionName()
        val persianVersion = convertToPersianNumbers(versionNumber)
        versionText?.text = "نسخه $persianVersion"

        playerView = findViewById(R.id.playerView)
        playerView.findViewById<android.widget.ProgressBar>(androidx.media3.ui.R.id.exo_buffering)?.indeterminateTintList =
            android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.yellow))
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
                finishIntroAfterError()
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
                                    val waitTime = minOf(videoDuration.coerceAtLeast(VIDEO_DURATION), 3000L)

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
                                finishIntroAfterError()
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
            finishIntroAfterError()
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
    private fun finishIntroAfterError() {
        playerView.visibility = View.GONE
        navigateToMain()
    }

    private fun getAppVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }

    private fun navigateToMain() {
        if (isNavigated) return
        isNavigated = true
        exoPlayer?.release(); exoPlayer = null
        lifecycleScope.launch {
            val next = try {
                if (FirebaseAuth.getInstance().currentUser != null && UserManager.activation() == UserManager.Activation.ACTIVE) MainActivity::class.java else LoginActivity::class.java
            } catch (e: TimeoutCancellationException) { LoginActivity::class.java }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { LoginActivity::class.java }
            startActivity(Intent(this@SplashActivity, next)); finish()
        }
    }

    // Intercept before PlayerView or another focused child consumes the remote OK.
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val confirm = event.keyCode in intArrayOf(
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER,
            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
            android.view.KeyEvent.KEYCODE_BUTTON_A
        )
        if (confirm) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                navigateToMain()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
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
