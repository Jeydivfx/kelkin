package com.example.kelkin.utils

import com.google.firebase.database.*

object FirebaseManager {
    fun getDatabase() = AdminSession.database
    fun getMoviesRef() = getDatabase().getReference("movies")
    fun getCategoryRef() = getDatabase().getReference("category")
    fun getTvCategoriesRef() = getDatabase().getReference("tv_categories")
    fun getChannelsRef() = getDatabase().getReference("channels")
    fun getSeriesRef() = getDatabase().getReference("series")
    fun getSeriesCategoryRef() = getDatabase().getReference("series_categories")
    private fun reference(type: String): DatabaseReference? = when (type) {
        "movie" -> getMoviesRef(); "series" -> getSeriesRef(); "channels" -> getChannelsRef()
        "category" -> getCategoryRef(); "tv_category" -> getTvCategoriesRef(); "series_category" -> getSeriesCategoryRef()
        else -> null
    }
    fun saveOrUpdate(type: String, id: String, data: Map<String, Any?>, onResult: (Boolean) -> Unit) {
        if (!AdminSession.isAuthorized) { onResult(false); return }
        val ref = reference(type) ?: run { onResult(false); return }
        ref.child(id).updateChildren(data).addOnCompleteListener { onResult(it.isSuccessful) }
    }
    fun deleteItem(type: String, id: String, onResult: (Boolean) -> Unit) {
        if (!AdminSession.isAuthorized) { onResult(false); return }
        val ref = reference(type) ?: run { onResult(false); return }
        ref.child(id).removeValue().addOnCompleteListener { onResult(it.isSuccessful) }
    }
    fun createItem(type: String, data: Map<String, Any?>, onResult: (String?) -> Unit) {
        if (!AdminSession.isAuthorized) { onResult(null); return }
        val ref = reference(type) ?: run { onResult(null); return }
        val key = ref.push().key ?: run { onResult(null); return }
        // Allocation is atomic across all administrators. Reserving a number before writing
        // may leave a harmless gap after a failed write, but never overwrites another item.
        getDatabase().getReference("catalog_counters").child(type).runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                current.value = maxOf((current.value as? Number)?.toLong() ?: 0L, System.currentTimeMillis()) + 1
                return Transaction.success(current)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null || !committed || !AdminSession.isAuthorized) { onResult(null); return }
                val id = snapshot?.getValue(Long::class.java) ?: run { onResult(null); return }
                ref.child(key).setValue(data + ("id" to id)).addOnCompleteListener { onResult(if (it.isSuccessful) key else null) }
            }
        }, false)
    }
}
