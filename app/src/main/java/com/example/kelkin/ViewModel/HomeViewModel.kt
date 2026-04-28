package com.example.kelkin.ViewModels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.AndroidViewModel // تغییر از ViewModel به AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kelkin.DataClass.Channel
import com.example.kelkin.DataClass.Credentials
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.Radio
import com.example.kelkin.DataClass.TmdbApiService
import com.example.kelkin.DataClass.TmdbMovieDetails
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // این ایمپورت برای TypeToken ضروری است
import kotlinx.coroutines.launch


class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FirebaseDatabase.getInstance().reference
    private val tmdbApi = TmdbApiService.create()
    private val prefs = application.getSharedPreferences("kelkin_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    val moviesList = MutableLiveData<List<Movie>>()
    val credentials = MutableLiveData<Credentials>()
    val movieDetailsMap = MutableLiveData<Map<String, TmdbMovieDetails>>()
    private val detailsCache = mutableMapOf<String, TmdbMovieDetails>()
    private val _myList = MutableLiveData<MutableList<Movie>>(mutableListOf())
    val myList: LiveData<MutableList<Movie>> get() = _myList
    private val _continueWatchingList = MutableLiveData<MutableList<Movie>>(mutableListOf())
    val continueWatchingList: LiveData<MutableList<Movie>> get() = _continueWatchingList
    private val _channels = MutableLiveData<List<Channel>>(emptyList())
    val channels: LiveData<List<Channel>> get() = _channels
    private val _movies = MutableLiveData<List<Movie>>(emptyList())
    val movies: LiveData<List<Movie>> get() = _movies

    private val _radioStations = MutableLiveData<List<Radio>>(emptyList())
    val radioStations: LiveData<List<Radio>> get() = _radioStations

    init {
        loadMyList()
        loadContinueList()
    }


    private fun saveMyList() {
        val json = gson.toJson(_myList.value)
        prefs.edit().putString("saved_my_list", json).apply()
    }

    private fun loadMyList() {
        val json = prefs.getString("saved_my_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<MutableList<Movie>>() {}.type
                val savedList: MutableList<Movie> = gson.fromJson(json, type)
                _myList.value = savedList

                val apiKey = credentials.value?.apiKey ?: ""


                savedList.forEach { movie ->
                    if (movie.tmdb_id.isNotEmpty() && apiKey.isNotEmpty()) {
                        fetchTmdbDetails(movie.tmdb_id, apiKey)
                    }
                }
            } catch (e: Exception) {
                Log.e("kelkinDebug", "Error loading list: ${e.message}")
                _myList.value = mutableListOf()
            }
        }
    }

    fun addToMyList(movie: Movie) {
        val currentList = _myList.value ?: mutableListOf()
        if (!currentList.any { it.tmdb_id == movie.tmdb_id }) {
            currentList.add(movie)
            _myList.value = currentList
            saveMyList()
        }
    }

    fun removeFromMyList(movie: Movie) {
        val currentList = _myList.value ?: mutableListOf()
        currentList.removeAll { it.tmdb_id == movie.tmdb_id }
        _myList.value = currentList
        saveMyList()
    }

    fun isInMyList(tmdbId: String): Boolean {
        return _myList.value?.any { it.tmdb_id == tmdbId } ?: false
    }

    private fun saveContinueList() {
        val json = gson.toJson(_continueWatchingList.value)
        prefs.edit().putString("saved_continue_list", json).apply()
    }


    fun loadContinueList() {
        val json = prefs.getString("saved_continue_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<MutableList<Movie>>() {}.type
                val savedList: MutableList<Movie> = gson.fromJson(json, type)
                _continueWatchingList.value = savedList

                val apiKey = credentials.value?.apiKey ?: ""
                savedList.forEach { movie ->
                    if (movie.tmdb_id.isNotEmpty() && apiKey.isNotEmpty()) {
                        fetchTmdbDetails(movie.tmdb_id, apiKey)
                    }
                }
            } catch (e: Exception) {
                _continueWatchingList.value = mutableListOf()
            }
        }
    }


    fun updateContinueWatching(movie: Movie, position: Long, duration: Long) {
        val currentList = _continueWatchingList.value ?: mutableListOf()

        currentList.removeAll { it.tmdb_id == movie.tmdb_id }

        movie.lastPosition = position
        movie.totalDuration = duration


        currentList.add(0, movie)
        _continueWatchingList.value = currentList

        saveContinueList()
    }



    fun fetchData() {
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) loadCredentials()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadCredentials() {
        database.child("credentials").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val creds = snapshot.getValue(Credentials::class.java)
                creds?.let {
                    credentials.postValue(it)

                    loadMoviesFromFirebase(it.apiKey)

                    _myList.value?.forEach { movie ->
                        if (movie.tmdb_id.isNotEmpty()) fetchTmdbDetails(movie.tmdb_id, it.apiKey)
                    }

                    _continueWatchingList.value?.forEach { movie ->
                        if (movie.tmdb_id.isNotEmpty()) {
                            fetchTmdbDetails(movie.tmdb_id, it.apiKey)
                        }
                    }

                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadMoviesFromFirebase(apiKey: String) {
        database.child("movies").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movies = mutableListOf<Movie>()
                snapshot.children.forEach { child ->
                    try {
                        val movie = Movie(
                            id = (child.child("id").value as? Long) ?: 0L,
                            name_fa = child.child("name_fa").value?.toString() ?: "",
                            description_fa = child.child("description_fa").value?.toString() ?: "",
                            tmdb_id = child.child("tmdb_id").value?.toString() ?: "",
                            videoUrl1 = child.child("videoUrl1").value?.toString() ?: "",
                            videoUrl2 = child.child("videoUrl2").value?.toString() ?: ""
                        )
                        movies.add(movie)
                        if (movie.tmdb_id.isNotEmpty()) fetchTmdbDetails(movie.tmdb_id, apiKey)
                    } catch (e: Exception) { Log.e("MONITOR", "Error parsing movie") }
                }
                moviesList.postValue(movies)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun fetchTmdbDetails(tmdbId: String, apiKey: String) {
        if (detailsCache.containsKey(tmdbId)) {
            movieDetailsMap.postValue(detailsCache)
            return
        }

        viewModelScope.launch {
            try {
                val details = tmdbApi.getMovieDetails(tmdbId, apiKey)
                detailsCache[tmdbId] = details
                movieDetailsMap.postValue(detailsCache)
            } catch (e: Exception) {
                Log.e("MyList", "Error for $tmdbId: ${e.message}")
            }
        }
    }

    fun fetchChannels() {
        database.child("channels").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val channelList = mutableListOf<Channel>()
                for (childSnapshot in snapshot.children) {
                    val channel = childSnapshot.getValue(Channel::class.java)
                    channel?.let { channelList.add(it) }
                }
                _channels.value = channelList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("kelkinDebug", "Firebase Error: ${error.message}")
            }
        })
    }

    fun loadAllMovies() {
        database.child("movies").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Movie>()
                for (child in snapshot.children) {
                    try {
                        val movie = child.getValue(Movie::class.java)
                        movie?.let { list.add(it) }
                    } catch (e: Exception) {
                        Log.e("MoviesFragment", "خطا در پارس کردن فیلم: ${child.key} -> ${e.message}")
                    }
                }
                _movies.postValue(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun fetchRadios() {
        // اصلاح شد: به جای "radios" از "radio" استفاده کن (مطابق عکس دیتابیس)
        database.child("radio").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Radio>()
                for (child in snapshot.children) {
                    try {
                        val radio = child.getValue(Radio::class.java)
                        radio?.let { list.add(it) }
                    } catch (e: Exception) {
                        Log.e("RadioError", "Error: ${e.message}")
                    }
                }
                _radioStations.postValue(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }




}