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

    var yearProvider: (Series) -> String = { "" }
    var onVisible: ((Series) -> Unit)? = null
    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = getItem(position)
        onVisible?.invoke(series)
        holder.bind(series)
        holder.bindYear(yearProvider(series))

        holder.itemView.setOnClickListener { onSeriesClick(series) }

        bindPosterFocus(holder.itemView)

        holder.itemView.setOnKeyListener(null)
    }

    fun refreshYears() {
        if (com.example.kelkin.BuildConfig.IS_TV && itemCount > 0) notifyItemRangeChanged(0, itemCount, "year")
    }
    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == "year" }) holder.bindYear(yearProvider(getItem(position)))
        else super.onBindViewHolder(holder, position, payloads)
    }
    override fun onViewAttachedToWindow(holder: SeriesViewHolder) {
        super.onViewAttachedToWindow(holder)
        bindPosterFocus(holder.itemView)
    }
    override fun onViewDetachedFromWindow(holder: SeriesViewHolder) {
        resetPosterFocus(holder.itemView)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: SeriesViewHolder) {
        holder.itemView.setOnFocusChangeListener(null)
        resetPosterFocus(holder.itemView)
        super.onViewRecycled(holder)
    }

    class SeriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgSeriesPoster)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtSeriesTitleGrid)
        private val txtImdbRating: TextView = itemView.findViewById(R.id.txtImdbRating)

        fun bindYear(year: String) {
            itemView.findViewById<TextView>(R.id.home_reference_year)?.text = year.toPersianDigits()
        }
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

            imgPoster.setBackgroundResource(R.drawable.mobile_poster_placeholder)
            Glide.with(itemView.context)
                .load(fullUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .error(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .fitCenter()
                .into(imgPoster)
        }
    }

    class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
        override fun areItemsTheSame(oldItem: Series, newItem: Series) = oldItem.identity() == newItem.identity()
        override fun areContentsTheSame(oldItem: Series, newItem: Series) = oldItem == newItem
    }
}
