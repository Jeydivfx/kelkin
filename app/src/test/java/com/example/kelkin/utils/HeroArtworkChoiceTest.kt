package com.example.kelkin.utils

import org.junit.Assert.*
import org.junit.Test

class HeroArtworkChoiceTest {
    @Test fun acceptsPersianArabicAndEnglishPositiveNumbers() {
        assertEquals(3, parseHeroArtworkNumber("۳"))
        assertEquals(12, parseHeroArtworkNumber("١٢"))
        assertEquals(7, parseHeroArtworkNumber(" 7 "))
        listOf("", "0", "۰", "-1", "1.5", "one", "999999999999999999").forEach {
            assertNull(parseHeroArtworkNumber(it))
        }
    }

    @Test fun sameNumberIsAppliedToBothProvidersInsteadOfFallingBackToTheirFirstImage() {
        val choice = numberedHeroArtwork("film", 3, listOf("fanart-1"), listOf("tmdb-1", "tmdb-2", "tmdb-3"))!!
        assertNull(choice.fanartUrl)
        assertEquals("tmdb-3", choice.tmdbPath)
        assertEquals(3, choice.number)
        assertEquals("film", choice.movieKey)
    }

    @Test fun bothMatchingImagesArePinnedAndMissingNumbersCannotBeSaved() {
        val choice = numberedHeroArtwork("film", 2, listOf("f1", "f2"), listOf("t1", "t2"))!!
        assertEquals("f2", choice.fanartUrl)
        assertEquals("t2", choice.tmdbPath)
        assertNull(numberedHeroArtwork("film", 3, listOf("f1"), listOf("t1", "t2")))
        assertNull(numberedHeroArtwork("film", 0, listOf("f1"), listOf("t1")))
    }
}
