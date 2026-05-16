package com.example.kelkin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
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

    // وضعیت پلتفرم: می تواند "DIRECT" یا "VK" یا "OK" باشد
    private var videoPlatform = "DIRECT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val startPos = intent.getLongExtra("start_pos", 0L)

        // ۱. تشخیص و جداسازی پلتفرم‌ها بر اساس لینک ورودی
        if (videoUrl.contains("vkvideo.ru") || videoUrl.contains("vk.com")) {
            videoPlatform = "VK"
            val embedUrl = convertToVkEmbedUrl(videoUrl, startPos)
            setupWebView(embedUrl)
        } else if (videoUrl.contains("ok.ru")) {
            videoPlatform = "OK"
            val embedUrl = convertToOkEmbedUrl(videoUrl, startPos)
            setupWebView(embedUrl)
        } else {
            videoPlatform = "DIRECT"
            setupPlayer(videoUrl, startPos)
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // اگر ویدیو تحت وب (VK یا OK) بود ابتدا دیتای آن را استخراج می‌کنیم، در غیر این صورت مستقیم از اکسوپلیر می‌گیریم
                if (videoPlatform == "VK" || videoPlatform == "OK") {
                    saveWebPositionAndFinish()
                } else {
                    savePositionDirectly()
                    finish()
                }
            }
        })
    }

    // متد تبدیل لینک معمولی VK به لینک Embed مستقل
    private fun convertToVkEmbedUrl(url: String, startPosMs: Long): String {
        try {
            val videoPart = url.substringAfter("video-").substringAfter("video_")
            if (videoPart.contains("_")) {
                val parts = videoPart.split("_")
                val oid = parts[0]
                val id = parts[1].split("?")[0]
                val finalOid = if (url.contains("video-")) "-$oid" else oid
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

    // متد تبدیل لینک ثابت OK.ru به لینک Embed مستقل به همراه اعمال زمان شروع فیلم
    private fun convertToOkEmbedUrl(url: String, startPosMs: Long): String {
        try {
            val startSeconds = startPosMs / 1000
            val videoId = url.substringAfterLast("/")
            return "https://ok.ru/videoembed/$videoId?autoplay=1&st.start=$startSeconds"
        } catch (e: Exception) {
            Log.e("OK_Parser", "Error converting URL: ${e.message}")
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
        binding.playerView.visibility = View.GONE
        binding.vkPlayerContainer.visibility = View.VISIBLE

        binding.vkWebView.apply {
            keepScreenOn = true

            isFocusable = false
            isFocusableInTouchMode = false
            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // تزریق استایل‌های تمام‌صفحه و جاوااسکریپت بر اساس پلتفرم ویدیویی جاری
                    if (videoPlatform == "VK") {
                        val cssFullscreen = """
                            var style = document.createElement('style');
                            style.innerHTML = 'video, .video_box, iframe { width: 100vw !important; height: 100vh !important; position: fixed !important; top: 0 !important; left: 0 !important; z-index: 999999 !important; object-fit: contain !important; }';
                            document.head.appendChild(style);
                        """.trimIndent()
                        evaluateJavascript("javascript:$cssFullscreen", null)
                    } else if (videoPlatform == "OK") {
                        val jsOkSetup = """
                            var video = document.querySelector('video');
                            if (video) {
                                var overlay = document.querySelector('.vp_g_overlay');
                                if (overlay) overlay.style.display = 'none';
                                
                                video.style.position = 'fixed';
                                video.style.top = '0';
                                video.style.left = '0';
                                video.style.width = '100vw';
                                video.style.height = '100vh';
                                video.style.zIndex = '999999';
                                video.style.backgroundColor = 'black';
                                video.style.objectFit = 'contain';
                            }
                        """.trimIndent()
                        evaluateJavascript("javascript:$jsOkSetup", null)
                    }

                    // انتقال مستقیم فوکوس به لایه ماوس مجازی تلویزیون
                    binding.vkVirtualMouse.requestFocus()
                }
            }
            webChromeClient = android.webkit.WebChromeClient()
            loadUrl(url)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // مدیریت دکمه‌های کنترلی سخت‌افزاری تلویزیون برای هر دو پلیر تحت وب
        if (videoPlatform == "VK" || videoPlatform == "OK") {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    binding.vkVirtualMouse.click(binding.vkWebView)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // متد استخراج هوشمند زمان پخش از داخل کدهای HTML هر دو وب‌پلیر به صورت مجزا
    private fun saveWebPositionAndFinish() {
        val jsGetTime = if (videoPlatform == "VK") {
            """
                (function() {
                    var video = document.querySelector('video');
                    return video ? [video.currentTime, video.duration] : [0, 0];
                })()
            """.trimIndent()
        } else {
            // کد اختصاصی گرفتن زمان برای پلیر OK.ru
            """
                (function() {
                    var video = document.querySelector('video');
                    return video ? [video.currentTime, video.duration] : [0, 0];
                })()
            """.trimIndent()
        }

        binding.vkWebView.evaluateJavascript(jsGetTime) { result ->
            try {
                if (!result.isNullOrEmpty() && result != "[0,0]" && result != "null") {
                    val cleanResult = result.replace("[", "").replace("]", "")
                    val parts = cleanResult.split(",")
                    if (parts.size >= 2) {
                        val currentSeconds = parts[0].toDoubleOrNull() ?: 0.0
                        val durationSeconds = parts[1].toDoubleOrNull() ?: 0.0

                        val currentPosMs = (currentSeconds * 1000).toLong()
                        val durationMs = (durationSeconds * 1000).toLong()

                        savePositionDirectly(currentPosMs, durationMs)
                    }
                }
            } catch (e: Exception) {
                Log.e("ContinueWatching", "Error parsing Web time: ${e.message}")
            }
            finish()
        }
    }

    // متد جامع و منعطف ذخیره‌سازی زمان فیلم‌ها در سیستم ادامه پخش دیتابیس لوکل برنامه
    private fun savePositionDirectly(customPos: Long? = null, customDuration: Long? = null) {
        val currentPos = customPos ?: (player?.currentPosition ?: 0L)
        val totalDur = customDuration ?: (player?.duration ?: 0L)

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
            val prefs = getSharedPreferences("kelkin_prefs", Context.MODE_PRIVATE)
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