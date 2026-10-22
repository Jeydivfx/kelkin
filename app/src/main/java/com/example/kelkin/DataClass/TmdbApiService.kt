package com.example.kelkin.DataClass

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(@Path("movie_id") movieId: String, @Query("api_key") apiKey: String): TvBackdropImages
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "release_dates",
        @Query("language") lang: String = "en-US"
    ): TmdbMovieDetails

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): MovieCreditsResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: String,
        @Query("api_key") apiKey: String,
        @Query("language") lang: String = "en-US"
    ): TmdbTvDetails

    // Deliberately omit language: return Chinese, English and untagged artwork alike.
    @GET("tv/{tv_id}/images")
    suspend fun getTvImages(
        @Path("tv_id") tvId: String,
        @Query("api_key") apiKey: String
    ): TvBackdropImages

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: String,
        @Query("api_key") apiKey: String
    ): MovieCreditsResponse

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"

        fun create(): TmdbApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TmdbApiService::class.java)
        }
    }
}
