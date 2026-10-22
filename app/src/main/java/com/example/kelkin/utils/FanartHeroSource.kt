package com.example.kelkin.utils

import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class HeroFanartBackdrop(val movieId: String, val url: String?, val number: Int = 1)

internal data class FanartImage(val url: String? = null, val lang: String? = null,
    val likes: String? = null, val width: String? = null, val height: String? = null)
internal data class FanartMovieImages(val moviebackground: List<FanartImage>? = null,
    val movie4kbackground: List<FanartImage>? = null)

internal interface FanartApi {
    @GET("movies/{id}")
    suspend fun movieImages(@Path("id") id: String, @Header("api-key") key: String): FanartMovieImages

    companion object {
        fun create(): FanartApi = Retrofit.Builder()
            .baseUrl("https://webservice.fanart.tv/v3.2/")
            .client(OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(FanartApi::class.java)
    }
}

internal fun fanartBackgrounds(images: FanartMovieImages): List<String> {
    return (images.moviebackground.orEmpty() + images.movie4kbackground.orEmpty())
        .mapNotNull { image ->
            val url = image.url?.toHttpUrlOrNull() ?: return@mapNotNull null
            if (url.host != "fanart.tv" && !url.host.endsWith(".fanart.tv")) return@mapNotNull null
            val width = image.width?.toIntOrNull() ?: 0
            val height = image.height?.toIntOrNull() ?: 0
            if (width > 0 && height > 0 && (width < 1280 || height < 720 || width.toFloat() / height !in 1.5f..2.1f)) return@mapNotNull null
            image.copy(url = url.newBuilder().scheme("https").build().toString())
        }.sortedWith(compareByDescending<FanartImage> { it.lang.isNullOrBlank() || it.lang == "00" }
            .thenByDescending { it.likes?.toIntOrNull() ?: 0 }
            .thenByDescending { (it.width?.toLongOrNull() ?: 0) * (it.height?.toLongOrNull() ?: 0) })
        .mapNotNull { it.url }.distinct()
}

internal fun selectFanartHeroImage(images: FanartMovieImages): String? = fanartBackgrounds(images).firstOrNull()

/** Used only for the selected Home hero; no catalog poster or detail artwork is changed. */
internal class FanartHeroSource(private val api: FanartApi = FanartApi.create(),
    private val now: () -> Long = System::currentTimeMillis) {
    private data class Cached(val expires: Long, val urls: List<String>)
    private val cache = mutableMapOf<String, Cached>()
    private var enabled = false
    private var apiKey = ""

    fun configure(enabled: Boolean, key: String): Boolean {
        val trimmed = key.trim()
        if (this.enabled == enabled && apiKey == trimmed) return false
        this.enabled = enabled
        apiKey = trimmed
        cache.clear()
        return true
    }

    suspend fun backgrounds(movieId: String): List<String> {
        if (!enabled || apiKey.isBlank() || movieId.toLongOrNull()?.let { it > 0 } != true) return emptyList()
        cache[movieId]?.takeIf { it.expires > now() }?.let { return it.urls }
        return try {
            val urls = fanartBackgrounds(api.movieImages(movieId, apiKey))
            cache[movieId] = Cached(now() + if (urls.isNotEmpty()) 6 * 60 * 60_000L else 10 * 60_000L, urls)
            urls
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            // Never log a request or key; any HTTP/authentication/network error falls back to TMDB.
            cache[movieId] = Cached(now() + 60_000L, emptyList())
            emptyList()
        }
    }

    suspend fun backdrop(movieId: String, number: Int = 1, pinnedUrl: String? = null): String? {
        if (!enabled || apiKey.isBlank() || number <= 0 || movieId.toLongOrNull()?.let { it > 0 } != true) return null
        if (pinnedUrl != null) {
            val pinned = selectFanartHeroImage(FanartMovieImages(moviebackground = listOf(FanartImage(pinnedUrl))))
            if (pinned != null) return pinned
        }
        return numberedHeroImage(backgrounds(movieId), number)
    }
}
