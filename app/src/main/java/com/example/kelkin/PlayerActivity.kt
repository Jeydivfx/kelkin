package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.databinding.ActivityPlayerBinding
import com.google.gson.Gson

class PlayerActivity : AppCompatActivity() {

    val binding by lazy { ActivityPlayerBinding.inflate(layoutInflater) }
    private var player: ExoPlayer? = null
    var isVkVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val startPos = intent.getLongExtra("start_pos", 0L)

        if (videoUrl.contains("vkvideo.ru") || videoUrl.contains("vk.com")) {
            isVkVideo = true
            val embedUrl = convertToVkEmbedUrl(videoUrl, startPos)
            setupWebView(embedUrl)
        } else {
            isVkVideo = false
            setupPlayer(videoUrl, startPos)
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isVkVideo) {
                    savePositionDirectly()
                }
                finish()
            }
        })
    }

    private fun convertToVkEmbedUrl(url: String, startPosMs: Long): String {
        try {
            val videoPart = url.substringAfter("video-").substringAfter("video_")
            if (videoPart.contains("_")) {
                val parts = videoPart.split("_")
                val oid = parts[0]
                val id = parts[1].split("?")[0]
                val finalOid = if (url.contains("video-")) "-$oid" else oid

                // تبدیل میلی‌ثانیه به ثانیه برای پلیر تحت وب VK
                val startSeconds = startPosMs / 1000

                return if (startSeconds > 0) {
                    "https://vk.com/video_ext.php?oid=$finalOid&id=$id&autoplay=1&t=${startSeconds}s"
                } else {
                    "https://vk.com/video_ext.php?oid=$finalOid&id=$id&autoplay=1"
                }
            }
        } catch (e: Exception) {
            Log.e("VK_Parser", "Error converting URL: ${e.message}")
        }
        return url
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer(url: String, startPos: Long) {
        binding.playerView.visibility = View.VISIBLE
        binding.vkPlayerContainer.visibility = View.GONE

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            binding.playerView.keepScreenOn = true
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            if (startPos > 0) { exoPlayer.seekTo(startPos) }
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    private fun setupWebView(url: String) {
        binding.playerView.visibility = android.view.View.GONE
        binding.vkPlayerContainer.visibility = android.view.View.VISIBLE

        binding.vkWebView.apply {
            keepScreenOn = true

            // --- اضافه کردن این ۳ خط برای جلوگیری از دزدیدن فوکوس ---
            isFocusable = false                  // وب‌ویو نباید قابل فوکوس باشد
            isFocusableInTouchMode = false       // جلوگیری از فوکوس لمسی
            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS // بلاک کردن فوکوس فرزندان وب‌ویو

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    val cssFullscreen = """
                    var style = document.createElement('style');
                    style.innerHTML = 'video, .video_box, iframe { width: 100vw !important; height: 100vh !important; position: fixed !important; top: 0 !important; left: 0 !important; z-index: 999999 !important; object-fit: contain !important; }';
                    document.head.appendChild(style);
                """.trimIndent()
                    evaluateJavascript("javascript:$cssFullscreen", null)

                    // بعد از پایان لود، فوکوس را با قدرت به لایه موس مجازی می‌دهیم
                    binding.vkVirtualMouse.requestFocus()
                }
            }
            webChromeClient = android.webkit.WebChromeClient()
            loadUrl(url)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isVkVideo) {
            // کلیدهای جهت‌نما و کلیک به طور خودکار توسط کلاس VirtualMouseView مدیریت می‌شوند.
            // اینجا فقط کلیدهای خاص ریموت مثل دکمه پخش/توقف سخت‌افزاری را هندل می‌کنیم.
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    binding.vkVirtualMouse.click(binding.vkWebView)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun savePositionDirectly() {
        val currentPos = player?.currentPosition ?: 0L
        val totalDur = player?.duration ?: 0L

        val resultIntent = Intent().apply {
            putExtra("last_pos", currentPos)
            putExtra("total_dur", totalDur)
        }
        setResult(RESULT_OK, resultIntent)

        val movieTmdbId = intent.getStringExtra("movie_tmdb_id")
        val movieJson = intent.getStringExtra("movie_json")

        if (movieTmdbId == null || movieJson == null) {
            Log.e("ContinueWatching", "Error: Intent data is NULL!")
            return
        }

        if (currentPos > 2000) {
            val prefs = getSharedPreferences("kelkin_prefs", MODE_PRIVATE)
            val gson = Gson()

            val json = prefs.getString("saved_continue_list", null)
            val type = object : com.google.gson.reflect.TypeToken<MutableList<Movie>>() {}.type
            val list: MutableList<Movie> = if (json != null) {
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }

            val currentMovie = gson.fromJson(movieJson, Movie::class.java)
            list.removeAll { it.tmdb_id == movieTmdbId }

            currentMovie.lastPosition = currentPos
            currentMovie.totalDuration = totalDur
            list.add(0, currentMovie)

            prefs.edit().putString("saved_continue_list", gson.toJson(list)).apply()
        } else {
            Log.w("ContinueWatching", "Position too low ($currentPos), not saving.")
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}