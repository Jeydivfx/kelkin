package com.example.kelkin.utils

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.kelkin.DataClass.StreamHeaders
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class KelkinLivePlayer(
    private val context: Context,
    private val vlcVideoLayout: VLCVideoLayout,
    private val onError: (String) -> Unit
) {
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    init {
        setupPlayer()
    }

    private fun setupPlayer() {
        val options = ArrayList<String>()
        options.add("--network-caching=2000")

        libVLC = LibVLC(context, options)
        mediaPlayer = MediaPlayer(libVLC)

        mediaPlayer?.setEventListener { event ->
            if (event.type == MediaPlayer.Event.Playing) {
                timeoutRunnable?.let { handler.removeCallbacks(it) }
            } else if (event.type == MediaPlayer.Event.EncounteredError) {
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                onError("خطا در پخش کانال!")
            }
        }
        mediaPlayer?.attachViews(vlcVideoLayout, null, false, false)
    }

    fun play(url: String, headers: StreamHeaders) {
        timeoutRunnable?.let { handler.removeCallbacks(it) }

        timeoutRunnable = Runnable {
            onError("کانال در دسترس نیست!")
        }
        handler.postDelayed(timeoutRunnable!!, 20000)

        val media = Media(libVLC, Uri.parse(url))

        media.addOption(":http-referrer=${headers.referrer}")
        media.addOption(":http-user-agent=${headers.user_agent}")
        media.addOption(":http-origin=${headers.origin}")

        mediaPlayer?.media = media
        mediaPlayer?.play()
    }

    fun release() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        mediaPlayer?.stop()
        mediaPlayer?.release()
        libVLC?.release()
    }
}