package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.databinding.ActivityPlayerBinding
import com.google.gson.Gson

class PlayerActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPlayerBinding.inflate(layoutInflater) }
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val startPos = intent.getLongExtra("start_pos", 0L)

        setupPlayer(videoUrl, startPos)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                savePositionDirectly()
                finish()
            }
        })
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer(url: String, startPos: Long) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer

            //Keep the player awake when a video is playing
            binding.playerView.keepScreenOn = true

            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)

            if (startPos > 0) {
                exoPlayer.seekTo(startPos)
            }

            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
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
            Log.e("ContinueWatching", "Error: Intent data is NULL! Check DetailFragment extras.")
            return
        }

        if (currentPos > 2000) {
            val prefs = getSharedPreferences("kelkin_prefs", MODE_PRIVATE)
            val gson = com.google.gson.Gson()

            val json = prefs.getString("saved_continue_list", null)
            val type = object : com.google.gson.reflect.TypeToken<MutableList<com.example.kelkin.DataClass.Movie>>() {}.type
            val list: MutableList<com.example.kelkin.DataClass.Movie> = if (json != null) {
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }

            val currentMovie = gson.fromJson(movieJson, com.example.kelkin.DataClass.Movie::class.java)
            list.removeAll { it.tmdb_id == movieTmdbId }

            currentMovie.lastPosition = currentPos
            currentMovie.totalDuration = totalDur
            list.add(0, currentMovie)

        } else {
            Log.w("ContinueWatching", "Step 4: Position too low ($currentPos), not saving.")
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