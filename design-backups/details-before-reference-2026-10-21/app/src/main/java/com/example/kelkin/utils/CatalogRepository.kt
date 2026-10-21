package com.example.kelkin.utils

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.google.firebase.database.DatabaseReference
import com.google.firebase.auth.FirebaseAuth

class CatalogRepository private constructor(private val application: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listeners = linkedMapOf<DatabaseReference, ValueEventListener>()
    private var owners = 0
    private val permits = Semaphore(4)
    private val inFlight = mutableSetOf<String>()
    private val retryAfter = mutableMapOf<String, Long>()
    val syncError = MutableLiveData<String?>(null)
    val catalogLoading = MutableLiveData(true)
    private val history = WatchHistory(application)
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        listeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        listeners.clear()
        if (auth.currentUser != null) refresh()
    }
    companion object {
        @Volatile private var instance: CatalogRepository? = null
        fun get(application: Application): CatalogRepository = instance ?: synchronized(this) {
            instance ?: CatalogRepository(application).also { instance = it }
        }
    }
    fun acquire() {
        if (owners++ == 0) FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }
    fun release() {
        if (--owners == 0) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
            listeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
            listeners.clear()
        }
    }
    private fun listen(ref: DatabaseReference, listener: ValueEventListener) {
        if (ref in listeners) return
        listeners[ref] = listener
        ref.addValueEventListener(listener)
    }
    fun refresh() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        listeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        listeners.clear()
        syncError.value = null
        startSyncLanguages(); startSyncQualities(); startSyncMovies(); startSyncCategories()
        startSyncChannels(); startSyncTvCategories(); startSyncSeries(); startSyncSeriesCategories()
        syncStreamHeaders()
        syncHeroSelection()
    }
    private val dbHelper = DatabaseHelper(application)
    private val database = FirebaseDatabase.getInstance().reference
    private val tmdbApi = TmdbApiService.create()
    private val prefs = application.getSharedPreferences("kelkin_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    val moviesList = MutableLiveData<List<Movie>>()
    val credentials = MutableLiveData<Credentials>()
    val movieDetailsMap = MutableLiveData<Map<String, TmdbMovieDetails>>()
    val streamHeaders = MutableLiveData<StreamHeaders>()
    val heroMovieKey = MutableLiveData<String?>(prefs.getString("hero_movie_key", null))
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
    val languagesList = MutableLiveData<List<Language>>()
    val qualitiesList = MutableLiveData<List<Quality>>()
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
        loadLanguages()
        loadQualities()

        loadChannelsFromLocalDatabase()
        loadLocalHeaders()
        loadMetadata()
    }

    private fun syncHeroSelection() {
        listen(database.child("app_config/home/hero_movie_key"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val key = snapshot.value?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                prefs.edit().apply {
                    if (key == null) remove("hero_movie_key") else putString("hero_movie_key", key)
                }.apply()
                heroMovieKey.value = key
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("CatalogRepository", "Hero selection unavailable; retaining cached selection", error.toException())
            }
        })
    }

    private fun loadFromLocalDatabase() {
        scope.launch(Dispatchers.IO) {
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
            val langCol = cursor.getColumnIndexOrThrow("language")
            val qualCol = cursor.getColumnIndexOrThrow("quality")

            while (cursor.moveToNext()) {
                try {
                    val movie = Movie(
                        id = cursor.getLong(idCol),
                        name_fa = cursor.getString(nameCol) ?: "",
                        description_fa = cursor.getString(descCol) ?: "",
                        category = cursor.getLong(categoryCol),
                        tmdb_id = cursor.getString(tmdbCol) ?: "",
                        videoUrl1 = cursor.getString(videoCol) ?: "",
                        posterUrl = cursor.getString(posterCol) ?: "",
                        lastPosition = cursor.getLong(lastPosCol),
                        totalDuration = cursor.getLong(durationCol),
                        language = cursor.getString(langCol) ?: "",
                        quality = cursor.getString(qualCol) ?: "",
                        remoteKey = cursor.getString(cursor.getColumnIndexOrThrow("remoteKey")) ?: ""
                    )
                    mList.add(movie)
                } catch (e: Exception) {
                    Log.e("KelkinSync", "خطا در پارس فیلم: ${e.message}")
                }
            }
            cursor.close()

            catalogLoading.postValue(false)
            moviesList.postValue(mList)
            _movies.postValue(mList)
        }
    }


    private fun loadSeriesFromLocalDatabase() {
        scope.launch(Dispatchers.IO) {
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
                val langCol = cursor.getColumnIndexOrThrow("language")
                val qualCol = cursor.getColumnIndexOrThrow("quality")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val nameFa = cursor.getString(nameCol) ?: ""
                    val descriptionFa = cursor.getString(descCol) ?: ""
                    val category = cursor.getLong(catCol)
                    val tmdbId = cursor.getString(tmdbCol) ?: ""
                    val posterUrl = cursor.getString(posterCol) ?: ""
                    val seasonsJson = cursor.getString(seasonsJsonCol)
                    val language = cursor.getString(langCol) ?: ""
                    val quality = cursor.getString(qualCol) ?: ""


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
                        seasons = seasonsMap,
                        language = language,
                        quality = quality,
                        remoteKey = cursor.getString(cursor.getColumnIndexOrThrow("remoteKey")) ?: ""
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
        listen(database.child("movies"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("movies", null, null)
                        for (child in snapshot.children) {
                            val values = ContentValues().apply {
                                put("id", child.child("id").value?.toString()?.toLongOrNull() ?: stableContentId(child.key.orEmpty()))
                                put("remoteKey", child.key.orEmpty())
                                put("posterUrl", child.child("posterUrl").value?.toString().orEmpty())
                                put("voteAverage", child.child("voteAverage").value?.toString()?.toDoubleOrNull() ?: 0.0)
                                put("name_fa", child.child("name_fa").value?.toString() ?: "")
                                put("category", child.child("category").value?.toString()?.toLongOrNull() ?: 0L)
                                put("tmdb_id", child.child("tmdb_id").value?.toString() ?: "")
                                put("description_fa", child.child("description_fa").value?.toString() ?: "")
                                put("videoUrl1", child.child("videoUrl1").value?.toString() ?: "")
                                put("language", child.child("language").value?.toString() ?: "")
                                put("quality", child.child("quality").value?.toString() ?: "")
                            }
                            db.insertOrThrow("movies", null, values)
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
                syncError.postValue("اتصال به سرور برقرار نشد؛ دوباره تلاش کنید.")
                Log.e("KelkinSync", "خطای فایربیس در سینک فیلم‌ها: ${error.message}")
            }
        })
    }

    fun startSyncCategories() {
        listen(database.child("category"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                scope.launch(Dispatchers.IO) {
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
            override fun onCancelled(error: DatabaseError) { syncError.postValue("اتصال به سرور برقرار نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود.") }
        })
    }


    fun startSyncChannels() {
        listen(database.child("channels"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    val list = mutableListOf<Channel>()

                    try {
                        db.beginTransaction()
                        // پاک کردن دیتای قبلی
                        db.delete("channels", null, null)

                        for (child in snapshot.children) {
                            // دریافت مقدار سورس‌ها
                            val sourceJsonString = sourceJson(child)

                            val channel = Channel(
                                id = child.child("id").value?.toString()?.toLongOrNull() ?: 0L,
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
                syncError.postValue("اتصال به سرور برقرار نشد؛ دوباره تلاش کنید.")
                Log.e("SourceDebug", "خطای اتصال فایربیس: ${error.message}")
            }
        })
    }


    private fun saveMyList() {
        val json = gson.toJson(_myList.value)
        prefs.edit().putString("saved_my_list", json).apply()
    }

    private fun loadMyList() {
        val type = object : TypeToken<MutableList<Movie>>() {}.type
        _myList.value = runCatching { gson.fromJson<MutableList<Movie>>(prefs.getString("saved_my_list", "[]"), type) }.getOrNull() ?: mutableListOf()
    }

    fun addToMyList(movie: Movie) {
        val currentList = _myList.value.orEmpty().toMutableList()
        if (currentList.none { it.identity() == movie.identity() }) {
            currentList.add(0, movie.copy())
            _myList.value = currentList
            saveMyList()
        }
    }

    fun removeFromMyList(movie: Movie) {
        _myList.value = _myList.value.orEmpty().filterNot { it.identity() == movie.identity() }.toMutableList()
        saveMyList()
    }

    fun isInMyList(movie: Movie): Boolean = _myList.value.orEmpty().any { it.identity() == movie.identity() }

    private fun saveContinueList() {
        val json = gson.toJson(_continueWatchingList.value)
        prefs.edit().putString("saved_continue_list", json).apply()
    }

    fun loadContinueList() { _continueWatchingList.value = history.load() }

    fun updateContinueWatching(movie: Movie, position: Long, duration: Long) {
        _continueWatchingList.value = history.save(movie, position, duration)
    }

    fun fetchTmdbDetails(tmdbId: String) = fetchMetadata("movie", tmdbId)

    private fun fetchMetadata(type: String, id: String) {
        val key = metadataKey(type, id)
        if (id.isBlank() || detailsCache.containsKey(key) || !inFlight.add(key)) return
        if ((retryAfter[key] ?: 0L) > System.currentTimeMillis()) { inFlight.remove(key); return }
        scope.launch {
            try {
                val details = permits.withPermit {
                    if (type == "series") {
                        val tv = fetchTvDetailsForPoster(id) ?: error("No TV metadata")
                        TmdbMovieDetails(tv.id, tv.name, tv.overview, tv.first_air_date.orEmpty(), tv.vote_average, tv.poster_path, tv.original_name, tv.backdrop_path, tv.genres, null)
                    } else fetchMovieDetailsForPoster(id)
                }
                detailsCache[key] = details
                movieDetailsMap.value = detailsCache.toMap()
                prefs.edit().putString("metadata_cache_v2", gson.toJson(detailsCache)).apply()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { retryAfter[key] = System.currentTimeMillis() + 60_000L }
            finally { inFlight.remove(key) }
        }
    }

    private fun loadMetadata() {
        val type = object : TypeToken<Map<String, TmdbMovieDetails>>() {}.type
        runCatching { gson.fromJson<Map<String, TmdbMovieDetails>>(prefs.getString("metadata_cache_v2", "{}"), type) }
            .getOrNull()?.let { detailsCache.putAll(it); movieDetailsMap.value = detailsCache.toMap() }
    }

    fun fetchChannels() {
        database.child("channels").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Channel>()

                for (child in snapshot.children) {
                    val sourceJsonString = sourceJson(child)

                    val channel = Channel(
                        id = child.child("id").value?.toString()?.toLongOrNull() ?: 0L,
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

            override fun onCancelled(error: DatabaseError) { syncError.postValue("اتصال به سرور برقرار نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود.") }
        })
    }


    suspend fun getMovieFullDetails(tmdbId: String): Pair<TmdbMovieDetails?, MovieCreditsResponse?> {
        return try {
            val details = fetchMovieDetailsForPoster(tmdbId)
            val credits = runCatching { tmdbApi.getMovieCredits(tmdbId, apiKey()) }.getOrNull()
            details to credits
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { null to null }
    }


    fun syncStreamHeaders() {
        listen(database.child("app_config").child("stream_headers"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // خواندن دستی فیلدها برای اطمینان از صحت داده‌ها
                val ua = snapshot.child("user_agent").value?.toString() ?: ""
                val ref = snapshot.child("referrer").value?.toString() ?: ""
                val ori = snapshot.child("origin").value?.toString() ?: ""

                if (snapshot.exists()) {
                    val headers = StreamHeaders(ua, ref, ori)
                    scope.launch(Dispatchers.IO) {
                        val values = ContentValues().apply {
                            put("id", 1); put("userAgent", ua); put("referrer", ref); put("origin", ori)
                        }
                        dbHelper.writableDatabase.insertWithOnConflict("stream_config", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                        streamHeaders.postValue(headers)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { syncError.postValue("اتصال به سرور برقرار نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود.") }
        })
    }

    fun isInitialDataLoaded(): Boolean {
        return !moviesList.value.isNullOrEmpty() && detailsCache.size >= 5
    }

    fun loadCategories() {
        scope.launch(Dispatchers.IO) {
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
        scope.launch(Dispatchers.IO) {
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
        listen(database.child("tv_categories"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
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
                syncError.postValue("اتصال به سرور برقرار نشد؛ دوباره تلاش کنید.")
                android.util.Log.e("TVDebug", "خطای اتصال فایربیس: ${error.message}")
            }
        })
    }


    fun startSyncSeries() {
        listen(database.child("series"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("series", null, null)

                        for (child in snapshot.children) {
                            val seasonsSnapshot = child.child("seasons").value
                            val seasonsJson = if (seasonsSnapshot != null) gson.toJson(seasonsSnapshot) else "{}"

                            val values = ContentValues().apply {
                                put("id", child.child("id").value?.toString()?.toLongOrNull() ?: stableContentId(child.key.orEmpty()))
                                put("remoteKey", child.key.orEmpty())
                                put("posterUrl", child.child("posterUrl").value?.toString().orEmpty())
                                put("voteAverage", child.child("voteAverage").value?.toString()?.toDoubleOrNull() ?: 0.0)
                                put("name_fa", child.child("name_fa").value?.toString() ?: "")
                                put("category", child.child("category").value?.toString()?.toIntOrNull() ?: 0)
                                put("tmdb_id", child.child("tmdb_id").value?.toString() ?: "")
                                put("description_fa", child.child("description_fa").value?.toString() ?: "")
                                put("seasons_json", seasonsJson)
                                put("language", child.child("language").value?.toString() ?: "")
                                put("quality", child.child("quality").value?.toString() ?: "")
                            }
                            db.insertOrThrow("series", null, values)
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
                syncError.postValue("اتصال به سرور برقرار نشد؛ دوباره تلاش کنید.")
                Log.e("SeriesSync", "خطای فایربیس در سینک سریال‌ها: ${error.message}")
            }
        })
    }

    fun loadSeriesCategories() {
        scope.launch(Dispatchers.IO) {
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
        listen(database.child("series_categories"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
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
                syncError.postValue("اتصال به سرور برقرار نشد؛ دوباره تلاش کنید.")
                Log.e("KelkinSync", "خطای فایربیس در سینک دسته‌بندی سریال‌ها: ${error.message}")
            }
        })
    }


    fun fetchSeriesTmdbDetails(tmdbId: String) = fetchMetadata("series", tmdbId)



    fun initRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
    }

    suspend fun fetchMovieDetailsForPoster(tmdbId: String): TmdbMovieDetails {
        return detailsCache[metadataKey("movie", tmdbId)] ?: tmdbApi.getMovieDetails(tmdbId, apiKey())
    }

    private suspend fun apiKey(): String = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null).use {
            if (it.moveToFirst()) it.getString(0).orEmpty() else ""
        }
    }


    suspend fun fetchTvDetailsForPoster(tvId: String): TmdbTvDetails? {
        return try { tmdbApi.getTvDetails(tvId, apiKey()) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { null }
    }


    fun loadLanguages() {
        scope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<Language>()
            list.add(Language(0, "همه زبان‌ها"))

            val cursor = db.query("languages", null, null, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""
                    list.add(Language(id.toLong(), name))
                } while (cursor.moveToNext())
            }
            cursor.close()
            languagesList.postValue(list)
        }
    }

    fun startSyncLanguages() {
        listen(database.child("language"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("languages", null, null)

                        for (child in snapshot.children) {
                            val key = child.key ?: ""
                            val id = key.replace("lan_", "").toIntOrNull() ?: child.child("id").value?.toString()?.toIntOrNull() ?: 0
                            val name = child.child("name").value?.toString() ?: ""

                            val values = ContentValues().apply {
                                put("id", id)
                                put("name", name)
                            }
                            db.insert("languages", null, values)
                        }
                        db.setTransactionSuccessful()
                    } catch (e: Exception) {
                    } finally {
                        db.endTransaction()
                    }
                    loadLanguages()
                }
            }

            override fun onCancelled(error: DatabaseError) { syncError.postValue("اتصال به سرور برقرار نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود.") }
        })
    }

    fun loadQualities() {
        scope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<Quality>()
            list.add(Quality(0, "همه کیفیت‌ها"))

            val cursor = db.query("qualities", null, null, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: ""
                    list.add(Quality(id.toLong(), name))
                } while (cursor.moveToNext())
            }
            cursor.close()
            qualitiesList.postValue(list)
        }
    }

    fun startSyncQualities() {
        listen(database.child("quality"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("qualities", null, null)

                        for (child in snapshot.children) {
                            val key = child.key ?: ""
                            val id = key.replace("qu_", "").toIntOrNull() ?: child.child("id").value?.toString()?.toIntOrNull() ?: 0
                            val name = child.child("name").value?.toString() ?: ""

                            val values = ContentValues().apply {
                                put("id", id)
                                put("name", name)
                            }
                            db.insert("qualities", null, values)
                        }
                        db.setTransactionSuccessful()
                    } catch (e: Exception) {
                    } finally {
                        db.endTransaction()
                    }
                    loadQualities()
                }
            }

            override fun onCancelled(error: DatabaseError) { syncError.postValue("اتصال به سرور برقرار نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود.") }
        })
    }



    private fun sourceJson(child: DataSnapshot): String? {
        val raw = child.child("sources").value ?: child.child("source").value ?: return null
        return if (raw is String) raw else gson.toJson(raw)
    }
    private fun loadLocalHeaders() {
        scope.launch(Dispatchers.IO) {
            dbHelper.readableDatabase.rawQuery("SELECT userAgent, referrer, origin FROM stream_config WHERE id=1", null).use {
                val headers = if (it.moveToFirst()) StreamHeaders(it.getString(0).orEmpty(), it.getString(1).orEmpty(), it.getString(2).orEmpty()) else StreamHeaders()
                streamHeaders.postValue(headers)
            }
        }
    }
    private fun loadChannelsFromLocalDatabase() {
        scope.launch(Dispatchers.IO) {
            val list = mutableListOf<Channel>()
            dbHelper.readableDatabase.query("channels", null, null, null, null, null, null).use { c ->
                while (c.moveToNext()) list += Channel(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    name_fa = c.getString(c.getColumnIndexOrThrow("name_fa")).orEmpty(),
                    category = c.getLong(c.getColumnIndexOrThrow("category_id")),
                    logoUrl = c.getString(c.getColumnIndexOrThrow("logoUrl")).orEmpty(),
                    videoUrl = c.getString(c.getColumnIndexOrThrow("videoUrl")).orEmpty(),
                    source = c.getString(c.getColumnIndexOrThrow("source"))
                )
            }
            // Do not let an older cache read replace a fresh remote result.
            withContext(Dispatchers.Main) { if (_channels.value.isNullOrEmpty()) _channels.value = list }
        }
    }

}
