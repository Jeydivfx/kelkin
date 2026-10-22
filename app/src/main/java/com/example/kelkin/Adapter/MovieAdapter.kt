package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R
import com.example.kelkin.databinding.ItemMovieCardBinding
import com.example.kelkin.utils.*
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import java.util.Locale

class MovieAdapter(private val onMovieClick: (Movie) -> Unit) :
    ListAdapter<Movie, MovieAdapter.MovieViewHolder>(object : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(old: Movie, new: Movie) = old.identity() == new.identity()
        override fun areContentsTheSame(old: Movie, new: Movie) = old == new
    }), FocusKeyProvider {
    var showFullPoster = false
    var fillGridCell = false
    var posterSizeScale = 1f
    var homePolish = false
    var yearProvider: (Movie) -> String = { "" }
    var onVisible: ((Movie) -> Unit)? = null
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).identity())
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.identity()
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.identity() == key }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = if (homePolish && com.example.kelkin.BuildConfig.IS_TV) {
            ItemMovieCardBinding.bind(LayoutInflater.from(parent.context).inflate(R.layout.item_home_caption_poster, parent, false))
        } else ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (fillGridCell) {
            binding.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
        }
        if (posterSizeScale != 1f) {
            binding.root.layoutParams = binding.root.layoutParams.apply {
                width = (width * posterSizeScale).toInt()
                height = (height * posterSizeScale).toInt()
            }
        }
        if (homePolish && com.example.kelkin.BuildConfig.IS_TV) {
            binding.root.stateListAnimator = null
            (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                marginStart = (7 * parent.resources.displayMetrics.density).toInt()
                marginEnd = marginStart
            }
        }
        return MovieViewHolder(binding)
    }
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie, onMovieClick, showFullPoster, homePolish)
        if (homePolish && com.example.kelkin.BuildConfig.IS_TV) holder.bindCaption(movie.name_fa, yearProvider(movie))
        onVisible?.invoke(movie)
    }
    fun refreshHomeCaptions() {
        if (homePolish && com.example.kelkin.BuildConfig.IS_TV && itemCount > 0) notifyItemRangeChanged(0, itemCount, "caption")
    }
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == "caption" }) {
            val movie = getItem(position)
            holder.bindCaption(movie.name_fa, yearProvider(movie))
        } else super.onBindViewHolder(holder, position, payloads)
    }
    class MovieViewHolder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun clearFocusEffect() { resetPosterFocus(itemView) }
        fun bindCaption(title: String, year: String) {
            itemView.findViewById<android.widget.TextView>(R.id.home_reference_title)?.text = title
            itemView.findViewById<android.widget.TextView>(R.id.home_reference_year)?.text = year.toPersianDigits()
        }
        fun bind(movie: Movie, onClick: (Movie) -> Unit, showFullPoster: Boolean, homePolish: Boolean) {
            clearFocusEffect()
            if (com.example.kelkin.BuildConfig.IS_TV) {
                bindPosterFocus(itemView)

            }
            itemView.contentDescription = movie.name_fa
            val posterRequest = Glide.with(itemView).load(ImageUrls.poster(movie.posterUrl))
                .placeholder(R.drawable.hero_test_header).error(R.drawable.hero_test_header)
            if (!com.example.kelkin.BuildConfig.IS_TV) posterRequest
                .placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .error(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            if (showFullPoster) posterRequest.fitCenter() else posterRequest.centerCrop()
            if (homePolish) posterRequest.transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(250))
            posterRequest.into(binding.moviePoster)
            (binding.txtImdbRating.parent as View).visibility = if (movie.voteAverage > 0) View.VISIBLE else View.GONE
            binding.txtImdbRating.text = String.format(Locale.ENGLISH, "%.1f", movie.voteAverage).toPersianDigits()
            binding.movieProgress.visibility = if (movie.lastPosition > 0 && movie.totalDuration > 0) View.VISIBLE else View.GONE
            binding.movieProgress.progress = if (movie.totalDuration > 0) (movie.lastPosition * 100 / movie.totalDuration).toInt().coerceIn(0, 100) else 0
            itemView.setOnClickListener { onClick(movie) }
            itemView.setOnKeyListener(null)
        }
    }
    override fun onViewAttachedToWindow(holder: MovieViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (com.example.kelkin.BuildConfig.IS_TV) bindPosterFocus(holder.itemView)
    }
    override fun onViewDetachedFromWindow(holder: MovieViewHolder) {
        if (com.example.kelkin.BuildConfig.IS_TV) resetPosterFocus(holder.itemView)
        super.onViewDetachedFromWindow(holder)
    }
    override fun onViewRecycled(holder: MovieViewHolder) {
        holder.clearFocusEffect()
        super.onViewRecycled(holder)
    }
}
