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
import com.example.kelkin.DataClass.Channel
import com.example.kelkin.R

class ChannelAdapter(private val onChannelClick: (Channel) -> Unit) :
    ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = getItem(position)
        holder.bind(channel)
        holder.itemView.setOnClickListener { onChannelClick(channel) }

        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            view.isSelected = hasFocus
            view.isActivated = hasFocus

            if (hasFocus) {
                view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
            } else {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }
    }

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgLogo: ImageView = itemView.findViewById(R.id.channelLogo)
        private val txtName: TextView = itemView.findViewById(R.id.txtChannelName)

        fun bind(channel: Channel) {
            txtName.text = channel.name_fa

            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true

            Glide.with(itemView.context)
                .load(channel.logoUrl)
                .placeholder(R.drawable.ic_menu_tv)
                .into(imgLogo)
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}