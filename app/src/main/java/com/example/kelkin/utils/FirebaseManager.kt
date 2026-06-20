package com.example.kelkin.utils

import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {
    private val database = FirebaseDatabase.getInstance()

    fun getMoviesRef() = database.getReference("movies")
    fun getCategoryRef() = database.getReference("category")
    fun getTvCategoriesRef() = database.getReference("tv_categories")
    fun getChannelsRef() = database.getReference("channels")

    fun saveOrUpdate(refType: String, id: String, data: Map<String, Any>, onResult: (Boolean) -> Unit) {
        val ref = when(refType) {
            "movie" -> getMoviesRef()
            "category" -> getCategoryRef()
            "tv_category" -> getTvCategoriesRef()
            "channels" -> getChannelsRef()
            else -> return
        }

        ref.child(id).updateChildren(data)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun getDatabase(): com.google.firebase.database.FirebaseDatabase {
        return com.google.firebase.database.FirebaseDatabase.getInstance()
    }


    fun deleteItem(refType: String, id: String, onResult: (Boolean) -> Unit) {
        val ref = when(refType) {
            "movie" -> getMoviesRef()
            "category" -> getCategoryRef()
            "tv_category" -> getTvCategoriesRef()
            "channels" -> getChannelsRef()
            else -> return
        }

        ref.child(id).removeValue()
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }
}