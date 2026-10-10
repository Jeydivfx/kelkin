package com.example.kelkin.utils

import android.content.Context
import com.example.kelkin.DataClass.Movie
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WatchHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kelkin_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    fun load(): MutableList<Movie> = synchronized(lock) {
        runCatching {
            val type = object : TypeToken<MutableList<Movie>>() {}.type
            gson.fromJson<MutableList<Movie>>(prefs.getString("saved_continue_list", "[]"), type) ?: mutableListOf()
        }.getOrDefault(mutableListOf())
    }
    fun save(item: Movie, position: Long, duration: Long, ended: Boolean = false): MutableList<Movie> = synchronized(lock) {
        val list = load()
        val complete = ended || duration > 0 && position >= duration - minOf(60_000L, duration / 20)
        // Do not replace a useful resume point merely because a new playback failed to start.
        if (!complete && position < 2_000L) return@synchronized list
        list.removeAll { it.identity() == item.identity() }
        if (!complete) list.add(0, item.copy(lastPosition = position, totalDuration = duration.coerceAtLeast(0)))
        prefs.edit().putString("saved_continue_list", gson.toJson(list)).apply()
        list
    }
    companion object { private val lock = Any() }
}
