package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kelkin.R
import com.example.kelkin.databinding.ItemEpisodeCardBinding

class EpisodesAdapter(
    private val episodes: List<Map.Entry<String, String>>,
    private val backdropPath: String,
    private val onEpisodeClick: (String, String) -> Unit
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val entry = episodes[position]
        holder.bind(entry.key, entry.value, backdropPath, onEpisodeClick)
    }

    override fun getItemCount(): Int = episodes.size

    class EpisodeViewHolder(private val binding: ItemEpisodeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(epKey: String, videoUrl: String, backdropPath: String, onClick: (String, String) -> Unit) {
            binding.root.isFocusable = true

            //
            binding.txtEpisodeTitle.text = "قسمت ${epKey.replace("ep", "")}"

            binding.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {

                    binding.txtEpisodeTitle.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))

                } else {

                    binding.txtEpisodeTitle.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_white))

                }
            }


            if (backdropPath.isNotEmpty()) {
                val fullImageUrl = if (backdropPath.startsWith("http")) backdropPath else "https://image.tmdb.org/t/p/w500$backdropPath"
                Glide.with(binding.imgEpisodeBackdrop.context)
                    .load(fullImageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.imgEpisodeBackdrop)
            }

            binding.root.setOnClickListener { onClick(epKey, videoUrl) }
        }
    }
}