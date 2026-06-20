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
    private val detailsCache = mutableMapOf<String, TmdbMovieDetails>()

    private val _myList = MutableLiveData<MutableList<Movie>>(mutableListOf())
    val myList: LiveData<MutableList<Movie>> get() = _myList

    val categoriesList = MutableLiveData<List<Category>>()

    private val _continueWatchingList = MutableLiveData<MutableList<Movie>>(mutableListOf())
    val continueWatchingList: LiveData<MutableList<Movie>> get() = _continueWatchingList

    private val _channels = MutableLiveData<List<Channel>>(emptyList())
    val channels: LiveData<List<Channel>> get() = _channels


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

        startSyncMovies()
        startSyncCategories()
        startSyncChannels()
        startSyncTvCategories()
    }

    private fun loadFromLocalDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val mList = mutableListOf<Movie>()

            val cursor = db.query("movies", null, null, null, null, null, null)

            val idCol = cursor.getColumnIndexOrThrow("id")
            val nameCol = cursor.getColumnIndexOrThrow("name_fa")
            val descCol = cursor.getColumnIndexOrThrow("description_fa")
            val categoryCol = cursor.getColumnIndexOrThrow("category") // ۱. اضافه کردن این خط
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
                    val cat = cursor.getLong(categoryCol) // ۲. خواندن مقدار کتگوری
                    val tmdbId = cursor.getString(tmdbCol) ?: ""
                    val videoUrl = cursor.getString(videoCol) ?: ""
                    val poster = cursor.getString(posterCol) ?: ""
                    val lastPos = cursor.getLong(lastPosCol)
                    val duration = cursor.getLong(durationCol)

                    val movie = Movie(
                        id = id,
                        name_fa = name,
                        description_fa = desc,
                        category = cat, // ۳. مقداردهی به مدل
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
    // ۱. سینک ریل‌تایم فیلم‌ها
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
                        Log.d("KelkinSync", "فیلم‌ها ریل‌تایم سینک شدند.")
                    } catch (e: Exception) {
                        Log.e("KelkinSync", "خطا در سینک فیلم‌ها: ${e.message}")
                    } finally {
                        db.endTransaction()
                    }
                    loadFromLocalDatabase() // آپدیت لیست فیلم‌ها در UI
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("KelkinSync", "خطای فایربیس در سینک فیلم‌ها: ${error.message}")
            }
        })
    }

    // ۲. سینک ریل‌تایم کتگوری‌ها
    fun startSyncCategories() {
        // اصلاح مسیر به "category" (مطابق تصویر)
        database.child("category").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    try {
                        db.beginTransaction()
                        db.delete("categories", null, null)

                        for (child in snapshot.children) {
                            // کلید در اینجا همان "cat_01" است
                            val key = child.key ?: ""
                            // استخراج عدد از کلید (مثلاً cat_01 -> 1)
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
                    val list = mutableListOf<Channel>()

                    for (child in snapshot.children) {
                        // خواندن دستی فیلدها برای جلوگیری از خطای نام‌گذاری
                        val channel = Channel(
                            id = child.child("id").value?.toString()?.toIntOrNull() ?: 0,
                            name_fa = child.child("name_fa").value?.toString() ?: "",
                            category = child.child("category").value?.toString()?.toLongOrNull() ?: 0L,
                            logoUrl = child.child("logoUrl").value?.toString() ?: "",
                            videoUrl = child.child("videoUrl").value?.toString() ?: ""
                        )
                        list.add(channel)
                    }

                    _channels.postValue(list)
                }
            }
            override fun onCancelled(error: DatabaseError) {
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
        if (json != null) {
            val type = object : TypeToken<MutableList<Movie>>() {}.type
            _continueWatchingList.value = gson.fromJson(json, type)
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

    fun fetchTmdbDetails(tmdbId: String) {
        if (detailsCache.containsKey(tmdbId)) return

        viewModelScope.launch(Dispatchers.IO) {
            // خواندن مستقیم از دیتابیس (بدون نیاز به LiveData)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null)
            val apiKey = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()

            if (apiKey.isEmpty()) {
                Log.e("KelkinSync", "خطا: API Key در دیتابیس پیدا نشد!")
                return@launch
            }

            try {
                Log.d("KelkinSync", "درخواست به TMDB برای ID: $tmdbId")
                val details = tmdbApi.getMovieDetails(tmdbId, apiKey)
                detailsCache[tmdbId] = details
                movieDetailsMap.postValue(detailsCache)
            } catch (e: Exception) {
                Log.e("KelkinSync", "خطای TMDB: ${e.message}")
            }
        }
    }

    // Keeping these for other parts of your app
    fun fetchChannels() {
        database.child("channels").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Channel::class.java) }
                _channels.postValue(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    // این متد را به HomeViewModel اضافه کنید
    // استفاده از MovieCreditsResponse به جای CreditsResponse
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

    fun isInitialDataLoaded(): Boolean {
        // آیا لیست فیلم‌ها پر است؟ و آیا حداقل ۵ مورد دیتای TMDB داریم؟
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
            // گزینه پیش‌فرض برای تلویزیون
            list.add(TvCategory(0, "همه شبکه‌ها"))

            // کوئری از جدول جدید tv_categories
            val cursor = db.query("tv_categories", null, null, null, null, null, null)

            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    list.add(TvCategory(id, name))
                } while (cursor.moveToNext())
            }
            cursor.close()

            // ارسال به لایو دیتا مخصوص تلویزیون
            tvCategoriesList.postValue(list)
        }
    }

    fun startSyncTvCategories() {
        // مسیر فایربیس را دوباره چک کن، باید دقیقاً "tv_categories" باشد
        database.child("tv_categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("TVDebug", "فایربیس متصل شد. دیتای دریافتی: ${snapshot.value}")

                viewModelScope.launch(Dispatchers.IO) {
                    val db = dbHelper.writableDatabase
                    db.beginTransaction()
                    try {
                        db.delete("tv_categories", null, null) // پاکسازی جدول
                        for (child in snapshot.children) {
                            val key = child.key ?: ""
                            // اینجا چک کن که کلیدها دقیقا فرمت cat_01 دارند یا نه
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
                    loadTvCategories() // آپدیت لیست بعد از ذخیره
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("TVDebug", "خطای اتصال فایربیس: ${error.message}")
            }
        })
    }


    fun initRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
    }


    // Add this method to HomeViewModel.kt
    suspend fun fetchMovieDetailsForPoster(tmdbId: String): TmdbMovieDetails {
        // We fetch the API key from the local DB just like your other methods
        val apiKey = try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null)
            val key = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()
            key
        } catch (e: Exception) { "" }

        // Use your existing Retrofit service (tmdbApi)
        return tmdbApi.getMovieDetails(tmdbId, apiKey)
    }


}