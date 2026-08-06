package com.example.kelkin.Adapter

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.kelkin.DataClass.Series
import com.example.kelkin.R
import com.example.kelkin.databinding.ItemMovieCardBinding
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import java.util.Locale

class SeriesAdapter(
    private val onSeriesClick: (Series) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder>() {

    private var seriesList: List<Series> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val binding = ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        holder.bind(seriesList[position], onSeriesClick, position, itemCount)
    }

    override fun getItemCount(): Int = seriesList.size

    fun submitList(newList: List<Series>) {
        this.seriesList = ArrayList(newList)
        notifyDataSetChanged()
    }

    class SeriesViewHolder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(series: Series, onSeriesClick: (Series) -> Unit, position: Int, totalCount: Int) {
            val finalUrl = if (!series.posterUrl.isNullOrEmpty()) {
                "https://image.tmdb.org/t/p/w500${series.posterUrl}"
            } else null

            Glide.with(itemView.context)
                .load(finalUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.hero_test_header)
                .error(R.drawable.hero_test_header)
                .centerCrop()
                .into(binding.moviePoster)

            if (series.voteAverage > 0.0) {
                val ratingFormatted = String.format(Locale.ENGLISH, "%.1f", series.voteAverage).toPersianDigits()
                binding.txtImdbRating.text = ratingFormatted
                binding.txtImdbRating.visibility = View.VISIBLE
            } else {
                binding.txtImdbRating.text = "۰٫۰".toPersianDigits()
            }

            itemView.setOnClickListener { onSeriesClick(series) }

            val progressBar = binding.movieProgress
            progressBar.visibility = View.GONE

            itemView.isFocusable = true
            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (position >= totalCount - 1) return@setOnKeyListener true
                        }
                    }
                }
                false
            }
        }
    }
}