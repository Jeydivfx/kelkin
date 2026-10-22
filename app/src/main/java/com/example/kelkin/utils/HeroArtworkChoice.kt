package com.example.kelkin.utils

data class HeroArtworkChoice(val movieKey: String, val number: Int = 1,
    val fanartUrl: String? = null, val tmdbPath: String? = null)

internal fun parseHeroArtworkNumber(value: String): Int? {
    val text = value.trim()
    if (text.isEmpty()) return null
    val digits = text.map { Character.digit(it, 10).takeIf { digit -> digit >= 0 } ?: return null }.joinToString("")
    return digits.toIntOrNull()?.takeIf { it > 0 }
}

internal fun <T> numberedHeroImage(images: List<T>, number: Int): T? =
    if (number > 0) images.getOrNull(number - 1) else null

internal fun numberedHeroArtwork(movieKey: String, number: Int, fanart: List<String>, tmdb: List<String>): HeroArtworkChoice? {
    val fanartUrl = numberedHeroImage(fanart, number)
    val tmdbPath = numberedHeroImage(tmdb, number)
    return if (fanartUrl == null && tmdbPath == null) null else HeroArtworkChoice(movieKey, number, fanartUrl, tmdbPath)
}
