package com.example.kelkin.utils

import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import com.example.kelkin.BuildConfig
import com.example.kelkin.R

/** Animate reading order, rather than the single wrapper around all the header content. */
internal fun View.animateDetailEntrance() {
    if (!BuildConfig.IS_TV) return
    doOnPreDraw {
        val content = findViewById<ViewGroup>(R.id.detail_content) ?: return@doOnPreDraw
        val elements = mutableListOf<View>()
        fun collect(group: ViewGroup) {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (child.visibility != View.VISIBLE) continue
                when {
                    child.id == R.id.movie_info_row || child is androidx.recyclerview.widget.RecyclerView -> elements += child
                    child is TextView -> elements += child
                    child is ViewGroup -> collect(child)
                }
            }
        }
        collect(content)
        elements.forEachIndexed { index, child ->
            child.startAnimation(AnimationUtils.loadAnimation(context, R.anim.layout_animation).apply {
                startOffset = index * 65L
            })
        }
        // Genres may arrive from TMDB after the first frame, while the empty label is GONE.
        findViewById<TextView>(R.id.txtGenres)?.let { genres ->
            if (genres.text.isBlank()) {
                var watcher: android.text.TextWatcher? = null
                watcher = genres.addTextChangedListener { text ->
                    if (!text.isNullOrBlank()) {
                        genres.removeTextChangedListener(watcher)
                        genres.doOnPreDraw {
                            genres.startAnimation(AnimationUtils.loadAnimation(context, R.anim.layout_animation))
                        }
                    }
                }
            }
        }
    }
}
