from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
p=j+'utils/DatabaseHelper.kt'
edit(p,'DATABASE_VERSION = 7','DATABASE_VERSION = 8')
method(p,'onUpgrade','''    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Preserve the version-7 catalog and configuration. Older installations are also
        // repaired column-by-column, rather than dropping users' cached data.
        createMissingTables(db)
        val columns = mapOf(
            "movies" to mapOf("remoteKey" to "TEXT DEFAULT ''", "posterUrl" to "TEXT DEFAULT ''", "lastPosition" to "INTEGER DEFAULT 0", "totalDuration" to "INTEGER DEFAULT 0", "voteAverage" to "REAL DEFAULT 0", "language" to "TEXT DEFAULT ''", "quality" to "TEXT DEFAULT ''"),
            "series" to mapOf("remoteKey" to "TEXT DEFAULT ''", "posterUrl" to "TEXT DEFAULT ''", "seasons_json" to "TEXT DEFAULT '{}'", "lastPosition" to "INTEGER DEFAULT 0", "totalDuration" to "INTEGER DEFAULT 0", "voteAverage" to "REAL DEFAULT 0", "language" to "TEXT DEFAULT ''", "quality" to "TEXT DEFAULT ''"),
            "channels" to mapOf("source" to "TEXT DEFAULT ''")
        )
        columns.forEach { (table, additions) ->
            val existing = mutableSetOf<String>()
            db.rawQuery("PRAGMA table_info($table)", null).use { c ->
                while (c.moveToNext()) existing += c.getString(c.getColumnIndexOrThrow("name"))
            }
            additions.forEach { (column, definition) ->
                if (column !in existing) db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        }
    }

    private fun createMissingTables(db: SQLiteDatabase) = createAllTables(db)''')
edit(p,'CREATE TABLE ', 'CREATE TABLE IF NOT EXISTS ')
edit(p,'                quality TEXT','                quality TEXT,\n                remoteKey TEXT DEFAULT \'\'')
edit(p,'language = cursor.getString(tmdbIdIndex)', 'language = cursor.getString(languageIndex)')
edit(p,'quality = cursor.getString(tmdbIdIndex)', 'quality = cursor.getString(qualityIndex)')

src=j+'ViewModel/HomeViewModel.kt'
dst=j+'utils/CatalogRepository.kt'
text=read(src).replace('package com.example.kelkin.ViewModels','package com.example.kelkin.utils')
text=text.replace('class HomeViewModel(application: Application) : AndroidViewModel(application) {','class CatalogRepository private constructor(private val application: Application) {')
text=text.replace('viewModelScope','scope')
text=text.replace('import androidx.lifecycle.scope','import kotlinx.coroutines.CoroutineScope')
text=text.replace('import kotlinx.coroutines.launch','import kotlinx.coroutines.*\nimport kotlinx.coroutines.sync.Semaphore\nimport kotlinx.coroutines.sync.withPermit\nimport com.google.firebase.database.DatabaseReference\nimport com.google.firebase.auth.FirebaseAuth')
text=text.replace('    private val dbHelper', '''    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
        syncError.value = null
        startSyncLanguages(); startSyncQualities(); startSyncMovies(); startSyncCategories()
        startSyncChannels(); startSyncTvCategories(); startSyncSeries(); startSyncSeriesCategories()
        syncStreamHeaders()
    }
    private val dbHelper''')
text=re.sub(r'        startSyncLanguages\(\)\n.*?        syncStreamHeaders\(\)\n', '        loadChannelsFromLocalDatabase()\n        loadLocalHeaders()\n        loadMetadata()\n',text,count=1,flags=re.S)
text=re.sub(r'database\.child\("([^\"]+)"\)\.addValueEventListener\(',r'listen(database.child("\1"), ',text)
text=text.replace('database.child("app_config").child("stream_headers").addValueEventListener(', 'listen(database.child("app_config").child("stream_headers"), ')
text=text.replace('override fun onCancelled(error: DatabaseError) {}','override fun onCancelled(error: DatabaseError) { syncError.postValue("اتصال به سرور برقرار نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود.") }')
# All error handlers provide a visible state; keep their logs as well.
text=re.sub(r'(override fun onCancelled\(error: DatabaseError\) \{\n)(?!\s*syncError)',r'\1                syncError.postValue("اتصال به سرور برقرار نشد؛ دوباره تلاش کنید.")\n',text)
text=text.replace('            moviesList.postValue(mList)','            catalogLoading.postValue(false)\n            moviesList.postValue(mList)')
text=text.replace('                        quality = cursor.getString(qualCol) ?: ""','                        quality = cursor.getString(qualCol) ?: "",\n                        remoteKey = cursor.getString(cursor.getColumnIndexOrThrow("remoteKey")) ?: ""')
text=text.replace('                        quality = quality\n','                        quality = quality,\n                        remoteKey = cursor.getString(cursor.getColumnIndexOrThrow("remoteKey")) ?: ""\n')
# Sync copies every field needed offline and uses the backend identity.
needle='                            val values = ContentValues().apply {\n                                put("name_fa",'
text=text.replace(needle,'''                            val values = ContentValues().apply {
                                put("id", child.child("id").value?.toString()?.toLongOrNull() ?: stableContentId(child.key.orEmpty()))
                                put("remoteKey", child.key.orEmpty())
                                put("posterUrl", child.child("posterUrl").value?.toString().orEmpty())
                                put("voteAverage", child.child("voteAverage").value?.toString()?.toDoubleOrNull() ?: 0.0)
                                put("name_fa",''',1)
needle='                                put("id", child.child("id").value?.toString()?.toLongOrNull() ?: 0L)'
text=text.replace(needle,'''                                put("id", child.child("id").value?.toString()?.toLongOrNull() ?: stableContentId(child.key.orEmpty()))
                                put("remoteKey", child.key.orEmpty())
                                put("posterUrl", child.child("posterUrl").value?.toString().orEmpty())
                                put("voteAverage", child.child("voteAverage").value?.toString()?.toDoubleOrNull() ?: 0.0)''')
text=text.replace('val sourcesSnapshot = child.child("sources").value\n                            val sourceJsonString = if (sourcesSnapshot != null) gson.toJson(sourcesSnapshot) else null','val sourceJsonString = sourceJson(child)')
text=text.replace('val sourcesSnapshot = child.child("sources").value\n                    val sourceJsonString = if (sourcesSnapshot != null) gson.toJson(sourcesSnapshot) else null','val sourceJsonString = sourceJson(child)')
text=text.replace('db.insert("movies", null, values)','db.insertOrThrow("movies", null, values)').replace('db.insert("series", null, values)','db.insertOrThrow("series", null, values)')
write(dst,text)
method(dst,'loadMyList','''    private fun loadMyList() {
        val type = object : TypeToken<MutableList<Movie>>() {}.type
        _myList.value = runCatching { gson.fromJson<MutableList<Movie>>(prefs.getString("saved_my_list", "[]"), type) }.getOrNull() ?: mutableListOf()
    }''')
method(dst,'addToMyList','''    fun addToMyList(movie: Movie) {
        val currentList = _myList.value.orEmpty().toMutableList()
        if (currentList.none { it.identity() == movie.identity() }) {
            currentList.add(0, movie.copy())
            _myList.value = currentList
            saveMyList()
        }
    }''')
method(dst,'removeFromMyList','''    fun removeFromMyList(movie: Movie) {
        _myList.value = _myList.value.orEmpty().filterNot { it.identity() == movie.identity() }.toMutableList()
        saveMyList()
    }''')
edit(dst,'fun isInMyList(tmdbId: String): Boolean = _myList.value?.any { it.tmdb_id == tmdbId } ?: false','fun isInMyList(movie: Movie): Boolean = _myList.value.orEmpty().any { it.identity() == movie.identity() }')
method(dst,'loadContinueList','''    fun loadContinueList() { _continueWatchingList.value = history.load() }''')
method(dst,'updateContinueWatching','''    fun updateContinueWatching(movie: Movie, position: Long, duration: Long) {
        _continueWatchingList.value = history.save(movie, position, duration)
    }''')
method(dst,'fetchTmdbDetails','''    fun fetchTmdbDetails(tmdbId: String) = fetchMetadata("movie", tmdbId)

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
    }''')
method(dst,'fetchSeriesTmdbDetails','''    fun fetchSeriesTmdbDetails(tmdbId: String) = fetchMetadata("series", tmdbId)''')
method(dst,'getMovieFullDetails','''    suspend fun getMovieFullDetails(tmdbId: String): Pair<TmdbMovieDetails?, MovieCreditsResponse?> {
        return try {
            val details = fetchMovieDetailsForPoster(tmdbId)
            val credits = runCatching { tmdbApi.getMovieCredits(tmdbId, apiKey()) }.getOrNull()
            details to credits
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { null to null }
    }''')
method(dst,'fetchMovieDetailsForPoster','''    suspend fun fetchMovieDetailsForPoster(tmdbId: String): TmdbMovieDetails {
        return detailsCache[metadataKey("movie", tmdbId)] ?: tmdbApi.getMovieDetails(tmdbId, apiKey())
    }

    private suspend fun apiKey(): String = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery("SELECT apiKey FROM credentials WHERE id = 1", null).use {
            if (it.moveToFirst()) it.getString(0).orEmpty() else ""
        }
    }''')
method(dst,'fetchTvDetailsForPoster','''    suspend fun fetchTvDetailsForPoster(tvId: String): TmdbTvDetails? {
        return try { tmdbApi.getTvDetails(tvId, apiKey()) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { null }
    }''')
edit(dst,'minimumFetchIntervalInSeconds = 0','minimumFetchIntervalInSeconds = 3600')
edit(dst,'                streamHeaders.postValue(StreamHeaders(ua, ref, ori))','''                if (snapshot.exists()) {
                    val headers = StreamHeaders(ua, ref, ori)
                    scope.launch(Dispatchers.IO) {
                        val values = ContentValues().apply {
                            put("id", 1); put("userAgent", ua); put("referrer", ref); put("origin", ori)
                        }
                        dbHelper.writableDatabase.insertWithOnConflict("stream_config", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                        streamHeaders.postValue(headers)
                    }
                }''')
text=read(dst)
pos=text.rfind('\n}')
text=text[:pos]+'''
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
                    id = c.getInt(c.getColumnIndexOrThrow("id")),
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
'''+text[pos:]
write(dst,text)
fields=['moviesList','movieDetailsMap','streamHeaders','myList','categoriesList','continueWatchingList','channels','seriesList','seriesCategoriesList','languagesList','qualitiesList','tvCategoriesList','syncError','catalogLoading']
wrapper='''package com.example.kelkin.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.utils.CatalogRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatalogRepository.get(application)
    init { repository.acquire() }
'''+''.join('    val '+f+' = repository.'+f+'\n' for f in fields)+'''
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
'''
write(src,wrapper)
