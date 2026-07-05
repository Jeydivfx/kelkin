package com.example.kelkin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.ChannelAdapter
import com.example.kelkin.Adapter.SourceAdapter
import com.example.kelkin.DataClass.Channel
import com.example.kelkin.DataClass.ChannelSource
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.StreamHeaders
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.ActivityPlayerBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.atomic.AtomicBoolean

class PlayerActivity : AppCompatActivity() {

    internal val binding by lazy { ActivityPlayerBinding.inflate(layoutInflater) }

    private var exoPlayer: ExoPlayer? = null
    private lateinit var viewModel: HomeViewModel
    private lateinit var sourceAdapter: SourceAdapter

    private lateinit var currentChannel: Channel
    private var selectedCategoryId: Long = 0L

    private var videoPlatform = VideoPlatform.DIRECT
    private var currentVideoUrl: String = ""
    private var startPosition: Long = 0L
    private var currentHeaders: StreamHeaders? = null

    private var liveProgressBar: ProgressBar? = null
    private var liveStatusText: TextView? = null

    private val isReleased = AtomicBoolean(false)

    companion object {
        private const val PREFS_NAME = "kelkin_prefs"
        private const val SAVED_CONTINUE_LIST_KEY = "saved_continue_list"
        private const val MIN_SAVE_POSITION_MS = 2000L
        private const val VK_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val USER_AGENT_LIVE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }

    private enum class VideoPlatform {
        DIRECT, VK, OK, LIVE_TV
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        val channelJson = intent.getStringExtra("channel_json")
        currentChannel = if (!channelJson.isNullOrEmpty()) {
            Gson().fromJson(channelJson, Channel::class.java)
        } else {
            Channel()
        }

        selectedCategoryId = intent.getLongExtra("selected_category_id", 0L)
        currentVideoUrl = intent.getStringExtra("video_url") ?: ""
        startPosition = intent.getLongExtra("start_pos", 0L)

        sourceAdapter = SourceAdapter(emptyList()) { selectedSource ->
            playLiveStream(selectedSource.url)
            binding.sourceOverlay.visibility = View.GONE
        }

        binding.apply {
            rvSources.adapter = sourceAdapter
            rvSources.layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.streamHeaders.observe(this) { headers ->
            if (headers != null && videoPlatform == VideoPlatform.LIVE_TV) {
                currentHeaders = headers
                if (!isReleased.get()) {
                    playLiveStream(currentVideoUrl)
                }
            }
        }

        viewModel.channels.observe(this) { channels ->
            updateChannelList(channels)
        }

        viewModel.syncStreamHeaders()

        val isLive = intent.getBooleanExtra("is_live", false)
        val videoUrl = currentVideoUrl
        val startPos = startPosition

        videoPlatform = when {
            videoUrl.contains("vkvideo.ru") || videoUrl.contains("vk.com") -> VideoPlatform.VK
            videoUrl.contains("ok.ru") -> VideoPlatform.OK
            isLive -> VideoPlatform.LIVE_TV
            else -> VideoPlatform.DIRECT
        }

        when (videoPlatform) {
            VideoPlatform.VK -> setupWebViewPlayer(convertToVkEmbedUrl(videoUrl, startPos))
            VideoPlatform.OK -> setupWebViewPlayer(convertToOkEmbedUrl(videoUrl, startPos))
            VideoPlatform.LIVE_TV -> setupLiveTvPlayer(videoUrl)
            VideoPlatform.DIRECT -> setupDirectPlayer(videoUrl, startPos)
        }

        setupBackPressHandler()
        setupChannelList()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        if (isReleased.getAndSet(true)) {
            return
        }

        binding.vkWebView.apply {
            stopLoading()
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }

        binding.rvSources.adapter = null
        binding.rvLiveChannels.adapter = null

        releaseAllPlayers()

        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when {
            handleMediaPlayPause(keyCode) -> true
            handleChannelUpNavigation(keyCode) -> true
            handleChannelDownNavigation(keyCode) -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupDirectPlayer(url: String, startPos: Long) {
        hideAllVideoViews()
        binding.playerView.visibility = View.VISIBLE
        binding.playerView.useController = true

        val trackSelector = DefaultTrackSelector(this)
        exoPlayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { player ->
                binding.playerView.player = player
                player.setMediaItem(MediaItem.fromUri(url))
                if (startPos > 0) {
                    player.seekTo(startPos)
                }
                player.prepare()
                player.playWhenReady = true
            }
    }

    @OptIn(UnstableApi::class)
    private fun setupLiveTvPlayer(url: String) {
        hideAllVideoViews()

        val liveView = layoutInflater.inflate(R.layout.custom_live_player, null)
        binding.liveContainer.removeAllViews()
        binding.liveContainer.addView(liveView)
        binding.liveContainer.visibility = View.VISIBLE

        val livePlayerView = liveView.findViewById<PlayerView>(R.id.livePlayerView)
        liveProgressBar = liveView.findViewById(R.id.liveBufferingProgress)
        liveStatusText = liveView.findViewById(R.id.liveStatusText)
        val bufferingContainer = liveView.findViewById<View>(R.id.liveBufferingContainer)

        livePlayerView.useController = false
        livePlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)

        if (currentHeaders == null) {
            bufferingContainer.visibility = View.VISIBLE
            liveStatusText?.text = "در حال دریافت تنظیمات..."
            return
        }

        playLiveStream(url)
    }

    @OptIn(UnstableApi::class)
    private fun playLiveStream(url: String) {
        if (isReleased.get()) {
            return
        }

        val headers = currentHeaders
        if (headers == null) {
            Toast.makeText(this, "در حال دریافت تنظیمات...", Toast.LENGTH_SHORT).show()
            return
        }

        val bufferingContainer = binding.liveContainer.findViewById<View>(R.id.liveBufferingContainer)
        bufferingContainer.visibility = View.VISIBLE
        liveStatusText?.text = "در حال اتصال..."
        liveProgressBar?.visibility = View.VISIBLE

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT_LIVE)
            .setDefaultRequestProperties(mapOf(
                "Referer" to (headers.referrer ?: ""),
                "Origin" to (headers.origin ?: ""),
                "User-Agent" to USER_AGENT_LIVE
            ))

        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(url))

        val trackSelector = DefaultTrackSelector(this)

        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
                .also { player ->
                    val livePlayerView = binding.liveContainer.findViewById<PlayerView>(R.id.livePlayerView)
                    livePlayerView.player = player

                    player.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    bufferingContainer.visibility = View.GONE
                                }
                                Player.STATE_BUFFERING -> {
                                    bufferingContainer.visibility = View.VISIBLE
                                    liveStatusText?.text = "در حال آماده سازی ..."
                                    liveProgressBar?.visibility = View.VISIBLE
                                }
                                else -> {}
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            bufferingContainer.visibility = View.VISIBLE
                            liveStatusText?.text = "خطا در پخش! تلاش مجدد..."
                            liveProgressBar?.visibility = View.VISIBLE

                            binding.liveContainer.postDelayed({
                                if (!isReleased.get()) {
                                    playLiveStream(url)
                                }
                            }, 3000)
                        }
                    })
                }
        }

        exoPlayer?.apply {
            setMediaSource(hlsMediaSource)
            prepare()
            playWhenReady = true
        }

        videoPlatform = VideoPlatform.LIVE_TV
    }

    private fun setupWebViewPlayer(url: String) {
        hideAllVideoViews()
        binding.vkPlayerContainer.visibility = View.VISIBLE
        setupWebView(url)
    }

    private fun hideAllVideoViews() {
        binding.apply {
            playerView.visibility = View.GONE
            playerView.useController = true
            liveContainer.visibility = View.GONE
            liveContainer.removeAllViews()
            vkPlayerContainer.visibility = View.GONE
        }
    }

    private fun setupWebView(url: String) {
        binding.vkWebView.apply {
            keepScreenOn = true
            isFocusable = false
            isFocusableInTouchMode = false
            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = VK_USER_AGENT
            }

            webViewClient = createWebViewClient()
            webChromeClient = android.webkit.WebChromeClient()

            loadUrl(url)
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyVideoFullscreenStyling()
                binding.vkVirtualMouse.requestFocus()
            }
        }
    }

    private fun applyVideoFullscreenStyling() {
        val jsCode = when (videoPlatform) {
            VideoPlatform.VK -> getVkFullscreenScript()
            VideoPlatform.OK -> getOkFullscreenScript()
            else -> ""
        }

        if (jsCode.isNotEmpty()) {
            binding.vkWebView.evaluateJavascript(jsCode, null)
        }
    }

    private fun getVkFullscreenScript(): String {
        return """
            var style = document.createElement('style');
            style.innerHTML = 'video, .video_box, iframe { width: 100vw !important; height: 100vh !important; position: fixed !important; top: 0 !important; left: 0 !important; z-index: 999999 !important; object-fit: contain !important; }';
            document.head.appendChild(style);
        """.trimIndent()
    }

    private fun getOkFullscreenScript(): String {
        return """
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
    }

    private fun convertToVkEmbedUrl(url: String, startPosMs: Long): String {
        return try {
            val videoPart = url.substringAfter("video-").substringAfter("video_")
            if (videoPart.contains("_")) {
                val parts = videoPart.split("_")
                val oid = parts[0]
                val id = parts[1].split("?")[0]
                val finalOid = if (url.contains("video-")) "-$oid" else oid
                val startSeconds = startPosMs / 1000

                if (startSeconds > 0) {
                    "https://vk.com/video_ext.php?oid=$finalOid&id=$id&autoplay=1&t=${startSeconds}s"
                } else {
                    "https://vk.com/video_ext.php?oid=$finalOid&id=$id&autoplay=1"
                }
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }

    private fun convertToOkEmbedUrl(url: String, startPosMs: Long): String {
        return try {
            val startSeconds = startPosMs / 1000
            val videoId = url.substringAfterLast("/")
            "https://ok.ru/videoembed/$videoId?autoplay=1&st.start=$startSeconds"
        } catch (e: Exception) {
            url
        }
    }

    private fun setupChannelList() {
        binding.rvLiveChannels.apply {
            layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)

            val adapter = ChannelAdapter { selectedChannel ->
                currentChannel = selectedChannel
                playLiveStream(selectedChannel.videoUrl)
                binding.channelOverlay.visibility = View.GONE
            }

            this.adapter = adapter
        }
    }

    private fun updateChannelList(channels: List<Channel>?) {
        if (channels.isNullOrEmpty()) return

        val targetCategory = if (selectedCategoryId == 0L) currentChannel.category else selectedCategoryId
        val filteredChannels = channels.filter { it.category == targetCategory }

        (binding.rvLiveChannels.adapter as? ChannelAdapter)?.submitList(filteredChannels)
    }

    private fun showSourceOverlay(channel: Channel) {
        binding.sourceOverlay.visibility = View.VISIBLE
        binding.rvSources.visibility = View.GONE
        binding.tvNoSource.visibility = View.GONE

        val sources = parseChannelSources(channel)

        if (sources.isNotEmpty()) {
            binding.rvSources.visibility = View.VISIBLE
            sourceAdapter.updateData(sources)
            binding.rvSources.adapter = sourceAdapter
            binding.rvSources.requestFocus()
        } else {
            binding.tvNoSource.visibility = View.VISIBLE
            binding.tvNoSource.text = "هیچ سورسی برای این کانال وجود ندارد"
            binding.tvNoSource.requestFocus()
        }
    }

    private fun parseChannelSources(channel: Channel): List<ChannelSource> {
        return if (!channel.source.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<ChannelSource>>() {}.type
                Gson().fromJson(channel.source, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.sourceOverlay.visibility == View.VISIBLE -> {
                        binding.sourceOverlay.visibility = View.GONE
                        binding.root.requestFocus()
                    }
                    videoPlatform == VideoPlatform.LIVE_TV && binding.channelOverlay.visibility == View.VISIBLE -> {
                        binding.channelOverlay.visibility = View.GONE
                    }
                    videoPlatform == VideoPlatform.VK || videoPlatform == VideoPlatform.OK -> {
                        saveWebPositionAndFinish()
                    }
                    else -> {
                        savePositionDirectly()
                        finish()
                    }
                }
            }
        })
    }

    private fun handleMediaPlayPause(keyCode: Int): Boolean {
        if ((videoPlatform == VideoPlatform.VK || videoPlatform == VideoPlatform.OK)
            && keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            binding.vkVirtualMouse.click(binding.vkWebView)
            return true
        }
        return false
    }

    private fun handleChannelUpNavigation(keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP
            && videoPlatform == VideoPlatform.LIVE_TV
            && binding.sourceOverlay.visibility == View.GONE) {
            showSourceOverlay(currentChannel)
            return true
        }
        return false
    }

    private fun handleChannelDownNavigation(keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN
            && videoPlatform == VideoPlatform.LIVE_TV
            && binding.channelOverlay.visibility == View.GONE) {
            binding.channelOverlay.visibility = View.VISIBLE
            binding.rvLiveChannels.requestFocus()
            return true
        }
        return false
    }

    private fun saveWebPositionAndFinish() {
        val jsGetTime = """
            (function() {
                var video = document.querySelector('video');
                return video ? [video.currentTime, video.duration] : [0, 0];
            })()
        """.trimIndent()

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
                // ignore
            }
            finish()
        }
    }

    private fun savePositionDirectly(customPos: Long? = null, customDuration: Long? = null) {
        val currentPos = customPos ?: (exoPlayer?.currentPosition ?: 0L)
        val totalDur = customDuration ?: (exoPlayer?.duration ?: 0L)

        val resultIntent = Intent().apply {
            putExtra("last_pos", currentPos)
            putExtra("total_dur", totalDur)
        }
        setResult(RESULT_OK, resultIntent)

        val movieTmdbId = intent.getStringExtra("movie_tmdb_id")
        val movieJson = intent.getStringExtra("movie_json")

        if (movieTmdbId != null && movieJson != null && currentPos > MIN_SAVE_POSITION_MS) {
            saveToContinueWatchingList(movieTmdbId, movieJson, currentPos, totalDur)
        }
    }

    private fun saveToContinueWatchingList(movieTmdbId: String, movieJson: String, currentPos: Long, totalDur: Long) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        val json = prefs.getString(SAVED_CONTINUE_LIST_KEY, null)
        val type = object : TypeToken<MutableList<Movie>>() {}.type
        val list: MutableList<Movie> = if (json != null) {
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }

        val currentMovie = gson.fromJson(movieJson, Movie::class.java)
        list.removeAll { it.tmdb_id == movieTmdbId }

        currentMovie.lastPosition = currentPos
        currentMovie.totalDuration = totalDur
        list.add(0, currentMovie)

        prefs.edit().putString(SAVED_CONTINUE_LIST_KEY, gson.toJson(list)).apply()
    }

    private fun releaseAllPlayers() {
        releaseExoPlayer()
    }

    private fun releaseExoPlayer() {
        try {
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }
}