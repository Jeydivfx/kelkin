package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.databinding.ItemAdminListBinding

class AdminItemAdapter(
    private var items: List<Pair<String, Map<String, Any>>>,
    private val onEdit: (String, Map<String, Any>) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<AdminItemAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAdminListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAdminListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (id, data) = items[position]
        holder.binding.txtTitle.text = data["name_fa"] as? String ?: data["name"] as? String ?: "نامشخص"

        holder.binding.btnEdit.setOnClickListener { onEdit(id, data) }
        holder.binding.btnDelete.setOnClickListener { onDelete(id) }
    }

    override fun getItemCount() = items.size
    fun updateData(newItems: List<Pair<String, Map<String, Any>>>) {
        items = newItems
        notifyDataSetChanged()
    }
}