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
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits

class MovieGridAdapter(private val onMovieClick: (Movie) -> Unit) :
    ListAdapter<Movie, MovieGridAdapter.MovieViewHolder>(MovieDiffCallback()), FocusKeyProvider {


    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).identity())
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.identity()
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.identity() == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        return MovieViewHolder(view)
    }

    var yearProvider: (Movie) -> String = { "" }
    var onVisible: ((Movie) -> Unit)? = null
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        onVisible?.invoke(movie)
        holder.bind(movie)
        holder.bindYear(yearProvider(movie))

        holder.itemView.setOnClickListener { onMovieClick(movie) }

        bindPosterFocus(holder.itemView)

        holder.itemView.setOnKeyListener(null)

    }

    fun refreshYears() {
        if (com.example.kelkin.BuildConfig.IS_TV && itemCount > 0) notifyItemRangeChanged(0, itemCount, "year")
    }
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == "year" }) holder.bindYear(yearProvider(getItem(position)))
        else super.onBindViewHolder(holder, position, payloads)
    }
    override fun onViewAttachedToWindow(holder: MovieViewHolder) {
        super.onViewAttachedToWindow(holder)
        bindPosterFocus(holder.itemView)
    }
    override fun onViewDetachedFromWindow(holder: MovieViewHolder) {
        resetPosterFocus(holder.itemView)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: MovieViewHolder) {
        holder.itemView.setOnFocusChangeListener(null)
        resetPosterFocus(holder.itemView)
        super.onViewRecycled(holder)
    }

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgMoviePoster)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtMovieTitleGrid)
        private val txtImdbRating: TextView = itemView.findViewById(R.id.txtImdbRating)

        fun bindYear(year: String) {
            itemView.findViewById<TextView>(R.id.home_reference_year)?.text = year.toPersianDigits()
        }
        fun bind(movie: Movie) {
            txtTitle.text = movie.name_fa
            itemView.contentDescription = movie.name_fa

            val rating = movie.voteAverage
            val ratingStr = if (rating > 0.0) {
                String.format(java.util.Locale.ENGLISH, "%.1f", rating)
            } else {
                "—"
            }

            txtImdbRating.text = ratingStr.toPersianDigits()

            val fullUrl = ImageUrls.poster(movie.posterUrl)

            imgPoster.setBackgroundResource(R.drawable.mobile_poster_placeholder)
            val request = Glide.with(itemView.context)
                .load(fullUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
            request.placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .error(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            request.into(imgPoster)
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.identity() == newItem.identity()
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }
}
