package com.example.kelkin.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object UserManager {
    enum class Activation { ACTIVE, INACTIVE }
    suspend fun activation(): Activation = withTimeout(10_000) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withTimeout Activation.INACTIVE
        val snapshot = FirebaseDatabase.getInstance().getReference("users").child(uid).child("isActive").get().awaitResult()
        if (snapshot.getValue(Boolean::class.java) == true) Activation.ACTIVE else Activation.INACTIVE
    }
    suspend fun createUserProfileIfNotExist(uid: String, email: String?) = withTimeout(10_000) {
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid)
        suspendCancellableCoroutine<Unit> { continuation ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(current: MutableData): Transaction.Result {
                    if (current.value != null) return Transaction.abort()
                    current.value = mapOf("email" to email.orEmpty(), "isActive" to false, "createdAt" to ServerValue.TIMESTAMP)
                    return Transaction.success(current)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                    if (continuation.isActive) {
                        if (error != null) continuation.resumeWithException(error.toException()) else continuation.resume(Unit)
                    }
                }
            }, false)
        }
    }
}
