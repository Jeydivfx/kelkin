package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.Adapter.ChannelAdapter
import com.example.kelkin.Adapter.SourceAdapter
import com.example.kelkin.DataClass.*
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.ActivityPlayerBinding
import com.example.kelkin.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {
    internal val binding by lazy { ActivityPlayerBinding.inflate(layoutInflater) }
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private lateinit var model: HomeViewModel
    private lateinit var sourceAdapter: SourceAdapter
    private var channel = Channel()
    private var category = 0L
    private var currentUrl = ""
    private var headers = StreamHeaders()
    private var controllerRevealKey = -1
    private var platform = StreamUrls.Platform.DIRECT
    private var live = false
    private var resumePlayback = true
    private var stopped = false
    private var released = false
    private var finishingWeb = false
    private var centerPressed = false
    private var generation = 0
    private var attempts = 0
    private var retryJob: Job? = null
    private var connectionJob: Job? = null
    private var checkpointJob: Job? = null
    private var lastWebPosition = 0L
    private var lastWebDuration = 0L
    private var webPaused = false
    private var initialPosition = 0L
    private var fullscreenWebView: View? = null
    private var fullscreenWebCallback: WebChromeClient.CustomViewCallback? = null
    private var mobileCloseAction: View? = null
    private val history by lazy { WatchHistory(this) }
    private val card by lazy { runCatching { Gson().fromJson(intent.getStringExtra("movie_json"), Movie::class.java) }.getOrNull() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!BuildConfig.IS_TV) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            androidx.core.view.WindowInsetsControllerCompat(window, binding.root).apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout())
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                insets
            }
            val actions = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.RIGHT
                elevation = 24 * resources.displayMetrics.density
            }
            fun touchAction(label: String, click: () -> Unit): android.widget.Button {
                val button = android.widget.Button(this).apply {
                    text = label
                    backgroundTintList = null
                    setBackgroundResource(R.drawable.sl_button_high_contrast)
                    setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
                    textSize = 12f
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.yekanbakh_regular)
                    setOnClickListener { click() }
                }
                actions.addView(button, android.widget.LinearLayout.LayoutParams(-2, (48 * resources.displayMetrics.density).toInt()))
                return button
            }
            touchAction("کانال‌ها") {
                binding.sourceOverlay.visibility = View.GONE
                binding.channelOverlay.visibility = View.VISIBLE
                mobileCloseAction?.visibility = View.VISIBLE
                val adapter = binding.rvLiveChannels.adapter as? ChannelAdapter
                focusRow(binding.rvLiveChannels, adapter?.currentList?.indexOfFirst { it.id == channel.id }?.coerceAtLeast(0) ?: 0)
            }
            touchAction("سورس‌ها") { showSources() }
            mobileCloseAction = touchAction("بستن فهرست") { hideOverlays() }.apply { visibility = View.GONE }
            val liveActionCount = actions.childCount
            touchAction("بازگشت") { finishPlayback() }
            val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(-2, -2).apply {
                topToTop = 0; endToEnd = 0
            }
            binding.root.addView(actions, params)
            binding.root.post {
                for (index in 0 until liveActionCount) actions.getChildAt(index).visibility = if (live) View.VISIBLE else View.GONE
                mobileCloseAction?.visibility = View.GONE
            }
        }
        channel = runCatching { Gson().fromJson(savedInstanceState?.getString("channel") ?: intent.getStringExtra("channel_json"), Channel::class.java) }.getOrNull() ?: Channel()
        currentUrl = savedInstanceState?.getString("url") ?: intent.getStringExtra("video_url").orEmpty()
        category = intent.getLongExtra("selected_category_id", 0L)
        live = intent.getBooleanExtra("is_live", false)
        initialPosition = savedInstanceState?.getLong("position") ?: intent.getLongExtra("start_pos", 0L)
        resumePlayback = savedInstanceState?.getBoolean("playing") ?: true
        model = ViewModelProvider(this)[HomeViewModel::class.java]
        sourceAdapter = SourceAdapter(emptyList()) { source ->
            hideOverlays(); play(source.url)
        }
        binding.rvSources.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvSources.adapter = sourceAdapter
        binding.rvLiveChannels.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvLiveChannels.adapter = ChannelAdapter { selected ->
            channel = selected; hideOverlays(); play(selected.videoUrl)
        }
        binding.btnPlayerRetry.setOnClickListener { attempts = 0; play(currentUrl, lastPosition()) }
        binding.btnPlayerSources.setOnClickListener { hideStatus(); showSources() }
        binding.btnPlayerBack.setOnClickListener { finishPlayback() }
        model.streamHeaders.observe(this) { next ->
            if (next != headers) {
                headers = next ?: StreamHeaders()
                // Reconfigure only this stream, and preserve the user's pause state.
                if (live && platform == StreamUrls.Platform.DIRECT && player != null && !released) {
                    resumePlayback = player?.playWhenReady == true
                    play(currentUrl, lastPosition(), resetAttempts = false)
                }
            }
        }
        model.channels.observe(this) { all ->
            val filtered = if (category > 0) all.orEmpty().filter { it.category == category } else all.orEmpty()
            (binding.rvLiveChannels.adapter as ChannelAdapter).submitList(filtered)
        }
        play(currentUrl, initialPosition, resetAttempts = true)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullscreenWebView != null -> hideWebFullscreen()
                    binding.sourceOverlay.visibility == View.VISIBLE || binding.channelOverlay.visibility == View.VISIBLE -> hideOverlays()
                    binding.playerView.isControllerFullyVisible -> binding.playerView.hideController()
                    else -> finishPlayback()
                }
            }
        })
    }
    private fun play(url: String, position: Long = 0L, resetAttempts: Boolean = true) {
        if (released || stopped) return
        if (fullscreenWebView != null) hideWebFullscreen()
        retryJob?.cancel(); connectionJob?.cancel()
        generation++
        if (resetAttempts) attempts = 0
        currentUrl = url.trim()
        if (!StreamUrls.isValid(currentUrl)) { showError("لینک پخش معتبر نیست؛ سورس دیگری انتخاب کنید."); return }
        platform = StreamUrls.platform(currentUrl)
        hideStatus()
        if (platform == StreamUrls.Platform.DIRECT) playNative(position, resumePlayback)
        else playWeb(position)
    }
    private fun playNative(position: Long, playing: Boolean) {
        if (released || stopped) return
        connectionJob?.cancel()
        binding.vkPlayerContainer.visibility = View.GONE
        binding.playerView.visibility = if (live) View.GONE else View.VISIBLE
        binding.liveContainer.visibility = if (live) View.VISIBLE else View.GONE
        binding.vkWebView.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();});", null)
        session?.release(); session = null
        player?.release()
        val dataSource = DefaultHttpDataSource.Factory()
            .setUserAgent(headers.user_agent.ifBlank { "Kelkin/1.80" })
            .setConnectTimeoutMs(10_000).setReadTimeoutMs(15_000).setAllowCrossProtocolRedirects(false)
            .setDefaultRequestProperties(buildMap {
                if (headers.referrer.isNotBlank()) put("Referer", headers.referrer)
                if (headers.origin.isNotBlank()) put("Origin", headers.origin)
            })
        val selector = DefaultTrackSelector(this).apply {
            parameters = buildUponParameters().setPreferredAudioLanguages("fa", "en").setPreferredTextLanguages("fa", "en").setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
        }
        val native = ExoPlayer.Builder(this).setTrackSelector(selector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource)).build()
        player = native
        session = MediaSession.Builder(this, native).build()
        native.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        native.setHandleAudioBecomingNoisy(true)
        val playerView = if (live) {
            binding.liveContainer.removeAllViews()
            val root = layoutInflater.inflate(R.layout.custom_live_player, binding.liveContainer, true)
            root.findViewById<PlayerView>(R.id.livePlayerView)
        } else binding.playerView
        playerView.findViewById<android.widget.ProgressBar>(androidx.media3.ui.R.id.exo_buffering)?.indeterminateTintList =
            android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.yellow))
        playerView.player = native
        playerView.useController = !live || !BuildConfig.IS_TV
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        if (!live) {
            playerView.setShowSubtitleButton(true)
            playerView.controllerShowTimeoutMs = 5000
            val preferences = getSharedPreferences("subtitle_prefs", MODE_PRIVATE)
            val color = when (preferences.getInt("sub_color_index", 0)) { 1 -> android.graphics.Color.WHITE; 2 -> android.graphics.Color.GREEN; else -> android.graphics.Color.YELLOW }
            playerView.subtitleView?.apply {
                setApplyEmbeddedStyles(false)
                setStyle(androidx.media3.ui.CaptionStyleCompat(color, 0xC0000000.toInt(), android.graphics.Color.TRANSPARENT, androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null))
                setFractionalTextSize(when (preferences.getInt("sub_size_index", 1)) { 0 -> 0.035f; 2 -> 0.065f; else -> 0.05f })
            }
        }
        val token = generation
        native.addListener(object : Player.Listener {
            override fun onTrackSelectionParametersChanged(parameters: androidx.media3.common.TrackSelectionParameters) {
                if (native !== player || released) return
                val normalized = com.example.kelkin.utils.normalizeSubtitleSelection(parameters)
                if (normalized != parameters) native.trackSelectionParameters = normalized
                if (normalized.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)) {
                    playerView.subtitleView?.setCues(emptyList())
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (native !== player || token != generation || released) return
                when (state) {
                    Player.STATE_READY -> { attempts = 0; connectionJob?.cancel(); hideStatus() }
                    Player.STATE_ENDED -> { checkpoint(ended = true); showError("پخش تمام شد؛ با بازگشت عنوان دیگری انتخاب کنید.", canRetry = !live) }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                if (native !== player || token != generation || released || stopped) return
                connectionJob?.cancel()
                val retryable = error.errorCode in listOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
                if (live && retryable && attempts < 3) {
                    attempts++
                    showLoading("اتصال قطع شد؛ تلاش $attempts از ۳…")
                    retryJob = lifecycleScope.launch {
                        delay(2000L shl (attempts - 1))
                        if (token == generation && !stopped) play(currentUrl, resetAttempts = false)
                    }
                } else showError(when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "این سورس HTTP است؛ از لینک HTTPS یا سورس مجاز استفاده کنید."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "سرور لینک را نپذیرفت یا لینک منقضی شده؛ سورس دیگری انتخاب کنید."
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "فرمت این ویدیو روی دستگاه شما پشتیبانی نمی‌شود."
                    else -> "پخش انجام نشد؛ اتصال اینترنت را بررسی کنید یا سورس دیگری انتخاب کنید."
                })
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (native === player && !stopped) resumePlayback = native.playWhenReady
            }
        })
        native.setMediaItem(MediaItem.fromUri(currentUrl))
        if (!live && position > 0) native.seekTo(position)
        native.prepare(); native.playWhenReady = playing
        watchConnection(token)
        if (live) binding.root.requestFocus() else playerView.requestFocus()
    }
    private fun playWeb(position: Long) {
        session?.release(); session = null
        player?.release(); player = null
        binding.playerView.visibility = View.GONE
        binding.liveContainer.visibility = View.GONE
        binding.vkPlayerContainer.visibility = View.VISIBLE
        initialPosition = position
        lastWebPosition = position
        binding.vkWebView.apply {
            isFocusable = !BuildConfig.IS_TV
            descendantFocusability = if (BuildConfig.IS_TV) android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS else android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
            binding.vkVirtualMouse.visibility = if (BuildConfig.IS_TV) View.VISIBLE else View.GONE
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            // Preserve the installed WebView engine version instead of impersonating Chrome 126.
            settings.userAgentString = WebSettings.getDefaultUserAgent(context).replace("; wv", "")
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.allowFileAccess = false; settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val target = request?.url?.toString().orEmpty()
                    // Embedded players may navigate their own frames through CDN/auth hosts.
                    if (!StreamUrls.isValid(target)) return true
                    if (request?.isForMainFrame == false) return false
                    return StreamUrls.platform(target) == StreamUrls.Platform.DIRECT
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (released || stopped) return
                    val js = """
                        (function(){
                            function style(){
                                var v=document.querySelector('video');
                                if(!v) return;
                                // Do not cover the provider's play button with an unready black video.
                                if(v.readyState>=2 && !v.dataset.kelkinStyled){
                                    v.dataset.kelkinStyled='1';
                                    v.style.cssText+=';position:fixed;inset:0;width:100vw;height:100vh;object-fit:contain;background:black;z-index:999999';
                                }
                                if(!v.dataset.kelkin){
                                    v.dataset.kelkin='1';
                                    v.addEventListener('loadeddata',style,{once:true});
                                    v.addEventListener('playing',style,{once:true});
                                    v.addEventListener('ended',function(){window.kelkinEnded=true;});
                                    var seek=function(){if(${initialPosition / 1000.0}>0) v.currentTime=${initialPosition / 1000.0};};
                                    if(v.readyState>0)seek();else v.addEventListener('loadedmetadata',seek,{once:true});
                                }
                            }
                            style();new MutationObserver(style).observe(document.documentElement,{childList:true,subtree:true});
                        })();
                    """.trimIndent()
                    evaluateJavascript(js, null)
                    connectionJob?.cancel(); hideStatus()
                    if (!resumePlayback) webCommand("pause")
                    if (BuildConfig.IS_TV) binding.vkVirtualMouse.requestFocus()
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true && !stopped) showError("صفحهٔ پخش باز نشد (کد ${error?.errorCode})؛ اتصال یا Android System WebView را بررسی کنید.")
                }
                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                    if (request?.isForMainFrame == true && !stopped) showError("سورس وب پاسخ ${response?.statusCode} داد؛ ممکن است لینک حذف شده یا دسترسی محدود باشد.")
                }
            }
            webChromeClient = if (BuildConfig.IS_TV) WebChromeClient() else object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null || fullscreenWebView != null) { callback?.onCustomViewHidden(); return }
                    fullscreenWebView = view
                    fullscreenWebCallback = callback
                    binding.vkWebView.visibility = View.GONE
                    binding.vkPlayerContainer.addView(view, android.widget.FrameLayout.LayoutParams(-1, -1))
                }
                override fun onHideCustomView() { hideWebFullscreen() }
            }
            loadUrl(StreamUrls.embed(currentUrl, position))
        }
        showLoading("در حال بازکردن ویدیو…")
        watchConnection(generation)
    }
    private fun webCommand(command: String) {
        val operation = when (command) {
            "pause" -> "v.pause();"
            "play" -> "v.play();"
            "forward" -> "v.currentTime=Math.min(v.duration||v.currentTime+10,v.currentTime+10);"
            "backward" -> "v.currentTime=Math.max(0,v.currentTime-10);"
            else -> "if(v.paused)v.play();else v.pause();"
        }
        binding.vkWebView.evaluateJavascript("(function(){var v=document.querySelector('video');if(!v)return null;$operation return !v.paused;})()") { result ->
            if (!released && result in listOf("true", "false")) { webPaused = result == "false"; if (!stopped) resumePlayback = !webPaused }
        }
    }
    private fun hideWebFullscreen() {
        fullscreenWebView?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        fullscreenWebView = null
        val callback = fullscreenWebCallback
        fullscreenWebCallback = null
        binding.vkWebView.visibility = View.VISIBLE
        callback?.onCustomViewHidden()
    }
    private fun watchConnection(token: Int) {
        connectionJob?.cancel()
        connectionJob = lifecycleScope.launch {
            delay(25_000)
            if (token == generation && !stopped) showError("زمان اتصال تمام شد؛ دوباره تلاش کنید یا سورس دیگری انتخاب کنید.")
        }
    }
    private fun showLoading(message: String) {
        binding.playerView.controllerAutoShow = false
        binding.playerView.hideController()
        binding.playerStatusPanel.visibility = View.VISIBLE
        binding.playerStatusText.text = message
        binding.playerStatusProgress.visibility = View.VISIBLE
        binding.playerStatusActions.visibility = View.GONE
    }
    private fun showError(message: String, canRetry: Boolean = true) {
        if (released || isFinishing) return
        connectionJob?.cancel()
        binding.playerView.controllerAutoShow = false
        binding.playerView.hideController()
        binding.playerStatusPanel.visibility = View.VISIBLE
        binding.playerStatusText.text = message
        binding.playerStatusProgress.visibility = View.GONE
        binding.playerStatusActions.visibility = View.VISIBLE
        binding.btnPlayerRetry.visibility = if (canRetry) View.VISIBLE else View.GONE
        binding.btnPlayerSources.visibility = if (live && sources().isNotEmpty()) View.VISIBLE else View.GONE
        (if (canRetry) binding.btnPlayerRetry else binding.btnPlayerBack).requestFocus()
    }
    private fun hideStatus() {
        binding.playerStatusPanel.visibility = View.GONE
        binding.playerView.controllerAutoShow = true
    }
    private fun sources(): List<ChannelSource> = runCatching {
        val parsed = Gson().fromJson<com.google.gson.JsonElement>(channel.source, com.google.gson.JsonElement::class.java)
        val array = when { parsed == null -> null; parsed.isJsonArray -> parsed; parsed.isJsonObject -> com.google.gson.JsonArray().apply { parsed.asJsonObject.entrySet().forEach { add(it.value) } }; else -> null }
        val type = object : TypeToken<List<ChannelSource>>() {}.type
        Gson().fromJson<List<ChannelSource>>(array, type).orEmpty().filter { StreamUrls.isValid(it.url) }
    }.getOrDefault(emptyList())
    private fun showSources() {
        mobileCloseAction?.visibility = View.VISIBLE
        hideStatus()
        binding.channelOverlay.visibility = View.GONE
        binding.sourceOverlay.visibility = View.VISIBLE
        val list = sources()
        sourceAdapter.updateData(list)
        binding.tvNoSource.visibility = View.VISIBLE
        binding.tvNoSource.text = if (list.isEmpty()) "سورس دیگری برای این کانال ثبت نشده؛ Back برای بازگشت" else "انتخاب سورس پخش"
        binding.rvSources.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        if (list.isEmpty()) { binding.tvNoSource.isFocusable = true; binding.tvNoSource.requestFocus() }
        else focusRow(binding.rvSources, list.indexOfFirst { it.url == currentUrl }.coerceAtLeast(0))
    }
    private fun hideOverlays() {
        mobileCloseAction?.visibility = View.GONE
        binding.sourceOverlay.visibility = View.GONE; binding.channelOverlay.visibility = View.GONE
        if (!BuildConfig.IS_TV) return
        if (platform == StreamUrls.Platform.DIRECT) { if (live) binding.root.requestFocus() else binding.playerView.requestFocus() }
        else binding.vkVirtualMouse.requestFocus()
    }
    private fun focusRow(list: RecyclerView, position: Int) {
        list.scrollToPosition(position)
        if (!BuildConfig.IS_TV) return
        list.post { list.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() ?: list.requestFocus() }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val overlay = binding.sourceOverlay.visibility == View.VISIBLE || binding.channelOverlay.visibility == View.VISIBLE || binding.playerStatusActions.visibility == View.VISIBLE && binding.playerStatusPanel.visibility == View.VISIBLE
        if (!overlay) {
            if (live && keyCode == KeyEvent.KEYCODE_MENU) { showSources(); return true }
            val media = keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND)
            if (media) {
                if (platform != StreamUrls.Platform.DIRECT) webCommand(when (keyCode) { KeyEvent.KEYCODE_MEDIA_PLAY -> "play"; KeyEvent.KEYCODE_MEDIA_PAUSE -> "pause"; KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "forward"; KeyEvent.KEYCODE_MEDIA_REWIND -> "backward"; else -> "toggle" })
                else player?.let { p -> when (keyCode) { KeyEvent.KEYCODE_MEDIA_PLAY -> p.play(); KeyEvent.KEYCODE_MEDIA_PAUSE -> p.pause(); KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> p.seekForward(); KeyEvent.KEYCODE_MEDIA_REWIND -> p.seekBack(); else -> if (p.playWhenReady) p.pause() else p.play() } }
                return true
            }
            if (live) when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { showSources(); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    binding.channelOverlay.visibility = View.VISIBLE
                    val adapter = binding.rvLiveChannels.adapter as ChannelAdapter
                    focusRow(binding.rvLiveChannels, adapter.currentList.indexOfFirst { it.id == channel.id }.coerceAtLeast(0)); return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER -> { centerPressed = true; return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val overlay = binding.sourceOverlay.visibility == View.VISIBLE || binding.channelOverlay.visibility == View.VISIBLE || binding.playerStatusPanel.visibility == View.VISIBLE
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == controllerRevealKey) {
            controllerRevealKey = -1
            return true
        }
        val navigationKey = event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU)
        if (!overlay && !live && platform == StreamUrls.Platform.DIRECT && navigationKey &&
            event.action == KeyEvent.ACTION_DOWN && !binding.playerView.isControllerFullyVisible) {
            controllerRevealKey = event.keyCode
            binding.playerView.showController()
            binding.playerView.requestFocus()
            return true
        }
        val media = event.keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND)
        val liveControl = live && event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MENU)
        if (!overlay && (media || liveControl)) {
            if (event.action == KeyEvent.ACTION_DOWN && onKeyDown(event.keyCode, event)) return true
            if (event.action == KeyEvent.ACTION_UP && (media || onKeyUp(event.keyCode, event))) return true
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && centerPressed) {
            centerPressed = false
            if (event.eventTime - event.downTime >= 500) toggleFavorite()
            else if (platform != StreamUrls.Platform.DIRECT) webCommand("toggle") else player?.let { if (it.playWhenReady) it.pause() else it.play() }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
    private fun toggleFavorite() {
        val prefs = getSharedPreferences("kelkin_prefs", MODE_PRIVATE)
        val favorites = prefs.getStringSet("favorite_channels", emptySet()).orEmpty().toMutableSet()
        val key = channel.id.toString()
        val added = favorites.add(key)
        if (!added) favorites.remove(key)
        prefs.edit().putStringSet("favorite_channels", favorites).apply()
        Toast.makeText(this, if (added) "کانال نشان شد" else "کانال از نشان‌شده‌ها حذف شد", Toast.LENGTH_SHORT).show()
    }
    private fun lastPosition() = if (platform == StreamUrls.Platform.DIRECT) player?.currentPosition ?: initialPosition else lastWebPosition
    private fun checkpoint(ended: Boolean = false) {
        if (live || released) return
        if (platform == StreamUrls.Platform.DIRECT) persist(player?.currentPosition ?: 0L, player?.duration ?: 0L, ended)
        else binding.vkWebView.evaluateJavascript("(function(){var v=document.querySelector('video');return v?[v.currentTime,v.duration,v.ended,v.paused]:null;})()") { result ->
            if (!released) runCatching {
                val values = Gson().fromJson(result, com.google.gson.JsonArray::class.java) ?: return@runCatching
                lastWebPosition = (values[0].asDouble * 1000).toLong()
                lastWebDuration = if (values[1].isJsonNull) 0 else (values[1].asDouble * 1000).toLong()
                if (!stopped) webPaused = values[3].asBoolean
                persist(lastWebPosition, lastWebDuration, ended || values[2].asBoolean)
            }
        }
    }
    private fun persist(position: Long, duration: Long, ended: Boolean = false) {
        setResult(RESULT_OK, Intent().putExtra("last_pos", position).putExtra("total_dur", duration.coerceAtLeast(0)))
        card?.let { history.save(it, position, duration.coerceAtLeast(0), ended) }
    }
    private fun finishPlayback() {
        if (finishingWeb) return
        if (platform == StreamUrls.Platform.DIRECT || live) { checkpoint(); finish(); return }
        finishingWeb = true
        checkpoint()
        lifecycleScope.launch { delay(500); persist(lastWebPosition, lastWebDuration); finish() }
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("url", currentUrl); out.putString("channel", Gson().toJson(channel))
        out.putLong("position", lastPosition())
        out.putBoolean("playing", if (stopped) resumePlayback else if (platform == StreamUrls.Platform.DIRECT) player?.playWhenReady == true else !webPaused)
        super.onSaveInstanceState(out)
    }
    override fun onPause() {
        checkpoint()
        resumePlayback = if (platform == StreamUrls.Platform.DIRECT) player?.playWhenReady == true else !webPaused
        stopped = true
        retryJob?.cancel(); connectionJob?.cancel(); checkpointJob?.cancel()
        player?.pause()
        if (platform != StreamUrls.Platform.DIRECT) { webCommand("pause"); binding.vkWebView.onPause() }
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        val wasStopped = stopped
        stopped = false
        if (platform == StreamUrls.Platform.DIRECT) { if (wasStopped && resumePlayback) player?.play() }
        else { binding.vkWebView.onResume(); if (wasStopped && resumePlayback) webCommand("play") }
        checkpointJob?.cancel()
        checkpointJob = lifecycleScope.launch { while (isActive) { delay(5000); checkpoint() } }
    }
    override fun onDestroy() {
        hideWebFullscreen()
        released = true
        generation++
        retryJob?.cancel(); connectionJob?.cancel(); checkpointJob?.cancel()
        session?.release(); session = null
        binding.playerView.player = null
        player?.release(); player = null
        binding.rvSources.adapter = null; binding.rvLiveChannels.adapter = null
        binding.vkWebView.apply { stopLoading(); loadUrl("about:blank"); destroy() }
        super.onDestroy()
    }
}
