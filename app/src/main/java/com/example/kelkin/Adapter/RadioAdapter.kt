package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kelkin.DataClass.Radio
import com.example.kelkin.R

class RadioAdapter(private val onRadioClick: (Radio) -> Unit) :
    ListAdapter<Radio, RadioAdapter.RadioViewHolder>(RadioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RadioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return RadioViewHolder(view)
    }

    override fun onBindViewHolder(holder: RadioViewHolder, position: Int) {
        val radio = getItem(position)
        holder.bind(radio)

        holder.itemView.setOnClickListener { onRadioClick(radio) }

        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                view.z = 10f
            } else {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                view.z = 0f
            }
        }

        val totalItems = itemCount
        val spanCount = 5 // هماهنگ با GridLayoutManager

        holder.itemView.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        // اجازه خروج از لیست به سمت بالا (دکمه‌های فیلتر)
                        position < spanCount
                        false
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // مسدود کردن خروج از پایین لیست
                        position + spanCount >= totalItems
                    }
                    else -> false
                }
            } else false
        }
    }

    class RadioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgLogo: ImageView = itemView.findViewById(R.id.channelLogo)
        private val txtName: TextView = itemView.findViewById(R.id.txtChannelName)

        fun bind(radio: Radio) {
            txtName.text = radio.name_fa
            Glide.with(itemView.context)
                .load(radio.logoUrl)
                .placeholder(R.drawable.ic_menu_tv) // آیکون پیش‌فرض رادیو اگه داری جایگزین کن
                .into(imgLogo)
        }
    }

    class RadioDiffCallback : DiffUtil.ItemCallback<Radio>() {
        override fun areItemsTheSame(oldItem: Radio, newItem: Radio) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Radio, newItem: Radio) = oldItem == newItem
    }
}