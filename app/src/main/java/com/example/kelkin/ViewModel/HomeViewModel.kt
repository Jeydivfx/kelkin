package com.example.kelkin.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.utils.CatalogRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatalogRepository.get(application)
    init { repository.acquire() }
    val moviesList = repository.moviesList
    val movieDetailsMap = repository.movieDetailsMap
    val streamHeaders = repository.streamHeaders
    val heroMovieKey = repository.heroMovieKey
    val myList = repository.myList
    val categoriesList = repository.categoriesList
    val continueWatchingList = repository.continueWatchingList
    val channels = repository.channels
    val seriesList = repository.seriesList
    val seriesCategoriesList = repository.seriesCategoriesList
    val languagesList = repository.languagesList
    val qualitiesList = repository.qualitiesList
    val tvCategoriesList = repository.tvCategoriesList
    val syncError = repository.syncError
    val catalogLoading = repository.catalogLoading

    fun refresh() = repository.refresh()
    fun loadCategories() = repository.loadCategories()
    fun loadSeriesCategories() = repository.loadSeriesCategories()
    fun loadContinueList() = repository.loadContinueList()
    fun addToMyList(movie: Movie) = repository.addToMyList(movie)
    fun removeFromMyList(movie: Movie) = repository.removeFromMyList(movie)
    fun isInMyList(movie: Movie) = repository.isInMyList(movie)
    fun updateContinueWatching(movie: Movie, position: Long, duration: Long) = repository.updateContinueWatching(movie, position, duration)
    fun fetchTmdbDetails(id: String) = repository.fetchTmdbDetails(id)
    fun fetchSeriesTmdbDetails(id: String) = repository.fetchSeriesTmdbDetails(id)
    suspend fun fetchMovieDetailsForPoster(id: String) = repository.fetchMovieDetailsForPoster(id)
    suspend fun fetchTvDetailsForPoster(id: String) = repository.fetchTvDetailsForPoster(id)
    suspend fun getMovieFullDetails(id: String) = repository.getMovieFullDetails(id)
    override fun onCleared() { repository.release(); super.onCleared() }
}
