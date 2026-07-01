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
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.ChannelAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.ActivityPlayerBinding
import com.example.kelkin.utils.KelkinLivePlayer
import com.google.gson.Gson

class PlayerActivity : AppCompatActivity() {

    val binding by lazy { ActivityPlayerBinding.inflate(layoutInflater) }
    private var player: ExoPlayer? = null
    private lateinit var viewModel: HomeViewModel
    private var vlcPlayer: KelkinLivePlayer? = null
    private var videoPlatform = "DIRECT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val isLive = intent.getBooleanExtra("is_live", false)
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val startPos = intent.getLongExtra("start_pos", 0L)

        when {
            // ۱. پخش VK
            videoUrl.contains("vkvideo.ru") || videoUrl.contains("vk.com") -> {
                videoPlatform = "VK"
                // مخفی کردن بقیه پلیرها
                binding.playerView.visibility = View.GONE
                binding.vlcLayout.visibility = View.GONE
                setupWebView(convertToVkEmbedUrl(videoUrl, startPos))
            }
            // ۲. پخش OK
            videoUrl.contains("ok.ru") -> {
                videoPlatform = "OK"
                binding.playerView.visibility = View.GONE
                binding.vlcLayout.visibility = View.GONE
                setupWebView(convertToOkEmbedUrl(videoUrl, startPos))
            }

            isLive -> {
                videoPlatform = "LIVE_TV"
                binding.playerView.visibility = View.GONE
                binding.vkPlayerContainer.visibility = View.GONE
                binding.vlcLayout.visibility = View.VISIBLE

                vlcPlayer = KelkinLivePlayer(this, binding.vlcLayout) { errorMessage ->
                    runOnUiThread {
                        android.widget.Toast.makeText(this, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }


                viewModel.streamHeaders.observe(this) { headers ->
                    if (headers != null) {
                        vlcPlayer?.play(videoUrl, headers)
                    }
                }

                viewModel.syncStreamHeaders()

                setupChannelList()
            }

            else -> {
                videoPlatform = "DIRECT"
                binding.vkPlayerContainer.visibility = View.GONE
                binding.vlcLayout.visibility = View.GONE
                binding.playerView.visibility = View.VISIBLE
                setupPlayer(videoUrl, startPos)
            }
        }


        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (videoPlatform == "LIVE_TV" && binding.channelOverlay.visibility == View.VISIBLE) {
                    binding.channelOverlay.visibility = View.GONE
                    return
                }

                if (videoPlatform == "VK" || videoPlatform == "OK") {
                    saveWebPositionAndFinish()
                } else {
                    savePositionDirectly()
                    finish()
                }
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

                    binding.vkVirtualMouse.requestFocus()
                }
            }
            webChromeClient = android.webkit.WebChromeClient()
            loadUrl(url)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (videoPlatform == "VK" || videoPlatform == "OK") {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                binding.vkVirtualMouse.click(binding.vkWebView)
                return true
            }
        }

        if (videoPlatform == "LIVE_TV" && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (binding.channelOverlay.visibility == View.GONE) {
                binding.channelOverlay.visibility = View.VISIBLE
                binding.rvLiveChannels.requestFocus()
                return true // رویداد مصرف شد
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun saveWebPositionAndFinish() {
        val jsGetTime = if (videoPlatform == "VK") {
            """
                (function() {
                    var video = document.querySelector('video');
                    return video ? [video.currentTime, video.duration] : [0, 0];
                })()
            """.trimIndent()
        } else {
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

    private fun setupChannelList() {
        // تنظیمات ریسایکلر ویو
        binding.rvLiveChannels.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val adapter = ChannelAdapter { selectedChannel ->
            // ۱. دریافتِ هدرهایِ لحظه‌ای از ViewModel
            val currentHeaders = viewModel.streamHeaders.value

            // ۲. چک کردنِ اینکه آیا هدرها موجود هستند یا خیر
            if (currentHeaders != null) {
                vlcPlayer?.play(selectedChannel.videoUrl, currentHeaders)
            } else {
                // اگر هدرها هنوز نیامده، یک هشدار یا تلاش مجدد
                android.widget.Toast.makeText(this, "در حال دریافت تنظیمات...", android.widget.Toast.LENGTH_SHORT).show()
            }

            binding.channelOverlay.visibility = View.GONE
        }
        binding.rvLiveChannels.adapter = adapter

        viewModel.channels.observe(this) { channels ->
            if (!channels.isNullOrEmpty()) {
                adapter.submitList(channels)
                Log.d("KELKIN_DEBUG", "Channels observed in Player: ${channels.size}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        vlcPlayer?.release()
        vlcPlayer = null
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

}