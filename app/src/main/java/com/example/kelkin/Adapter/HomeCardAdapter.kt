package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R
import com.example.kelkin.utils.*
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits

data class HomeCard(val movie: Movie, val artwork: String?)

/** Home-specific cards leave other screens' original poster layouts unchanged. */
class HomeCardAdapter(private val landscape: Boolean, private val onClick: (Movie) -> Unit) :
    ListAdapter<HomeCard, HomeCardAdapter.Holder>(object : DiffUtil.ItemCallback<HomeCard>() {
        override fun areItemsTheSame(old: HomeCard, new: HomeCard) = old.movie.identity() == new.movie.identity()
        override fun areContentsTheSame(old: HomeCard, new: HomeCard) = old == new
    }), FocusKeyProvider {
    var onVisible: ((Movie) -> Unit)? = null
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).movie.identity())
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.movie?.identity()
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.movie.identity() == key }
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.home_card_image)
        val title: TextView = view.findViewById(R.id.home_card_title)
        val progress: ProgressBar = view.findViewById(R.id.home_card_progress)
        val remaining: TextView = view.findViewById(R.id.home_card_remaining)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context)
        .inflate(if (landscape) R.layout.item_home_continue else R.layout.item_home_poster, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val card = getItem(position)
        val movie = card.movie
        holder.title.text = movie.name_fa
        holder.itemView.contentDescription = movie.name_fa
        Glide.with(holder.image).load(card.artwork).placeholder(R.drawable.hero_test_header)
            .error(R.drawable.hero_test_header).centerCrop()
            .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(250)).into(holder.image)
        val hasProgress = landscape && movie.lastPosition > 0 && movie.totalDuration > 0 && movie.lastPosition < movie.totalDuration
        holder.progress.visibility = if (hasProgress) View.VISIBLE else View.GONE
        holder.remaining.visibility = if (hasProgress) View.VISIBLE else View.GONE
        if (hasProgress) {
            holder.progress.progress = (movie.lastPosition.toDouble() * 100 / movie.totalDuration).toInt().coerceIn(0, 100)
            val minutes = ((movie.totalDuration - movie.lastPosition).coerceAtLeast(0) + 59_999) / 60_000
            holder.remaining.text = "$minutes دقیقه مانده".toPersianDigits()
        }
        holder.itemView.setOnClickListener { onClick(movie) }
        onVisible?.invoke(movie)
    }
}
