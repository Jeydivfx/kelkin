package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.AdminItemAdapter
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentMovieListBinding
import com.example.kelkin.utils.*
import com.google.firebase.database.*
import kotlinx.coroutines.*

/** Movie and series editors share validation, authorization and lifecycle behavior. */
open class CatalogEditorFragment(private val series: Boolean) : AdminFragment(R.layout.fragment_movie_list) {
    private var binding: FragmentMovieListBinding? = null
    private val ref get() = if (series) FirebaseManager.getSeriesRef() else FirebaseManager.getMoviesRef()
    private val categories = linkedMapOf<String, Long>()
    private val languages = linkedMapOf<String, String>()
    private val qualities = linkedMapOf<String, String>()
    private var currentDialog: AlertDialog? = null
    private lateinit var adapter: AdminItemAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentMovieListBinding.bind(view).also { binding = it }
        ui.btnAddMovie.text = if (series) "افزودن سریال" else "افزودن فیلم"
        ui.txtAdminListTitle.text = if (series) "مدیریت سریال‌ها" else "مدیریت فیلم‌ها"
        ui.btnAdminBack.setOnClickListener { findNavController().popBackStack() }
        adapter = AdminItemAdapter(emptyList(), { key, data -> editItem(key, data) }, { key ->
            DialogUtils.showConfirmDialog(requireContext(), "حذف عنوان", "این عنوان از آرشیو حذف شود؟") {
                FirebaseManager.deleteItem(if (series) "series" else "movie", key) { success ->
                    if (isAdded) Toast.makeText(context, if (success) "عنوان حذف شد" else "حذف انجام نشد؛ دوباره تلاش کنید", Toast.LENGTH_SHORT).show()
                }
            }
        })
        ui.rvMoviesAdmin.layoutManager = LinearLayoutManager(requireContext())
        ui.rvMoviesAdmin.adapter = adapter
        val memory = RecyclerFocusMemory(this, ui.btnAddMovie).also { it.track(ui.rvMoviesAdmin) }
        ref.orderByChild("id").addViewValueEventListener(viewLifecycleOwner, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (binding == null) return
                @Suppress("UNCHECKED_CAST")
                val items = snapshot.children.mapNotNull { child -> child.key?.let { key -> (child.value as? Map<String, Any>)?.let { key to it } } }.reversed()
                ui.txtAdminListStatus.text = if (items.isEmpty()) "آرشیو خالی است؛ اولین عنوان را اضافه کنید." else "${items.size} عنوان • برای تغییر اطلاعات، ویرایش را انتخاب کنید."
                adapter.updateData(items) { memory.restore() }
            }
            override fun onCancelled(error: DatabaseError) {
                binding?.txtAdminListStatus?.text = "فهرست دریافت نشد؛ اتصال و دسترسی مدیر را بررسی کنید."
            }
        })
        ui.btnAddMovie.setOnClickListener { editItem(null, emptyMap()) }
        loadOptions()
    }
    private fun loadOptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val database = FirebaseManager.getDatabase()
                val cats = withTimeout(10_000) { database.getReference(if (series) "series_categories" else "category").get().awaitResult() }
                categories.clear()
                cats.children.forEach { child ->
                    val name = child.child("name").value?.toString().orEmpty()
                    val id = child.key.orEmpty().removePrefix("cat_").toLongOrNull() ?: 0L
                    if (name.isNotBlank()) categories[name] = id
                }
                for ((node, target) in listOf("language" to languages, "quality" to qualities)) {
                    val values = withTimeout(10_000) { database.getReference(node).get().awaitResult() }
                    target.clear()
                    values.children.forEach { child -> child.key?.let { key -> target[child.child("name").value?.toString().orEmpty()] = key } }
                }
            } catch (e: TimeoutCancellationException) { if (isAdded) Toast.makeText(context, "زمان دریافت گزینه‌ها تمام شد؛ دوباره تلاش کنید", Toast.LENGTH_SHORT).show() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (isAdded) Toast.makeText(context, "گزینه‌ها دریافت نشدند؛ اتصال را بررسی کنید", Toast.LENGTH_SHORT).show() }
        }
    }
    private fun select(options: List<String>, callback: (String) -> Unit) {
        if (options.isEmpty()) { Toast.makeText(context, "گزینه‌ها هنوز دریافت نشده‌اند", Toast.LENGTH_SHORT).show(); loadOptions(); return }
        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setItems(options.toTypedArray()) { _, index -> callback(options[index]) }.show()
    }
    private fun editItem(key: String?, data: Map<String, Any>) {
        val view = layoutInflater.inflate(if (series) R.layout.dialog_add_serie else R.layout.dialog_add_movie, null)
        val name = view.findViewById<EditText>(R.id.edtNameFa)
        val description = view.findViewById<EditText>(R.id.edtDescFa)
        val tmdb = view.findViewById<EditText>(R.id.edtTmdbId)
        val url = view.findViewById<EditText>(R.id.edtUrl1)
        val category = view.findViewById<TextView>(R.id.btnCategoryDropdown)
        val language = view.findViewById<TextView>(R.id.btnLanguageDropdown)
        val quality = view.findViewById<TextView>(R.id.btnQualityDropdown)
        view.findViewById<TextView>(R.id.txtGeneratedId)?.text = key ?: "شناسه هنگام ذخیره ایجاد می‌شود"
        name.setText(data["name_fa"]?.toString().orEmpty()); description.setText(data["description_fa"]?.toString().orEmpty())
        tmdb.setText(data["tmdb_id"]?.toString().orEmpty()); url?.setText(data["videoUrl1"]?.toString().orEmpty())
        if (series) url?.visibility = View.GONE
        var categoryCode = data["category"]?.toString()?.toLongOrNull() ?: 0L
        var languageCode = data["language"]?.toString().orEmpty()
        var qualityCode = data["quality"]?.toString().orEmpty()
        category.text = categories.entries.find { it.value == categoryCode }?.key ?: "انتخاب دسته‌بندی"
        language.text = languages.entries.find { it.value == languageCode }?.key ?: "انتخاب زبان"
        quality.text = qualities.entries.find { it.value == qualityCode }?.key ?: "انتخاب کیفیت"
        category.isFocusable = true; language.isFocusable = true; quality.isFocusable = true
        category.setOnClickListener { select(categories.keys.toList()) { selected -> categoryCode = categories.getValue(selected); category.text = selected } }
        language.setOnClickListener { select(languages.keys.toList()) { selected -> languageCode = languages.getValue(selected); language.text = selected } }
        quality.setOnClickListener { select(qualities.keys.toList()) { selected -> qualityCode = qualities.getValue(selected); quality.text = selected } }
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setTitle(if (key == null) "افزودن عنوان" else "ویرایش عنوان")
            .setView(view).setPositiveButton("ذخیره", null).setNegativeButton("انصراف", null)
        if (series && key != null) builder.setNeutralButton("مدیریت فصل و قسمت", null)
        val dialog = builder.create().also { currentDialog = it }
        dialog.show()
        if (series && key != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { dialog.dismiss(); manageEpisodes(key) }
        dialog.installValidatedSave(validate = {
            name.requireText("نام عنوان را وارد کنید") && tmdb.optionalTmdbId() && (series || url.requireStreamUrl()) &&
                if (categoryCode == 0L) { category.error = "دسته‌بندی را انتخاب کنید"; false } else true
        }) { done ->
            val updates = mapOf<String, Any>("name_fa" to name.text.toString().trim(), "description_fa" to description.text.toString().trim(),
                "tmdb_id" to tmdb.text.toString().trim(), "category" to categoryCode, "language" to languageCode, "quality" to qualityCode)
                .toMutableMap().apply { if (!series) put("videoUrl1", url.text.toString().trim()) }
            val onResult: (String?) -> Unit = { savedKey ->
                done(savedKey != null)
                if (isAdded) {
                    Toast.makeText(context, if (savedKey != null) "عنوان ذخیره شد" else "ذخیره انجام نشد؛ دوباره تلاش کنید", Toast.LENGTH_SHORT).show()
                    if (series && key == null && savedKey != null && binding != null) manageEpisodes(savedKey)
                }
            }
            if (key == null) FirebaseManager.createItem(if (series) "series" else "movie", updates, onResult)
            else FirebaseManager.saveOrUpdate(if (series) "series" else "movie", key, updates) { onResult(if (it) key else null) }
        }
        name.requestFocus()
    }
    private fun manageEpisodes(key: String) {
        val context = requireContext()
        val panel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 16, 24, 16) }
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(context).apply { addView(list) }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, (220 * resources.displayMetrics.density).toInt()))
        fun field(hint: String, number: Boolean) = EditText(context).apply {
            this.hint = hint; setTextColor(android.graphics.Color.WHITE)
            inputType = if (number) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            panel.addView(this)
        }
        val season = field("شماره فصل", true); val episode = field("شماره قسمت", true); val url = field("لینک قسمت", false)
        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog).setTitle("فصل‌ها و قسمت‌ها")
            .setView(panel).setPositiveButton("ذخیره قسمت", null).setNegativeButton("بستن", null).create()
        currentDialog = dialog
        val seasonRef = ref.child(key).child("seasons")
        var loadJob: Job? = null
        fun reload() {
            loadJob?.cancel()
            loadJob = viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val snapshot = withTimeout(10_000) { seasonRef.get().awaitResult() }
                    if (!dialog.isShowing) return@launch
                    list.removeAllViews()
                    snapshot.children.sortedBy { it.key?.toIntOrNull() }.forEach { s ->
                        s.children.sortedBy { it.key?.removePrefix("ep")?.toIntOrNull() }.forEach { ep ->
                            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                            row.addView(TextView(context).apply { text = "فصل ${s.key?.toIntOrNull()}، قسمت ${ep.key?.removePrefix("ep")?.toIntOrNull()}"; textSize = 16f; setTextColor(android.graphics.Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
                            row.addView(Button(context).apply {
                                text = "حذف"; isFocusable = true
                                backgroundTintList = null
                                setBackgroundResource(R.drawable.sl_button_high_contrast)
                                backgroundTintList = null
                                backgroundTintList = null
                                setBackgroundResource(R.drawable.bg_dialog_button)
                                setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
                                setOnClickListener {
                                    DialogUtils.showConfirmDialog(context, "حذف قسمت", "این قسمت حذف شود؟") {
                                        ep.ref.removeValue().addOnCompleteListener { task ->
                                            if (task.isSuccessful && dialog.isShowing) reload()
                                            else if (dialog.isShowing) Toast.makeText(context, "حذف انجام نشد", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            })
                            list.addView(row)
                        }
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { if (dialog.isShowing) Toast.makeText(context, "قسمت‌ها دریافت نشدند", Toast.LENGTH_SHORT).show() }
            }
        }
        dialog.setOnDismissListener { loadJob?.cancel() }
        dialog.show(); reload()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val s = season.text.toString().toIntOrNull()
            val ep = episode.text.toString().toIntOrNull()
            if (s == null || s !in 1..999) { season.error = "شماره فصل معتبر وارد کنید"; return@setOnClickListener }
            if (ep == null || ep !in 1..9999) { episode.error = "شماره قسمت معتبر وارد کنید"; return@setOnClickListener }
            if (!url.requireStreamUrl()) return@setOnClickListener
            val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            save.isEnabled = false
            seasonRef.child("%02d".format(java.util.Locale.US, s)).child("ep%02d".format(java.util.Locale.US, ep)).setValue(url.text.toString().trim()).addOnCompleteListener { task ->
                if (dialog.isShowing) {
                    save.isEnabled = true
                    if (task.isSuccessful) { episode.text.clear(); url.text.clear(); reload(); episode.requestFocus() }
                    else Toast.makeText(context, "ذخیره قسمت انجام نشد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        season.requestFocus()
    }
    override fun onDestroyView() { currentDialog?.dismiss(); currentDialog = null; binding = null; super.onDestroyView() }
}
