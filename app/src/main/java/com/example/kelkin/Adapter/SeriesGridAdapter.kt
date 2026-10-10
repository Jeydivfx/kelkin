package com.example.kelkin.Adapter

import com.example.kelkin.utils.*

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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.kelkin.DataClass.Series
import com.example.kelkin.R
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits

class SeriesGridAdapter(private val onSeriesClick: (Series) -> Unit) :
    ListAdapter<Series, SeriesGridAdapter.SeriesViewHolder>(SeriesDiffCallback()), FocusKeyProvider {


    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).identity())
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.identity()
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.identity() == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_series_grid, parent, false)
        return SeriesViewHolder(view)
    }

    var onVisible: ((Series) -> Unit)? = null
    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = getItem(position)
        onVisible?.invoke(series)
        holder.bind(series)

        holder.itemView.setOnClickListener { onSeriesClick(series) }

        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate().scaleX(1.04f).scaleY(1.04f).setDuration(200).start()
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
            itemView.contentDescription = series.name_fa

            val rating = series.voteAverage
            val ratingStr = if (rating > 0.0) {
                String.format(java.util.Locale.ENGLISH, "%.1f", rating)
            } else {
                "—"
            }

            txtImdbRating.text = ratingStr.toPersianDigits()

            val fullUrl = ImageUrls.poster(series.posterUrl)

            Glide.with(itemView.context)
                .load(fullUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.hero_test_header)
                .error(R.drawable.ic_menu_movie)
                .centerCrop()
                .into(imgPoster)
        }
    }

    class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
        override fun areItemsTheSame(oldItem: Series, newItem: Series) = oldItem.identity() == newItem.identity()
        override fun areContentsTheSame(oldItem: Series, newItem: Series) = oldItem == newItem
    }
}
