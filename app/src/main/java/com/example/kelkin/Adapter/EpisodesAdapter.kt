package com.example.kelkin.Adapter

import com.example.kelkin.utils.FocusKeyProvider
import com.example.kelkin.utils.stableContentId

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

    class EpisodeViewHolder(private val binding: ItemEpisodeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(epKey: String, videoUrl: String, backdropPath: String, onClick: (String, String) -> Unit) {
            binding.root.isFocusable = true
            binding.root.contentDescription = "قسمت ${epKey.replace("ep", "")}"
            binding.txtEpisodeTitle.text = "قسمت ${epKey.replace("ep", "")}"
            if (backdropPath.isNotEmpty()) {
                val fullImageUrl = if (backdropPath.startsWith("http")) backdropPath else "https://image.tmdb.org/t/p/w500$backdropPath"
                Glide.with(binding.imgEpisodeBackdrop.context)
                    .load(fullImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.imgEpisodeBackdrop)
            }

            binding.root.setOnClickListener { onClick(epKey, videoUrl) }
        }
    }
}