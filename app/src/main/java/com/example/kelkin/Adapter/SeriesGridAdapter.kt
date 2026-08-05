package com.example.kelkin.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kelkin.DataClass.Series
import com.example.kelkin.R
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits

class SeriesGridAdapter(private val onSeriesClick: (Series) -> Unit) :
    ListAdapter<Series, SeriesGridAdapter.SeriesViewHolder>(SeriesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_series_grid, parent, false)
        return SeriesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = getItem(position)
        holder.bind(series)

        holder.itemView.setOnClickListener { onSeriesClick(series) }

        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                view.elevation = 10f
                view.z = 10f
            } else {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                view.elevation = 0f
                view.z = 0f
            }
        }

        holder.itemView.setOnKeyListener(null)
    }

    class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgSeriesPoster)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtSeriesTitleGrid)
        private val txtImdbRating: TextView = itemView.findViewById(R.id.txtImdbRating)

        fun bind(series: Series) {
            txtTitle.text = series.name_fa

            val rating = series.voteAverage
            val ratingStr = if (rating > 0.0) {
                String.format(java.util.Locale.ENGLISH, "%.1f", rating)
            } else {
                "--"
            }

            txtImdbRating.text = ratingStr.toPersianDigits()

            val baseTMDB = "https://image.tmdb.org/t/p/w500"
            val posterPath = series.posterUrl

            Log.d("SeriesSearchTest", "PosterRawPath: ${series.posterUrl}")

            val fullUrl = if (!posterPath.isNullOrEmpty()) {
                val cleanPath = if (posterPath.startsWith("/")) posterPath else "/$posterPath"
                baseTMDB + cleanPath
            } else {
                null
            }

            Log.d("SeriesSearchTest", "نام سریال: ${series.name_fa} | لینک پوستر: ${series.posterUrl}")

            if (fullUrl != null) {
                Log.d("SeriesSearchTest", "Loading Image: $fullUrl")
            }

            Glide.with(itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.hero_test_header)
                .error(R.drawable.ic_menu_movie)
                .centerCrop()
                .into(imgPoster)
        }
    }

    class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
        override fun areItemsTheSame(oldItem: Series, newItem: Series) = oldItem.tmdb_id == newItem.tmdb_id
        override fun areContentsTheSame(oldItem: Series, newItem: Series) = oldItem == newItem
    }
}