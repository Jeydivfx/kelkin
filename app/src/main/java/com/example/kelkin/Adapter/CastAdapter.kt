package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kelkin.DataClass.CastMember
import com.example.kelkin.R

class CastAdapter(private val cast: List<CastMember>) : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

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
        holder.txtName.text = member.name

        val profilePath = member.profile_path

        if (!profilePath.isNullOrEmpty()) {

            val imageUrl = "https://image.tmdb.org/t/p/w200$profilePath"
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .circleCrop()
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
                // ۱.۱۰ یعنی ۱۰ درصد بزرگتر، کاملاً سافت و شیک
                view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .start()
            } else {
                // برگشت به حالت عادی
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