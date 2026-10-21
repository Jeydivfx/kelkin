package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.kelkin.DataClass.CastMember
import com.example.kelkin.R

class CastAdapter(private val cast: List<CastMember>, private val portraitDp: Int = 80) : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    private val shelfLayoutListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        val list = view as RecyclerView
        for (index in 0 until list.childCount) sizeTvCard(list.getChildAt(index), list)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (com.example.kelkin.BuildConfig.IS_TV) recyclerView.addOnLayoutChangeListener(shelfLayoutListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnLayoutChangeListener(shelfLayoutListener)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: CastViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.parent as? RecyclerView)?.let { sizeTvCard(holder.itemView, it) }
    }

    private fun sizeTvCard(item: View, list: RecyclerView) {
        if (!com.example.kelkin.BuildConfig.IS_TV) return
        val available = list.width - list.paddingLeft - list.paddingRight
        if (available <= 0) return
        val slot = available / 7
        val density = list.resources.displayMetrics.density
        val padding = (4 * density).toInt()
        val params = item.layoutParams as RecyclerView.LayoutParams
        if (params.width != slot || params.marginEnd != 0 || params.marginStart != 0) {
            params.width = slot
            params.marginStart = 0
            params.marginEnd = 0
            item.layoutParams = params
        }
        val verticalPadding = (if (portraitDp < 80) 4 * density else 8 * density).toInt()
        item.setPadding(padding, verticalPadding, padding, verticalPadding)
        val portrait = item.findViewById<ImageView>(R.id.imgCast).parent as androidx.cardview.widget.CardView
        val diameter = minOf((portraitDp * density).toInt(), (slot - 2 * padding).coerceAtLeast(1))
        if (portrait.layoutParams.width != diameter || portrait.layoutParams.height != diameter) {
            portrait.layoutParams = portrait.layoutParams.apply { width = diameter; height = diameter }
            portrait.radius = diameter / 2f
        }
        val name = item.findViewById<TextView>(R.id.txtCastName)
        if (name.layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            name.layoutParams = name.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
        }
    }

    class CastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgCast: ImageView = view.findViewById(R.id.imgCast)
        val txtName: TextView = view.findViewById(R.id.txtCastName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cast, parent, false)
        return CastViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val member = cast[position]
        holder.itemView.isFocusable = false
        holder.itemView.isClickable = false
        holder.txtName.text = member.name

        val profilePath = member.profile_path

        if (!profilePath.isNullOrEmpty()) {

            val imageUrl = "https://image.tmdb.org/t/p/w200$profilePath"
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.imgCast)
        } else {

            Glide.with(holder.itemView.context)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(holder.imgCast)
        }

        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .start()
            } else {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
        }

    }

    override fun getItemCount(): Int = cast.size
}
