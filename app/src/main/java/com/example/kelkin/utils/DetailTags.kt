package com.example.kelkin.utils

import android.view.View
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.example.kelkin.DataClass.Language
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.Quality
import com.example.kelkin.R

fun View.hideEmptyDetailTags() {
    for (id in listOf(R.id.txtTitleEnglish, R.id.txtRating, R.id.txtQuality, R.id.txtLanguage, R.id.txtYear, R.id.txtGenres)) {
        findViewById<TextView>(id)?.apply {
            visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
            addTextChangedListener {
                val wasHidden = visibility != View.VISIBLE
                visibility = if (it.isNullOrBlank()) View.GONE else View.VISIBLE
                // Metadata may arrive after the parent layout's entrance animation.
                if (id == R.id.txtTitleEnglish && wasHidden && visibility == View.VISIBLE) {
                    startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.layout_animation))
                }
            }
        }
    }
}
fun View.bindLocalDetailTags(card: Movie, languages: List<Language>, qualities: List<Quality>) {
    val language = languages.find { it.id.toString() == card.language || "lan_%02d".format(it.id) == card.language || it.name == card.language }?.name
    val quality = qualities.find { it.id.toString() == card.quality || "qu_%02d".format(it.id) == card.quality || it.name == card.quality }?.name
    fun readable(value: String) = value.takeUnless { it.toLongOrNull() != null || it.startsWith("lan_") || it.startsWith("qu_") }.orEmpty()
    findViewById<TextView>(R.id.txtLanguage)?.text = language ?: readable(card.language)
    findViewById<TextView>(R.id.txtQuality)?.text = quality ?: readable(card.quality)
}
