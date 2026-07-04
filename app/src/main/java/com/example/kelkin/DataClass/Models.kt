package com.example.kelkin.DataClass

import android.os.Parcelable
import java.io.Serializable


data class Category(
    val id: Int = 0,
    val name: String = ""
) : Serializable

data class TvCategory(
    val id: Int = 0,
    val name: String = ""
) : Serializable

data class Credentials(
    val apiKey: String = "",
    val userAgent: String = ""
)

data class StreamHeaders(
    val user_agent: String = "",
    val referrer: String = "",
    val origin: String = ""
)

data class Channel(
    val id: Int = 0,
    val name_fa: String = "",
    val category: Long = 0,
    val logoUrl: String = "",
    val videoUrl: String = "",
    val source: String? = null
)


data class ChannelSource(
    val name: String,
    val url: String,
    val geography: String
)



data class Movie(
    var id: Long = 0,
    var name_fa: String = "",
    var description_fa: String = "",
    val category: Long = 0,
    var tmdb_id: String = "",
    var videoUrl1: String = "",
    var posterUrl: String = "",
    var lastPosition: Long = 0,
    var totalDuration: Long = 0
) : Serializable{
    constructor() : this(0, "", "", 0, "", "",  "", 0, 0)}


data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val release_date: String,
    val vote_average: Double,
    val poster_path: String?,
    val original_title: String,
    val backdrop_path: String?,
    val genres: List<Genre>,
    val release_dates: ReleaseDatesResults?
)

data class MovieCreditsResponse(
    val id: Int,
    val cast: List<CastMember>,
    val crew: List<CrewMember>
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profile_path: String?
)

data class CrewMember(
    val id: Int,
    val name: String,
    val job: String,
    val profile_path: String?
)



data class Genre(val name: String)

data class ReleaseDatesResults(val results: List<ReleaseDateItem>)
data class ReleaseDateItem(val iso_3166_1: String, val release_dates: List<CertificationItem>)
data class CertificationItem(val certification: String)


