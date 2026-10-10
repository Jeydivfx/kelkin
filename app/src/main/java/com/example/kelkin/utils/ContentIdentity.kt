package com.example.kelkin.utils

import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.Series

fun Movie.identity(): String = "${contentType.ifBlank { "movie" }}:${remoteKey.ifBlank { tmdb_id.ifBlank { id.toString() } }}"
fun Series.identity(): String = "series:${remoteKey.ifBlank { tmdb_id.ifBlank { id.toString() } }}"
fun metadataKey(type: String, tmdbId: String) = "$type:$tmdbId"

fun Series.asCard() = Movie(
    id = id, name_fa = name_fa, description_fa = description_fa, category = category,
    tmdb_id = tmdb_id, posterUrl = posterUrl, language = language, quality = quality,
    voteAverage = voteAverage, contentType = "series", remoteKey = remoteKey
)

fun stableContentId(key: String): Long {
    var hash = -3750763034362895579L
    key.forEach { hash = (hash xor it.code.toLong()) * 1099511628211L }
    return hash
}

object ImageUrls {
    fun poster(path: String?): String? = tmdb(path, "w500")
    fun backdrop(path: String?): String? = tmdb(path, "w1280")
    private fun tmdb(path: String?, size: String): String? {
        val value = path?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("https://") || value.startsWith("http://")) return value
        return "https://image.tmdb.org/t/p/$size/${value.trimStart('/')}"
    }
}
