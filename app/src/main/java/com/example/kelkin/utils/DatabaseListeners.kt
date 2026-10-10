package com.example.kelkin.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

fun Query.addViewValueEventListener(owner: LifecycleOwner, listener: ValueEventListener) {
    addValueEventListener(listener)
    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) { removeEventListener(listener); owner.lifecycle.removeObserver(this) }
    })
}
