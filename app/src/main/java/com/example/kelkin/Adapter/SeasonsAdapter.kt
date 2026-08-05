package com.example.kelkin.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.databinding.ItemSeasonCardBinding // یا لایوت دلخواه خودت

class SeasonsAdapter(
    private val seasons: List<String>,
    private var selectedSeason: String,
    private val onSeasonClick: (String) -> Unit
) : RecyclerView.Adapter<SeasonsAdapter.SeasonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val binding = ItemSeasonCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeasonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        val seasonKey = seasons[position]
        holder.bind(seasonKey, seasonKey == selectedSeason) {
            val oldPosition = seasons.indexOf(selectedSeason)
            selectedSeason = seasonKey
            notifyItemChanged(oldPosition)
            notifyItemChanged(position)
            onSeasonClick(seasonKey)
        }
    }

    override fun getItemCount(): Int = seasons.size

    class SeasonViewHolder(private val binding: ItemSeasonCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(seasonKey: String, isSelected: Boolean, onClick: () -> Unit) {
            // فرض بر اینکه در لایوت یک TextView به نام txtSeasonName داری
            binding.root.isFocusable = true
            // اینجا می‌توانی متن یا استایل دکمه فصل را بر اساس انتخاب‌شده بودن تغییر دهی
            // مثلاً: binding.txtSeasonName.text = "فصل $seasonKey"

            binding.root.setOnClickListener { onClick() }
        }
    }
}