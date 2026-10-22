package com.example.kelkin.utils

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FanartHeroSourceTest {
    private class FakeApi : FanartApi {
        var calls = 0
        var images = FanartMovieImages()
        var failure: Exception? = null
        override suspend fun movieImages(id: String, key: String): FanartMovieImages {
            calls++
            failure?.let { throw it }
            return images
        }
    }

    @Test fun onlyLandscapeBackgroundsAreSelectedAndLegacyUrlsBecomeHttps() {
        val response = Gson().fromJson("""{
          "movieposter":[{"url":"https://assets.fanart.tv/poster.jpg"}],
          "moviebackground":[
            {"url":"https://assets.fanart.tv/small.jpg","width":640,"height":360,"likes":"999"},
            {"url":"https://other.example/image.jpg","likes":"999"},
            {"url":"https://assets.fanart.tv/english.jpg","lang":"en","likes":"100"},
            {"url":"http://assets.fanart.tv/clean.jpg","lang":"00","width":"1920","height":"1080","likes":"4"}
          ]} """, FanartMovieImages::class.java)
        assertEquals("https://assets.fanart.tv/clean.jpg", selectFanartHeroImage(response))
        assertNull(selectFanartHeroImage(Gson().fromJson("""{"movieposter":[{"url":"https://assets.fanart.tv/poster.jpg"}]}""", FanartMovieImages::class.java)))
    }

    @Test fun disabledMissingKeyAndInvalidIdFallBackWithoutNetwork() = runBlocking {
        val api = FakeApi()
        val source = FanartHeroSource(api)
        assertNull(source.backdrop("550"))
        source.configure(true, "")
        assertNull(source.backdrop("550"))
        source.configure(true, "test-key")
        assertNull(source.backdrop("not-a-movie-id"))
        assertEquals(0, api.calls)
    }

    @Test fun successfulAndEmptyResultsAreCachedAndConfigurationInvalidatesThem() = runBlocking {
        var time = 0L
        val api = FakeApi()
        val source = FanartHeroSource(api) { time }
        source.configure(true, "test-key")
        assertNull(source.backdrop("550"))
        assertNull(source.backdrop("550"))
        assertEquals(1, api.calls)
        time = 10 * 60_000L + 1
        api.images = FanartMovieImages(movie4kbackground = listOf(FanartImage("https://assets.fanart.tv/4k.jpg")))
        assertEquals("https://assets.fanart.tv/4k.jpg", source.backdrop("550"))
        assertEquals("https://assets.fanart.tv/4k.jpg", source.backdrop("550"))
        assertEquals(2, api.calls)
        source.configure(true, "replacement-test-key")
        source.backdrop("550")
        assertEquals(3, api.calls)
        source.configure(false, "replacement-test-key")
        assertNull(source.backdrop("550"))
    }

    @Test fun networkFailureFallsBackAndDoesNotRepeatOnEveryRender() = runBlocking {
        val api = FakeApi().apply { failure = IOException("offline") }
        val source = FanartHeroSource(api)
        source.configure(true, "test-key")
        assertNull(source.backdrop("550"))
        assertNull(source.backdrop("550"))
        assertEquals(1, api.calls)
    }

    @Test fun cancellingAnOldHeroIsNotConvertedIntoAFallbackResult() = runBlocking {
        val api = FakeApi().apply { failure = CancellationException("hero changed") }
        val source = FanartHeroSource(api)
        source.configure(true, "test-key")
        try {
            source.backdrop("550")
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun numberedAndPinnedChoicesRespectTheEnabledSwitch() = runBlocking {
        val api = FakeApi().apply {
            images = FanartMovieImages(moviebackground = listOf(
                FanartImage("https://assets.fanart.tv/first.jpg", likes = "2"),
                FanartImage("https://assets.fanart.tv/second.jpg", likes = "1")))
        }
        val source = FanartHeroSource(api)
        source.configure(true, "test-key")
        assertEquals("https://assets.fanart.tv/second.jpg", source.backdrop("550", 2))
        assertNull(source.backdrop("550", 3))
        assertEquals(1, api.calls)
        assertEquals("https://assets.fanart.tv/saved.jpg", source.backdrop("550", 2, "https://assets.fanart.tv/saved.jpg"))
        assertEquals(1, api.calls)
        source.configure(false, "test-key")
        assertNull(source.backdrop("550", 2, "https://assets.fanart.tv/saved.jpg"))
    }
}
