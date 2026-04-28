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
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R

class MovieGridAdapter(private val onMovieClick: (Movie) -> Unit) :
    ListAdapter<Movie, MovieGridAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie)

        holder.itemView.setOnClickListener { onMovieClick(movie) }

        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                view.elevation = 10f
                // برای اطمینان از اینکه کارت بزرگ شده زیر بقیه قرار نمی‌گیره
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

        fun bind(movie: Movie) {
            txtTitle.text = movie.name_fa

            val baseTMDB = "https://image.tmdb.org/t/p/w500"

            val posterPath = movie.posterUrl

            val fullUrl = if (!posterPath.isNullOrEmpty()) {
                val cleanPath = if (posterPath.startsWith("/")) posterPath else "/$posterPath"
                baseTMDB + cleanPath
            } else {
                null
            }


            if (fullUrl != null) {
                Log.d("GlideDebug", "Loading Image: $fullUrl")
            }

            Glide.with(itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.hero_test_header)
                .error(R.drawable.ic_menu_movie)
                .centerCrop()
                .into(imgPoster)
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.tmdb_id == newItem.tmdb_id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }
}