package com.example.kelkin.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UserManager {

    private val db = FirebaseDatabase.getInstance().reference

    fun checkUserActivationStatus(onResult: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            onResult(false)
            return
        }

        db.child("users").child(uid).child("isActive")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isActive = snapshot.getValue(Boolean::class.java) ?: false
                    onResult(isActive)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(false)
                }
            })
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun createUserProfileIfNotExist(uid: String, email: String?) {
        val userRef = db.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val userMap = mapOf(
                        "email" to email,
                        "isActive" to false,
                        "createdAt" to getCurrentDate()
                    )
                    userRef.setValue(userMap)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}