package com.example.kelkin.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.kelkin.DataClass.Movie

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kelkin.db"
        private const val DATABASE_VERSION = 8
        const val TABLE_MOVIES = "movies"
        const val TABLE_CHANNELS = "channels"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_TV_CATEGORIES = "tv_categories"
        const val TABLE_SERIES_CATEGORIES = "series_categories"
        const val TABLE_LANGUAGES = "languages"
        const val TABLE_QUALITIES = "qualities"
        const val TABLE_SERIES = "series"
        const val TABLE_CREDENTIALS = "credentials"
        const val TABLE_STREAM_CONFIG = "stream_config"
        private const val DEFAULT_API_KEY = "46f67bc4b98bf28dd951fc4522edf587"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
        insertDefaultData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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

    private fun createMissingTables(db: SQLiteDatabase) = createAllTables(db)

    private fun createAllTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TV_CATEGORIES (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SERIES_CATEGORIES (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_LANGUAGES (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_QUALITIES (id INTEGER PRIMARY KEY, name TEXT)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CHANNELS (
                id INTEGER PRIMARY KEY, 
                name_fa TEXT, 
                category_id INTEGER, 
                logoUrl TEXT, 
                videoUrl TEXT,
                source TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_MOVIES (
                id INTEGER PRIMARY KEY, 
                name_fa TEXT, 
                description_fa TEXT, 
                category INTEGER, 
                tmdb_id TEXT, 
                videoUrl1 TEXT, 
                posterUrl TEXT, 
                lastPosition INTEGER, 
                totalDuration INTEGER,
                voteAverage INTEGER,
                language TEXT,
                quality TEXT,
                remoteKey TEXT DEFAULT ''
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SERIES (
                id INTEGER PRIMARY KEY, 
                name_fa TEXT, 
                description_fa TEXT, 
                category INTEGER, 
                tmdb_id TEXT,
                posterUrl TEXT,
                lastPosition INTEGER, 
                totalDuration INTEGER,
                voteAverage INTEGER,
                seasons_json TEXT,
                language TEXT,
                quality TEXT,
                remoteKey TEXT DEFAULT ''
            )
        """.trimIndent())

        db.execSQL("""
        CREATE TABLE IF NOT EXISTS $TABLE_STREAM_CONFIG (
            id INTEGER PRIMARY KEY DEFAULT 1, 
            userAgent TEXT, 
            referrer TEXT, 
            origin TEXT
        )
        """.trimIndent())

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CREDENTIALS (id INTEGER PRIMARY KEY DEFAULT 1, apiKey TEXT, userAgent TEXT)")
    }

    private fun insertDefaultData(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put("id", 1)
            put("apiKey", DEFAULT_API_KEY)
            put("userAgent", "Mozilla/5.0")
        }
        db.insertWithOnConflict(TABLE_CREDENTIALS, null, values, SQLiteDatabase.CONFLICT_REPLACE)

        val configValues = ContentValues().apply {
            put("id", 1)
            put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            put("referrer", "https://www.aparatchi.com/")
            put("origin", "https://www.aparatchi.com")
        }
        db.insertWithOnConflict(TABLE_STREAM_CONFIG, null, configValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun searchMovies(query: String): List<Movie> {
        val movieList = mutableListOf<Movie>()
        val db = readableDatabase
        val searchQuery = "%${query.trim()}%"

        val sql = "SELECT * FROM $TABLE_MOVIES WHERE name_fa LIKE ? OR description_fa LIKE ?"

        val cursor = db.rawQuery(sql, arrayOf(searchQuery, searchQuery))

        try {
            if (cursor.moveToFirst()) {
                do {
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val nameFaIndex = cursor.getColumnIndexOrThrow("name_fa")
                    val descFaIndex = cursor.getColumnIndexOrThrow("description_fa")
                    val categoryIndex = cursor.getColumnIndexOrThrow("category")
                    val tmdbIdIndex = cursor.getColumnIndexOrThrow("tmdb_id")
                    val videoUrl1Index = cursor.getColumnIndexOrThrow("videoUrl1")
                    val posterUrlIndex = cursor.getColumnIndexOrThrow("posterUrl")
                    val lastPosIndex = cursor.getColumnIndexOrThrow("lastPosition")
                    val totalDurIndex = cursor.getColumnIndexOrThrow("totalDuration")
                    val languageIndex = cursor.getColumnIndexOrThrow("language")
                    val qualityIndex = cursor.getColumnIndexOrThrow("quality")

                    val movie = Movie(
                        id = cursor.getLong(idIndex),
                        name_fa = cursor.getString(nameFaIndex) ?: "",
                        description_fa = cursor.getString(descFaIndex) ?: "",
                        category = cursor.getLong(categoryIndex),
                        tmdb_id = cursor.getString(tmdbIdIndex) ?: "",
                        videoUrl1 = cursor.getString(videoUrl1Index) ?: "",
                        posterUrl = cursor.getString(posterUrlIndex) ?: "",
                        lastPosition = cursor.getLong(lastPosIndex),
                        totalDuration = cursor.getLong(totalDurIndex),
                        language = cursor.getString(languageIndex) ?: "",
                        quality = cursor.getString(qualityIndex) ?: ""
                    )
                    movieList.add(movie)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }

        return movieList
    }
}