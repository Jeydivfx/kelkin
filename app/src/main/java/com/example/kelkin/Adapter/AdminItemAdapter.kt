package com.example.kelkin.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.databinding.ItemAdminListBinding
import com.example.kelkin.utils.FocusKeyProvider
import com.example.kelkin.utils.stableContentId

class AdminItemAdapter(
    items: List<Pair<String, Map<String, Any>>>,
    private val onEdit: (String, Map<String, Any>) -> Unit,
    private val onDelete: (String) -> Unit
) : ListAdapter<Pair<String, Map<String, Any>>, AdminItemAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Pair<String, Map<String, Any>>>() {
    override fun areItemsTheSame(old: Pair<String, Map<String, Any>>, new: Pair<String, Map<String, Any>>) = old.first == new.first
    override fun areContentsTheSame(old: Pair<String, Map<String, Any>>, new: Pair<String, Map<String, Any>>) = old == new
}), FocusKeyProvider {
    init { setHasStableIds(true); submitList(items) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).first)
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.first
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.first == key }
    class ViewHolder(val binding: ItemAdminListBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemAdminListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (key, data) = getItem(position)
        holder.binding.txtTitle.text = data["name_fa"]?.toString() ?: data["name"]?.toString() ?: "نامشخص"
        holder.binding.txtMetadata.text = buildList {
            data["id"]?.let { add("شناسه: $it") }
            data["tmdb_id"]?.toString()?.takeIf { it.isNotBlank() }?.let { add("TMDB: $it") }
            data["seasons"]?.let { if (it is Map<*, *>) add("${it.size} فصل") }
        }.joinToString("   •   ")
        holder.binding.txtMetadata.visibility = if (holder.binding.txtMetadata.text.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        holder.binding.btnEdit.contentDescription = "ویرایش ${holder.binding.txtTitle.text}"
        holder.binding.btnDelete.contentDescription = "حذف ${holder.binding.txtTitle.text}"
        holder.binding.btnEdit.setOnClickListener { onEdit(key, data) }
        holder.binding.btnDelete.setOnClickListener { onDelete(key) }
    }
    fun updateData(items: List<Pair<String, Map<String, Any>>>, committed: () -> Unit = {}) = submitList(items, committed)
}
