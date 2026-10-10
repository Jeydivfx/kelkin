package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.AdminItemAdapter
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentChannelListBinding
import com.example.kelkin.utils.*
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*

class ChannelListFragment : AdminFragment(R.layout.fragment_channel_list) {
    private var binding: FragmentChannelListBinding? = null
    private var editor: AlertDialog? = null
    private val categories = linkedMapOf<String, Long>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentChannelListBinding.bind(view).also { binding = it }
        ui.btnAddChannel.text = "افزودن کانال"
        ui.btnAdminBack.setOnClickListener { findNavController().popBackStack() }
        val adapter = AdminItemAdapter(emptyList(), { key, data -> editChannel(key, data) }, { key ->
            DialogUtils.showConfirmDialog(requireContext(), "حذف کانال", "این کانال حذف شود؟") {
                FirebaseManager.deleteItem("channels", key) { success ->
                    if (isAdded) Toast.makeText(context, if (success) "کانال حذف شد" else "حذف انجام نشد", Toast.LENGTH_SHORT).show()
                }
            }
        })
        ui.rvChannels.layoutManager = LinearLayoutManager(requireContext()); ui.rvChannels.adapter = adapter
        val memory = RecyclerFocusMemory(this, ui.btnAddChannel).also { it.track(ui.rvChannels) }
        FirebaseManager.getChannelsRef().orderByChild("id").addViewValueEventListener(viewLifecycleOwner, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (binding == null) return
                @Suppress("UNCHECKED_CAST")
                val items = snapshot.children.mapNotNull { child -> child.key?.let { key -> (child.value as? Map<String, Any>)?.let { key to it } } }.reversed()
                ui.txtAdminListStatus.text = if (items.isEmpty()) "کانالی ثبت نشده؛ اولین کانال را اضافه کنید." else "${items.size} کانال • اطلاعات یا منابع پخش را ویرایش کنید."
                adapter.updateData(items) { memory.restore() }
            }
            override fun onCancelled(error: DatabaseError) {
                binding?.txtAdminListStatus?.text = "فهرست دریافت نشد؛ اتصال و دسترسی مدیر را بررسی کنید."
            }
        })
        ui.btnAddChannel.setOnClickListener { editChannel(null, emptyMap()) }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val data = withTimeout(10_000) { FirebaseManager.getTvCategoriesRef().get().awaitResult() }
                data.children.forEach { categories[it.child("name").value?.toString().orEmpty()] = it.key.orEmpty().removePrefix("cat_").toLongOrNull() ?: 0L }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (isAdded) Toast.makeText(context, "دسته‌بندی‌ها دریافت نشدند", Toast.LENGTH_SHORT).show() }
        }
    }
    private fun parseSources(value: String): List<Map<String, String>> {
        if (value.isBlank()) return emptyList()
        val json = JsonParser.parseString(value)
        val entries = when {
            json.isJsonArray -> json.asJsonArray.toList()
            json.isJsonObject -> json.asJsonObject.entrySet().map { it.value }
            else -> error("Invalid sources")
        }
        return entries.map {
            val entry = it.asJsonObject
            val name = entry.get("name")?.asString.orEmpty().trim()
            val url = entry.get("url")?.asString.orEmpty().trim()
            require(name.isNotBlank() && StreamUrls.isValid(url))
            mapOf("name" to name, "url" to url, "geography" to entry.get("geography")?.asString.orEmpty())
        }
    }
    private fun editChannel(key: String?, data: Map<String, Any>) {
        val view = layoutInflater.inflate(R.layout.dialog_add_channel, null)
        val name = view.findViewById<EditText>(R.id.edtChannelName)
        val url = view.findViewById<EditText>(R.id.edtStreamUrl)
        val logo = view.findViewById<EditText>(R.id.edtLogoUrl)
        val sources = view.findViewById<EditText>(R.id.edtSourceJson)
        val category = view.findViewById<TextView>(R.id.btnCategoryDropdown)
        name.setText(data["name_fa"]?.toString().orEmpty()); url.setText(data["videoUrl"]?.toString().orEmpty()); logo.setText(data["logoUrl"]?.toString().orEmpty())
        val source = data["sources"] ?: data["source"]
        sources.setText(if (source is String) source else if (source != null) Gson().toJson(source) else "")
        var categoryCode = data["category"]?.toString()?.toLongOrNull() ?: 0L
        category.text = categories.entries.find { it.value == categoryCode }?.key ?: "انتخاب دسته‌بندی"
        category.isFocusable = true
        category.setOnClickListener {
            if (categories.isEmpty()) { Toast.makeText(context, "دسته‌بندی‌ها هنوز دریافت نشده‌اند", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val labels = categories.keys.toList()
            AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setItems(labels.toTypedArray()) { _, index -> categoryCode = categories.getValue(labels[index]); category.text = labels[index] }.show()
        }
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setTitle(if (key == null) "افزودن کانال" else "ویرایش کانال")
            .setView(view).setPositiveButton("ذخیره", null).setNegativeButton("انصراف", null).create()
        editor = dialog; dialog.show()
        var parsed = emptyList<Map<String, String>>()
        dialog.installValidatedSave(validate = {
            var valid = name.requireText("نام کانال را وارد کنید") && url.requireStreamUrl()
            if (logo.text.isNotBlank() && !logo.requireStreamUrl()) valid = false
            if (categoryCode == 0L) { category.error = "دسته‌بندی را انتخاب کنید"; valid = false }
            try { parsed = parseSources(sources.text.toString()) }
            catch (e: Exception) { sources.error = "JSON باید فهرست سورس‌ها با name و url معتبر باشد"; valid = false }
            valid
        }) { done ->
            val updates = mapOf<String, Any?>("name_fa" to name.text.toString().trim(), "videoUrl" to url.text.toString().trim(), "logoUrl" to logo.text.toString().trim(), "category" to categoryCode, "sources" to parsed, "source" to null)
            val result: (String?) -> Unit = { saved ->
                done(saved != null)
                if (isAdded) Toast.makeText(context, if (saved != null) "کانال ذخیره شد" else "ذخیره انجام نشد؛ دوباره تلاش کنید", Toast.LENGTH_SHORT).show()
            }
            if (key == null) FirebaseManager.createItem("channels", updates, result)
            else FirebaseManager.saveOrUpdate("channels", key, updates) { result(if (it) key else null) }
        }
        name.requestFocus()
    }
    override fun onDestroyView() { editor?.dismiss(); editor = null; binding = null; super.onDestroyView() }
}
