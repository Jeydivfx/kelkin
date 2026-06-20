package com.example.kelkin.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.kelkin.DataClass.Movie

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kelkin.db"
        private const val DATABASE_VERSION = 3
        const val TABLE_MOVIES = "movies"
        const val TABLE_CHANNELS = "channels"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_TV_CATEGORIES = "tv_categories"
        const val TABLE_CREDENTIALS = "credentials"

        private const val DEFAULT_API_KEY = "46f67bc4b98bf28dd951fc4522edf587"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
        insertDefaultData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHANNELS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TV_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CREDENTIALS")
        onCreate(db)
    }

    private fun createAllTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_CATEGORIES (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL("CREATE TABLE $TABLE_TV_CATEGORIES (id INTEGER PRIMARY KEY, name TEXT)")

        db.execSQL("""
            CREATE TABLE $TABLE_CHANNELS (
                id INTEGER PRIMARY KEY, 
                name_fa TEXT, 
                category_id INTEGER, 
                logoUrl TEXT, 
                videoUrl TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_MOVIES (
                id INTEGER PRIMARY KEY, 
                name_fa TEXT, 
                description_fa TEXT, 
                category INTEGER, 
                tmdb_id TEXT, 
                videoUrl1 TEXT, 
                posterUrl TEXT, 
                lastPosition INTEGER, 
                totalDuration INTEGER
            )
        """.trimIndent())

        db.execSQL("CREATE TABLE $TABLE_CREDENTIALS (id INTEGER PRIMARY KEY DEFAULT 1, apiKey TEXT, userAgent TEXT)")
    }

    private fun insertDefaultData(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put("id", 1)
            put("apiKey", DEFAULT_API_KEY)
            put("userAgent", "Mozilla/5.0")
        }
        db.insertWithOnConflict(TABLE_CREDENTIALS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
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

                    val movie = Movie(
                        id = cursor.getLong(idIndex),
                        name_fa = cursor.getString(nameFaIndex) ?: "",
                        description_fa = cursor.getString(descFaIndex) ?: "",
                        category = cursor.getLong(categoryIndex),
                        tmdb_id = cursor.getString(tmdbIdIndex) ?: "",
                        videoUrl1 = cursor.getString(videoUrl1Index) ?: "",
                        posterUrl = cursor.getString(posterUrlIndex) ?: "",
                        lastPosition = cursor.getLong(lastPosIndex),
                        totalDuration = cursor.getLong(totalDurIndex)
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