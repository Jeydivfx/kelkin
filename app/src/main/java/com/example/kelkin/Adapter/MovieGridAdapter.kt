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

    var onVisible: ((Movie) -> Unit)? = null
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        onVisible?.invoke(movie)
        holder.bind(movie)

        holder.itemView.setOnClickListener { onMovieClick(movie) }

        if (!com.example.kelkin.BuildConfig.IS_TV) {
            holder.itemView.setOnFocusChangeListener(null)
            holder.itemView.isFocusable = false
            holder.itemView.animate().cancel()
            holder.itemView.scaleX = 1f; holder.itemView.scaleY = 1f
            holder.itemView.elevation = 0f; holder.itemView.translationZ = 0f
            return
        }

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

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgMoviePoster)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtMovieTitleGrid)
        private val txtImdbRating: TextView = itemView.findViewById(R.id.txtImdbRating)

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

            val request = Glide.with(itemView.context)
                .load(fullUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
            if (com.example.kelkin.BuildConfig.IS_TV) request.placeholder(R.drawable.hero_test_header).error(R.drawable.ic_menu_movie)
            else request.placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .error(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            request.into(imgPoster)
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.identity() == newItem.identity()
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }
}
