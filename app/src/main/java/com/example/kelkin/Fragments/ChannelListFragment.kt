package com.example.kelkin.Fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.AdminItemAdapter
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentChannelListBinding
import com.example.kelkin.utils.DialogUtils
import com.example.kelkin.utils.FirebaseManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import androidx.activity.OnBackPressedCallback

class ChannelListFragment : Fragment(R.layout.fragment_channel_list) {

    private var _binding: FragmentChannelListBinding? = null
    private val binding get() = _binding!!

    private val categoryNames = mutableListOf<String>()
    private val categoryMap = mutableMapOf<String, Long>()
    val edtSource = view?.findViewById<EditText>(R.id.edtSourceJson)
    private lateinit var adapter: AdminItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChannelListBinding.bind(view)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_channelListFragment_to_adminDashboardFragment)
            }
        })

        fetchTvCategories()
        setupRecyclerView()

        binding.btnAddChannel.setOnClickListener { showChannelDialog(null, null) }
        loadChannels()
    }

    private fun setupRecyclerView() {
        adapter = AdminItemAdapter(emptyList(),
            onEdit = { id, data -> showChannelDialog(id, data) },
            onDelete = { id -> deleteChannel(id) }
        )
        binding.rvChannels.layoutManager = LinearLayoutManager(context)
        binding.rvChannels.adapter = adapter
    }

    private fun fetchTvCategories() {
        FirebaseManager.getDatabase().getReference("tv_categories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryNames.clear()
                    categoryMap.clear()
                    for (cat in snapshot.children) {
                        val name = cat.child("name").getValue(String::class.java) ?: ""

                        val key = cat.key ?: "cat_0"
                        val id = key.replace("cat_", "").toLongOrNull() ?: 0L

                        if (name.isNotEmpty()) {
                            categoryNames.add(name)
                            categoryMap[name] = id
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    private fun loadChannels() {
        FirebaseManager.getDatabase().getReference("channels").orderByChild("id").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val key = child.key
                    val value = child.value as? Map<String, Any>
                    if (key != null && value != null) key to value else null
                }
                adapter.updateData(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showChannelDialog(id: String?, data: Map<String, Any>?) {

        val view = layoutInflater.inflate(R.layout.dialog_add_channel, null)

        val edtName = view.findViewById<EditText>(R.id.edtChannelName)
        val edtUrl = view.findViewById<EditText>(R.id.edtStreamUrl)
        val edtLogo = view.findViewById<EditText>(R.id.edtLogoUrl)
        val edtSource = view.findViewById<EditText>(R.id.edtSourceJson)
        val btnCat = view.findViewById<TextView>(R.id.btnCategoryDropdown)

        var selectedCat = (data?.get("category") as? Long) ?: 0L

        if (data != null) {
            edtName.setText(data["name_fa"] as? String ?: "")
            edtUrl.setText(data["videoUrl"] as? String ?: "")
            edtLogo.setText(data["logoUrl"] as? String ?: "")
            edtSource.setText(data["source"] as? String ?: "")

            val rawSource = data["source"] ?: data["sources"] ?: ""


            edtSource.setText(rawSource.toString())
            val catName = categoryMap.entries.find { it.value == selectedCat }?.key
            btnCat.text = catName ?: "دسته‌بندی: $selectedCat"
        }

        btnCat.setOnClickListener {
            showSelectionMenu(categoryNames, it) { name ->
                btnCat.text = "دسته‌بندی: $name"
                selectedCat = categoryMap[name] ?: 0L
            }
        }

        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(view)
            .setTitle(if (id == null) "افزودن کانال" else "ویرایش کانال")
            .setPositiveButton(if (id == null) "افزودن" else "ذخیره") { _, _ ->
                if (id == null) {
                    FirebaseManager.getDatabase().getReference("channels")
                        .orderByChild("id").limitToLast(1)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var newNumericId = 101L
                                if (snapshot.exists()) {
                                    for (child in snapshot.children) {
                                        newNumericId = (child.child("id").getValue(Long::class.java) ?: 100L) + 1
                                    }
                                }
                                saveChannelToFirebase("c$newNumericId", newNumericId, edtName, edtUrl, edtSource, edtLogo, selectedCat)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                } else {
                    val existingId = (data?.get("id") as? Long) ?: 0L
                    saveChannelToFirebase(id, existingId, edtName, edtUrl, edtSource, edtLogo, selectedCat)
                }
            }
            .setNegativeButton("لغو", null)
            .create().apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    setTextColor(requireContext().getColor(R.color.yellow))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                    setTextColor(requireContext().getColor(R.color.yellow))
                }
            }
    }
    private fun saveChannelToFirebase(key: String, numericId: Long, name: EditText, url: EditText,source: EditText, logo: EditText, cat: Long) {
        val map = mapOf(
            "id" to numericId,
            "name_fa" to name.text.toString(),
            "videoUrl" to url.text.toString(),
            "source" to source.text.toString(),
            "logoUrl" to logo.text.toString(),
            "category" to cat
        )
        FirebaseManager.saveOrUpdate("channels", key, map) { success ->
            if (success) Toast.makeText(context, "با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
            else Toast.makeText(context, "خطا در ذخیره در فایربیس", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteChannel(id: String) {
        DialogUtils.showConfirmDialog(requireContext(), "حذف کانال", "آیا مطمئن هستید که می‌خواهید این کانال حذف شود؟") {
            FirebaseManager.deleteItem("channels", id) { success ->
                if (success) Toast.makeText(context, "کانال حذف شد", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showSelectionMenu(options: List<String>, anchor: View, onSelected: (String) -> Unit) {
        // Use your project's ContextThemeWrapper or directly instantiate the Popup
        val popup = ListPopupWindow(requireContext())
        popup.anchorView = anchor

        // Use standard Android list item layout
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        popup.setAdapter(adapter)

        popup.setOnItemClickListener { _, _, position, _ ->
            onSelected(options[position])
            popup.dismiss()
        }
        popup.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}