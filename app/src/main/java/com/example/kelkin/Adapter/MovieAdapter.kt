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
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private var movies: List<Movie> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position], onMovieClick)
    }

    override fun getItemCount(): Int {
        val size = movies.size
        // لاگ برای تست:
        if (size == 0) Log.d("KelkinDebug", "آداپتور لیست خالی دریافت کرد!")
        return size
    }

    fun submitList(newList: List<Movie>) {
        // ایجاد یک لیست جدید از لیست ورودی تا تغییرات حتما اعمال شود
        this.movies = ArrayList(newList)
        notifyDataSetChanged()
    }

    class MovieViewHolder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie, onMovieClick: (Movie) -> Unit) {
            Log.d("KelkinDebug", "در حال نمایش فیلم: ${movie.name_fa}")


            val finalUrl = if (!movie.posterUrl.isNullOrEmpty()) {
                "https://image.tmdb.org/t/p/w500${movie.posterUrl}"
            } else {
                null
            }

            Log.d("KelkinDebug", "تلاش برای لود آدرس: $finalUrl")

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