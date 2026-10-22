package com.example.kelkin.Adapter

import com.example.kelkin.utils.*

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
    ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()), FocusKeyProvider {


    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).id.toString())
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.id.toString()
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.id.toString() == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        val manager = (parent as? RecyclerView)?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        if (manager?.orientation == RecyclerView.HORIZONTAL) {
            view.layoutParams = view.layoutParams.apply { width = (140 * parent.resources.displayMetrics.density).toInt() }
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                marginStart = (8 * parent.resources.displayMetrics.density).toInt()
                marginEnd = (8 * parent.resources.displayMetrics.density).toInt()
            }
        }
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = getItem(position)
        holder.bind(channel)
        holder.itemView.setOnClickListener { onChannelClick(channel) }
        holder.itemView.setOnLongClickListener {
            val prefs = it.context.getSharedPreferences("kelkin_prefs", 0)
            val favorites = prefs.getStringSet("favorite_channels", emptySet()).orEmpty().toMutableSet()
            val added = favorites.add(channel.id.toString())
            if (!added) favorites.remove(channel.id.toString())
            prefs.edit().putStringSet("favorite_channels", favorites).apply()
            android.widget.Toast.makeText(it.context, if (added) "کانال نشان شد" else "کانال از نشان‌شده‌ها حذف شد", android.widget.Toast.LENGTH_SHORT).show()
            true
        }

        if (!com.example.kelkin.BuildConfig.IS_TV) {
            holder.itemView.setOnFocusChangeListener(null)
            holder.itemView.animate().cancel()
            holder.itemView.scaleX = 1f; holder.itemView.scaleY = 1f
            holder.itemView.isSelected = false; holder.itemView.isActivated = false
            return
        }
        bindPosterFocus(holder.itemView)
    }

    override fun onViewAttachedToWindow(holder: ChannelViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (com.example.kelkin.BuildConfig.IS_TV) bindPosterFocus(holder.itemView)
    }
    override fun onViewDetachedFromWindow(holder: ChannelViewHolder) {
        if (com.example.kelkin.BuildConfig.IS_TV) resetPosterFocus(holder.itemView)
        super.onViewDetachedFromWindow(holder)
    }
    override fun onViewRecycled(holder: ChannelViewHolder) {
        if (com.example.kelkin.BuildConfig.IS_TV) resetPosterFocus(holder.itemView)
        super.onViewRecycled(holder)
    }
    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgLogo: ImageView = itemView.findViewById(R.id.channelLogo)
        private val txtName: TextView = itemView.findViewById(R.id.txtChannelName)

        fun bind(channel: Channel) {
            txtName.text = channel.name_fa
            itemView.contentDescription = channel.name_fa

            itemView.isFocusable = com.example.kelkin.BuildConfig.IS_TV
            itemView.isFocusableInTouchMode = com.example.kelkin.BuildConfig.IS_TV

            Glide.with(itemView.context)
                .load(channel.logoUrl)
                .placeholder(R.drawable.ic_menu_tv)
                .error(R.drawable.ic_menu_tv)
                .fitCenter()
                .into(imgLogo)
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}
