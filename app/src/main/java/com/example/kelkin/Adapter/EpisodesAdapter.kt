package com.example.kelkin.Adapter

import com.example.kelkin.utils.FocusKeyProvider
import com.example.kelkin.utils.stableContentId
import com.example.kelkin.utils.bindPosterFocus
import com.example.kelkin.utils.resetPosterFocus
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kelkin.R
import com.example.kelkin.databinding.ItemEpisodeCardBinding

class EpisodesAdapter(
    private val episodes: List<Map.Entry<String, String>>,
    private var backdropPath: String,
    private val onEpisodeClick: (String, String) -> Unit
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>(), FocusKeyProvider {
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(episodes[position].key)
    override fun focusKey(position: Int) = episodes.getOrNull(position)?.key
    override fun positionOfKey(key: String) = episodes.indexOfFirst { it.key == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val entry = episodes[position]
        holder.bind(entry.key, entry.value, backdropPath, onEpisodeClick)
    }

    fun updateBackdrop(path: String) { backdropPath = path; notifyItemRangeChanged(0, itemCount, "backdrop") }
    override fun getItemCount(): Int = episodes.size

    override fun onViewAttachedToWindow(holder: EpisodeViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (com.example.kelkin.BuildConfig.IS_TV) bindPosterFocus(holder.itemView)
    }
    override fun onViewDetachedFromWindow(holder: EpisodeViewHolder) {
        if (com.example.kelkin.BuildConfig.IS_TV) resetPosterFocus(holder.itemView)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: EpisodeViewHolder) {
        holder.resetImageFocus()
        super.onViewRecycled(holder)
    }

    class EpisodeViewHolder(private val binding: ItemEpisodeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun resetImageFocus() {
            if (com.example.kelkin.BuildConfig.IS_TV) resetPosterFocus(binding.root)
            binding.imgEpisodeBackdrop.animate().cancel()
            binding.imgEpisodeBackdrop.scaleX = 1f
            binding.imgEpisodeBackdrop.scaleY = 1f
            binding.imgEpisodeBackdrop.clearColorFilter()
            binding.root.onFocusChangeListener = null
        }

        fun bind(epKey: String, videoUrl: String, backdropPath: String, onClick: (String, String) -> Unit) {
            resetImageFocus()
            binding.root.isFocusable = true
            if (com.example.kelkin.BuildConfig.IS_TV) bindPosterFocus(binding.root)
            val title = "قسمت ${epKey.removePrefix("ep")}".toPersianDigits()
            binding.root.contentDescription = "$title؛ پخش"
            binding.txtEpisodeTitle.text = title
            if (backdropPath.isNotEmpty()) {
                val fullImageUrl = if (backdropPath.startsWith("http")) backdropPath else "https://image.tmdb.org/t/p/w500$backdropPath"
                Glide.with(binding.imgEpisodeBackdrop.context)
                    .load(fullImageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.mobile_poster_placeholder)
                    .error(R.drawable.mobile_poster_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.imgEpisodeBackdrop)
            } else {
                Glide.with(binding.imgEpisodeBackdrop).clear(binding.imgEpisodeBackdrop)
                binding.imgEpisodeBackdrop.setImageResource(R.drawable.mobile_poster_placeholder)
            }

            binding.root.setOnClickListener { onClick(epKey, videoUrl) }
        }
    }
}
