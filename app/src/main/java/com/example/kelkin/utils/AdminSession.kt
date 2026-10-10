package com.example.kelkin.utils

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException

/** Separate authentication prevents an admin login from replacing the viewer's Google account. */
object AdminSession {
    class RoleCheckException(cause: Exception) : Exception("Admin role check failed", cause)
    private val app by lazy {
        runCatching { FirebaseApp.getInstance("kelkin-admin") }.getOrElse {
            FirebaseApp.initializeApp(FirebaseApp.getInstance().applicationContext, FirebaseApp.getInstance().options, "kelkin-admin")
        }
    }
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance(app) }
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(app) }
    private var verifiedUid: String? = null
    val isAuthorized get() = verifiedUid != null && verifiedUid == auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): Boolean = withTimeout(15_000) {
        signOut()
        auth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
        try { verify() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw RoleCheckException(e) }
    }
    suspend fun verify(): Boolean = withTimeout(10_000) {
        verifiedUid = null
        val uid = auth.currentUser?.uid ?: return@withTimeout false
        val allowed = database.getReference("admins").child(uid).get().awaitResult().getValue(Boolean::class.java) == true
        if (allowed) verifiedUid = uid else auth.signOut()
        allowed
    }
    fun signOut() { verifiedUid = null; auth.signOut() }
}
