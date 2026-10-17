package com.example.kelkin.utils

import com.example.kelkin.DataClass.Movie

internal fun selectHeroMovie(movies: List<Movie>, key: String?): Movie? {
    val selected = key?.trim()?.takeIf { it.isNotEmpty() }
    return selected?.let { value ->
        movies.firstOrNull { it.remoteKey == value } ?: movies.firstOrNull { it.id.toString() == value }
    } ?: movies.minWithOrNull(compareBy<Movie> { it.id }.thenBy { it.remoteKey })
}
