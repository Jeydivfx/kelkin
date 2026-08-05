package com.example.kelkin.ViewModels

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kelkin.DataClass.*
import com.example.kelkin.utils.DatabaseHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper(application)
    private val database = FirebaseDatabase.getInstance().reference
    private val tmdbApi = TmdbApiService.create()
    private val prefs = application.getSharedPreferences("kelkin_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    val moviesList = MutableLiveData<List<Movie>>()
    val credentials = MutableLiveData<Credentials>()
    val movieDetailsMap = MutableLiveData<Map<String, TmdbMovieDetails>>()

    val streamHeaders = MutableLiveData<StreamHeaders>()
    private val detailsCache = mutableMapOf<String, TmdbMovieDetails>()
    private val _myList = MutableLiveData<MutableList<Movie>>(mutableListOf())
    val myList: LiveData<MutableList<Movie>> get() = _myList
    val categoriesList = MutableLiveData<List<Category>>()
    private val _continueWatchingList = MutableLiveData<MutableList<Movie>>(mutableListOf())
    val continueWatchingList: LiveData<MutableList<Movie>> get() = _continueWatchingList
    private val _channels = MutableLiveData<List<Channel>>(emptyList())
    val channels: LiveData<List<Channel>> get() = _channels

    val seriesList = MutableLiveData<List<Series>>()
    private val _series = MutableLiveData<List<Series>>(emptyList())
    val series: LiveData<List<Series>> get() = _series
    val seriesCategoriesList = MutableLiveData<List<SeriesCategory>>()


    private val _movies = MutableLiveData<List<Movie>>(emptyList())
    val movies: LiveData<List<Movie>> get() = _movies

    val tvCategoriesList = MutableLiveData<List<TvCategory>>()


    init {
        initRemoteConfig()
        loadFromLocalDatabase()
        loadMyList()
        loadContinueList()
        loadCategories()
        loadTvCategories()
        loadSeriesFromLocalDatabase()
        loadSeriesCategories()

        startSyncMovies()
        startSyncCategories()
        startSyncChannels()
        startSyncTvCategories()
        startSyncSeries()
        startSyncSeriesCategories()
        syncStreamHeaders()
    }

    private fun loadFromLocalDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val mList = mutableListOf<Movie>()

            val cursor = db.query("movies", null, null, null, null, null, null)

            val idCol = cursor.getColumnIndexOrThrow("id")
            val nameCol = cursor.getColumnIndexOrThrow("name_fa")
            val descCol = cursor.getColumnIndexOrThrow("description_fa")
            val categoryCol = cursor.getColumnIndexOrThrow("category")
            val tmdbCol = cursor.getColumnIndexOrThrow("tmdb_id")
            val videoCol = cursor.getColumnIndexOrThrow("videoUrl1")
            val posterCol = cursor.getColumnIndexOrThrow("posterUrl")
            val lastPosCol = cursor.getColumnIndexOrThrow("lastPosition")
            val durationCol = cursor.getColumnIndexOrThrow("totalDuration")

            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val desc = cursor.getString(descCol) ?: ""
                    val cat = cursor.getLong(categoryCol)
                    val tmdbId = cursor.getString(tmdbCol) ?: ""
                    val videoUrl = cursor.getString(videoCol) ?: ""
                    val poster = cursor.getString(posterCol) ?: ""
                    val lastPos = cursor.getLong(lastPosCol)
                    val duration = cursor.getLong(durationCol)

                    val movie = Movie(
                        id = id,
                        name_fa = name,
                        description_fa = desc,
                        category = cat,
                        tmdb_id = tmdbId,
                        videoUrl1 = videoUrl,
                        posterUrl = poster,
                        lastPosition = lastPos,
                        totalDuration = duration
                    )
                    mList.add(movie)
                } catch (e: Exception) {
                    Log.e("KelkinSync", "خطا در پارس فیلم: ${e.message}")
                }
            }
            cursor.close()

            moviesList.postValue(mList)
            _movies.postValue(mList)
        }
    }


    private fun loadSeriesFromLocalDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val sList = mutableListOf<Series>()

            val cursor = db.query("series", null, null, null, null, null, null)

            try {
                val idCol = cursor.getColumnIndexOrThrow("id")
                val nameCol = cursor.getColumnIndexOrThrow("name_fa")
                val descCol = cursor.getColumnIndexOrThrow("description_fa")
                val catCol = cursor.getColumnIndexOrThrow("category")
                val tmdbCol = cursor.getColumnIndexOrThrow("tmdb_id")
                val posterCol = cursor.getColumnIndexOrThrow("posterUrl")
                val seasonsJsonCol = cursor.getColumnIndexOrThrow("seasons_json")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val nameFa = cursor.getString(nameCol) ?: ""
                    val descriptionFa = cursor.getString(descCol) ?: ""
                    val category = cursor.getLong(catCol)
                    val tmdbId = cursor.getString(tmdbCol) ?: ""
                    val posterUrl = cursor.getString(posterCol) ?: ""
                    val seasonsJson = cursor.getString(seasonsJsonCol)


                    val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                    val seasonsMap: Map<String, Map<String, String>> = if (!seasonsJson.isNullOrEmpty()) {
                        try {
                            gson.fromJson(seasonsJson, type) ?: emptyMap()
                        } catch (e: Exception) {
                            Log.e("SeriesDebug", "خطا در پارس جیسون فصل‌ها: ${e.message}")
                            emptyMap()
                        }
                    } else {
                        emptyMap()
                    }

                    val seriesItem = Series(
                        id = id,
                        name_fa = nameFa,
                        description_fa = descriptionFa,
                        category = category,
                        tmdb_id = tmdbId,
                        posterUrl = posterUrl,
                        seasons = seasonsMap
                    )
                    sList.add(seriesItem)
                }
            } catch (e: Exception) {
                Log.e("SeriesDebug", "خطا در خواندن سریال‌ها از لوکال: ${e.message}")
            } finally {
                cursor.close()
            }

            seriesList.postValue(sList)
            _series.postValue(sList)
        }
    }

    fun startSyncMovies() {
        database.child("movies").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("movies", null, null)
                        for (child in snapshot.children) {
                            val values = ContentValues().apply {
                                put("name_fa", child.child("name_fa").value?.toString() ?: "")
                                put("category", child.child("category").value?.toString()?.toLongOrNull() ?: 0L)
                                put("tmdb_id", child.child("tmdb_id").value?.toString() ?: "")
                                put("description_fa", child.child("description_fa").value?.toString() ?: "")
                                put("videoUrl1", child.child("videoUrl1").value?.toString() ?: "")
                            }
                            db.insert("movies", null, values)
                        }
                        db.setTransactionSuccessful()
                    } catch (e: Exception) {
                        Log.e("KelkinSync", "خطا در سینک فیلم‌ها: ${e.message}")
                    } finally {
                        db.endTransaction()
                    }
                    loadFromLocalDatabase()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("KelkinSync", "خطای فایربیس در سینک فیلم‌ها: ${error.message}")
            }
        })
    }

    fun startSyncCategories() {
        database.child("category").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("categories", null, null)

                        for (child in snapshot.children) {
                            val key = child.key ?: ""
                            val id = key.replace("cat_", "").toIntOrNull() ?: 0
                            val name = child.child("name").value?.toString() ?: ""


                            val values = ContentValues().apply {
                                put("id", id)
                                put("name", name)
                            }
                            db.insert("categories", null, values)
                        }
                        db.setTransactionSuccessful()
                    } catch (e: Exception) {
                    } finally {
                        db.endTransaction()
                    }
                    loadCategories()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun startSyncChannels() {
        database.child("channels").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    val list = mutableListOf<Channel>()

                    try {
                        db.beginTransaction()
                        // پاک کردن دیتای قبلی
                        db.delete("channels", null, null)

                        for (child in snapshot.children) {
                            // دریافت مقدار سورس‌ها
                            val sourcesSnapshot = child.child("sources").value
                            val sourceJsonString = if (sourcesSnapshot != null) gson.toJson(sourcesSnapshot) else null

                            val channel = Channel(
                                id = child.child("id").value?.toString()?.toIntOrNull() ?: 0,
                                name_fa = child.child("name_fa").value?.toString() ?: "",
                                category = child.child("category").value?.toString()?.toLongOrNull() ?: 0L,
                                logoUrl = child.child("logoUrl").value?.toString() ?: "",
                                videoUrl = child.child("videoUrl").value?.toString() ?: "",
                                source = sourceJsonString
                            )
                            list.add(channel)

                            // ذخیره در دیتابیس لوکال
                            val values = ContentValues().apply {
                                put("id", channel.id)
                                put("name_fa", channel.name_fa)
                                put("category_id", channel.category)
                                put("logoUrl", channel.logoUrl)
                                put("videoUrl", channel.videoUrl)
                                put("source", channel.source)
                            }

                            val result = db.insert("channels", null, values)
                            if (result == -1L) {
                                Log.e("SourceDebug", "خطا در درج کانال ${channel.id} در دیتابیس")
                            }
                        }
                        db.setTransactionSuccessful()
                      } catch (e: Exception) {
                        Log.e("SourceDebug", "خطای بحرانی در سینک کانال‌ها: ${e.message}")
                    } finally {
                        db.endTransaction()
                    }

                    _channels.postValue(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SourceDebug", "خطای اتصال فایربیس: ${error.message}")
            }
        })
    }


    private fun saveMyList() {
        val json = gson.toJson(_myList.value)
        prefs.edit().putString("saved_my_list", json).apply()
    }

    private fun loadMyList() {
        val json = prefs.getString("saved_my_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Movie>>() {}.type
            _myList.value = gson.fromJson(json, type)
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

    fun isInMyList(tmdbId: String): Boolean = _myList.value?.any { it.tmdb_id == tmdbId } ?: false

    private fun saveContinueList() {
        val json = gson.toJson(_continueWatchingList.value)
        prefs.edit().putString("saved_continue_list", json).apply()
    }

    fun loadContinueList() {
        val json = prefs.getString("saved_continue_list", null)
        val type = object : TypeToken<MutableList<Movie>>() {}.type
        val list: MutableList<Movie> = if (json != null) {
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }
        _continueWatchingList.postValue(list)
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

    fun fetchTmdbDetails(tmdbId: String) {
        if (detailsCache.containsKey(tmdbId)) return

        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null)
            val apiKey = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()

            if (apiKey.isEmpty()) {
                Log.e("KelkinSync", "خطا: API Key در دیتابیس پیدا نشد!")
                return@launch
            }

            try {
                val details = tmdbApi.getMovieDetails(tmdbId, apiKey)
                detailsCache[tmdbId] = details
                movieDetailsMap.postValue(detailsCache)
            } catch (e: Exception) {
                Log.e("KelkinSync", "خطای TMDB: ${e.message}")
            }
        }
    }

    fun fetchChannels() {
        database.child("channels").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Channel>()

                for (child in snapshot.children) {
                    val sourcesSnapshot = child.child("sources").value
                    val sourceJsonString = if (sourcesSnapshot != null) gson.toJson(sourcesSnapshot) else null

                    val channel = Channel(
                        id = child.child("id").value?.toString()?.toIntOrNull() ?: 0,
                        name_fa = child.child("name_fa").value?.toString() ?: "",
                        category = child.child("category").value?.toString()?.toLongOrNull() ?: 0L,
                        logoUrl = child.child("logoUrl").value?.toString() ?: "",
                        videoUrl = child.child("videoUrl").value?.toString() ?: "",
                        source = sourceJsonString
                    )
                    list.add(channel)
                }

                _channels.postValue(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun getMovieFullDetails(tmdbId: String, onResult: (TmdbMovieDetails?, MovieCreditsResponse?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null)
            val apiKey = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()

            if (apiKey.isEmpty()) return@launch

            try {
                val details = tmdbApi.getMovieDetails(tmdbId, apiKey)
                val credits = tmdbApi.getMovieCredits(tmdbId, apiKey)
                onResult(details, credits)
            } catch (e: Exception) {
                Log.e("KelkinSync", "خطا: ${e.message}")
                onResult(null, null)
            }
        }
    }


    fun syncStreamHeaders() {
        database.child("app_config").child("stream_headers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // خواندن دستی فیلدها برای اطمینان از صحت داده‌ها
                val ua = snapshot.child("user_agent").value?.toString() ?: ""
                val ref = snapshot.child("referrer").value?.toString() ?: ""
                val ori = snapshot.child("origin").value?.toString() ?: ""

                streamHeaders.postValue(StreamHeaders(ua, ref, ori))
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun isInitialDataLoaded(): Boolean {
        return !moviesList.value.isNullOrEmpty() && detailsCache.size >= 5
    }

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<Category>()
            list.add(Category(0, "همه ژانرها"))

            val cursor = db.query("categories", null, null, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    list.add(Category(id, name))
                } while (cursor.moveToNext())
            }
            cursor.close()
            categoriesList.postValue(list)
        }
    }

    fun loadTvCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<TvCategory>()
            list.add(TvCategory(0, "همه شبکه‌ها"))
            val cursor = db.query("tv_categories", null, null, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    list.add(TvCategory(id, name))
                } while (cursor.moveToNext())
            }
            cursor.close()

            tvCategoriesList.postValue(list)
        }
    }

    fun startSyncTvCategories() {
        database.child("tv_categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    db.beginTransaction()
                    try {
                        db.delete("tv_categories", null, null)
                        for (child in snapshot.children) {
                            val key = child.key ?: ""
                            val id = key.replace("cat_", "").toIntOrNull() ?: 0
                            val name = child.child("name").value?.toString() ?: "نامشخص"

                            val values = ContentValues().apply {
                                put("id", id)
                                put("name", name)
                            }
                            db.insert("tv_categories", null, values)
                        }
                        db.setTransactionSuccessful()
                        android.util.Log.d("TVDebug", "دیتا با موفقیت در دیتابیس ذخیره شد.")
                    } catch (e: Exception) {
                        android.util.Log.e("TVDebug", "خطا در ذخیره دیتابیس: ${e.message}")
                    } finally {
                        db.endTransaction()
                    }
                    loadTvCategories()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("TVDebug", "خطای اتصال فایربیس: ${error.message}")
            }
        })
    }


    fun startSyncSeries() {
        database.child("series").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("series", null, null)

                        for (child in snapshot.children) {
                            // استخراج دقیق نود seasons به صورت یک Map کامل از فایربیس
                            val seasonsSnapshot = child.child("seasons").value
                            val seasonsJson = if (seasonsSnapshot != null) gson.toJson(seasonsSnapshot) else "{}"

                            val values = ContentValues().apply {
                                put("id", child.child("id").value?.toString()?.toLongOrNull() ?: 0L)
                                put("name_fa", child.child("name_fa").value?.toString() ?: "")
                                put("category", child.child("category").value?.toString()?.toIntOrNull() ?: 0)
                                put("tmdb_id", child.child("tmdb_id").value?.toString() ?: "")
                                put("description_fa", child.child("description_fa").value?.toString() ?: "")
                                put("seasons_json", seasonsJson) // ذخیره ساختار فصل‌ها و اپیزودها به صورت JSON
                            }
                            db.insert("series", null, values)
                        }
                        db.setTransactionSuccessful()
                    } catch (e: Exception) {
                        Log.e("SeriesSync", "خطا در سینک سریال‌ها: ${e.message}")
                    } finally {
                        db.endTransaction()
                    }
                    loadSeriesFromLocalDatabase()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SeriesSync", "خطای فایربیس در سینک سریال‌ها: ${error.message}")
            }
        })
    }

    fun loadSeriesCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<SeriesCategory>()
            list.add(SeriesCategory(0, "همه دسته‌ها"))

            val cursor = db.query("series_categories", null, null, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""
                    list.add(SeriesCategory(id, name))
                } while (cursor.moveToNext())
            }
            cursor.close()
            seriesCategoriesList.postValue(list)
        }
    }

    fun startSyncSeriesCategories() {
        database.child("series_categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("series_categories", null, null)

                        for (child in snapshot.children) {
                            val key = child.key ?: ""
                            val id = key.replace("cat_", "").toIntOrNull() ?: child.child("id").value?.toString()?.toIntOrNull() ?: 0
                            val name = child.child("name").value?.toString() ?: ""

                            val values = ContentValues().apply {
                                put("id", id)
                                put("name", name)
                            }
                            db.insert("series_categories", null, values)
                        }
                        db.setTransactionSuccessful()
                    } catch (e: Exception) {
                        Log.e("KelkinSync", "خطا در سینک دسته‌بندی سریال‌ها: ${e.message}")
                    } finally {
                        db.endTransaction()
                    }
                    loadSeriesCategories()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("KelkinSync", "خطای فایربیس در سینک دسته‌بندی سریال‌ها: ${error.message}")
            }
        })
    }


    fun fetchSeriesTmdbDetails(tmdbId: String) {
        if (tmdbId.isEmpty() || detailsCache.containsKey(tmdbId)) return

        viewModelScope.launch(Dispatchers.IO) {
            val tvDetails = fetchTvDetailsForPoster(tmdbId)
            if (tvDetails != null) {
                val mappedDetails = TmdbMovieDetails(
                    id = tvDetails.id,
                    title = tvDetails.name,
                    overview = tvDetails.overview,
                    release_date = tvDetails.first_air_date ?: "",
                    vote_average = tvDetails.vote_average,
                    poster_path = tvDetails.poster_path,
                    original_title = tvDetails.original_name,
                    backdrop_path = tvDetails.backdrop_path,
                    genres = tvDetails.genres,
                    release_dates = null
                )
                detailsCache[tmdbId] = mappedDetails
                movieDetailsMap.postValue(detailsCache)
            }
        }
    }



    fun initRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
    }

    suspend fun fetchMovieDetailsForPoster(tmdbId: String): TmdbMovieDetails {
        val apiKey = try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null)
            val key = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()
            key
        } catch (e: Exception) { "" }

        return tmdbApi.getMovieDetails(tmdbId, apiKey)
    }


    suspend fun fetchTvDetailsForPoster(tvId: String): TmdbTvDetails? {
        val apiKey = try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null)
            val key = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()
            key
        } catch (e: Exception) { "" }

        if (apiKey.isEmpty()) return null

        return try {
            tmdbApi.getTvDetails(tvId, apiKey)
        } catch (e: Exception) {
            Log.e("SeriesDebug", "خطا در دریافت اطلاعات TV از TMDB: ${e.message}")
            null
        }
    }


}