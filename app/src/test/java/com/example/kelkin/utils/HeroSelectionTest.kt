package com.example.kelkin.utils

import com.example.kelkin.DataClass.Movie
import org.junit.Assert.*
import org.junit.Test

class HeroSelectionTest {
    private val first = Movie(id = 3, remoteKey = "movie_a")
    private val featured = Movie(id = 8, remoteKey = "movie_b")
    @Test fun remoteKeySelectsTheFeaturedFilm() {
        assertEquals(featured, selectHeroMovie(listOf(first, featured, Movie(id = 20)), "movie_b"))
    }
    @Test fun numericIdsAreAccepted() {
        assertEquals(featured, selectHeroMovie(listOf(first, featured), "8"))
    }
    @Test fun addingNewMoviesDoesNotReplaceFallbackHero() {
        assertEquals(first, selectHeroMovie(listOf(featured, first, Movie(id = 99)), null))
        assertEquals(first, selectHeroMovie(listOf(first, featured), "deleted"))
    }
    @Test fun emptyCatalogHasNoHero() { assertNull(selectHeroMovie(emptyList(), "movie_b")) }
}
