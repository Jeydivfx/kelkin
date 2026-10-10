package com.example.kelkin

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.utils.WatchHistory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchHistoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val history get() = WatchHistory(context)
    @Before fun reset() { context.getSharedPreferences("kelkin_prefs", Context.MODE_PRIVATE).edit().remove("saved_continue_list").commit() }
    @Test fun failedStartPreservesResumePoint() {
        val movie = Movie(id = 1, remoteKey = "a")
        history.save(movie, 45000, 120000)
        history.save(movie, 0, 0)
        assertEquals(45000L, history.load().single().lastPosition)
    }
    @Test fun completionRemovesMovieWithoutRemovingSeriesWithSameId() {
        val movie = Movie(id = 1, tmdb_id = "42")
        val series = movie.copy(contentType = "series", seasonKey = "2", episodeKey = "ep3", videoUrl1 = "https://example.org/ep3.mp4")
        history.save(movie, 45000, 120000)
        history.save(series, 30000, 120000)
        history.save(movie, 120000, 120000)
        val remaining = history.load().single()
        assertEquals("series", remaining.contentType)
        assertEquals("2", remaining.seasonKey)
        assertEquals("ep3", remaining.episodeKey)
        assertEquals(series.videoUrl1, remaining.videoUrl1)
    }
}
