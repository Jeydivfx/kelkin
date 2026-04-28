package com.example.kelkin.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.TmdbMovieDetails
import com.example.kelkin.R
import com.example.kelkin.databinding.ItemMovieCardBinding

class MovieAdapter(
    private var movies: List<Movie> = emptyList(),
    private val onMovieClick: (Movie) -> Unit


) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.bind(movie, onMovieClick)
    }

    override fun getItemCount(): Int = movies.size

    fun submitList(newList: List<Movie>) {
        this.movies = ArrayList(newList)
        notifyDataSetChanged()
    }

    class MovieViewHolder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie, onMovieClick: (Movie) -> Unit) {

            val finalUrl = if (!movie.posterUrl.isNullOrEmpty()) {
                "https://image.tmdb.org/t/p/w500${movie.posterUrl}"
            } else {
                null
            }

            Glide.with(itemView.context)
                .load(finalUrl)
                .placeholder(R.drawable.hero_test_header)
                .error(R.drawable.hero_test_header)
                .centerCrop()
                .into(binding.moviePoster)

            itemView.setOnClickListener {
                onMovieClick(movie)
            }

            val progressBar = binding.movieProgress
            if (movie.lastPosition > 0 && movie.totalDuration > 0) {
                progressBar.visibility = View.VISIBLE
                val progressPercent = (movie.lastPosition * 100 / movie.totalDuration).toInt()
                progressBar.progress = progressPercent
            } else {
                progressBar.visibility = View.GONE
            }

        }
    }
}