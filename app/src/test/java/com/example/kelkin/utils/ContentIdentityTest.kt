package com.example.kelkin.utils

import com.example.kelkin.DataClass.Movie
import org.junit.Assert.*
import org.junit.Test

class ContentIdentityTest {
    @Test fun movieAndSeriesWithSameIdRemainSeparate() {
        val movie = Movie(id = 42, tmdb_id = "42")
        val series = movie.copy(contentType = "series")
        assertNotEquals(movie.identity(), series.identity())
        assertNotEquals(stableContentId(movie.identity()), stableContentId(series.identity()))
    }
    @Test fun metadataAndDatabaseOrderDoNotChangeRemoteIdentity() {
        val original = Movie(id = 1, remoteKey = "firebase-push-key", tmdb_id = "42")
        val updated = original.copy(id = 999, posterUrl = "/poster.jpg", voteAverage = 8.5)
        assertEquals(original.identity(), updated.identity())
    }
    @Test fun imageUrlsPreserveExternalPosters() {
        assertNull(ImageUrls.poster(""))
        assertEquals("https://example.org/p.jpg", ImageUrls.poster("https://example.org/p.jpg"))
        assertEquals("https://image.tmdb.org/t/p/w500/p.jpg", ImageUrls.poster("/p.jpg"))
    }
}
