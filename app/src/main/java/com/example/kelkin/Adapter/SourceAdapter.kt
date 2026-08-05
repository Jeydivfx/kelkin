package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.DataClass.ChannelSource
import com.example.kelkin.R

class SourceAdapter(
    private var sources: List<ChannelSource>,
    private val onSourceSelected: (ChannelSource) -> Unit
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {

    class SourceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtSourceName)
        val txtGeo: TextView = view.findViewById(R.id.txtSourceGeo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        // اینجا باید یک فایل XML به اسم item_source طراحی کنی
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_source, parent, false)
        return SourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val source = sources[position]
        holder.txtName.text = source.name
        holder.txtGeo.text = source.geography

        holder.itemView.setOnClickListener {
            onSourceSelected(source)
        }
    }

    override fun getItemCount() = sources.size

    fun updateData(newSources: List<ChannelSource>) {
        this.sources = newSources
        notifyDataSetChanged()
    }
}