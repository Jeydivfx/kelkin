package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.AdminItemAdapter
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentMovieListBinding
import com.example.kelkin.utils.DialogUtils
import com.example.kelkin.utils.FirebaseManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class SeriesListFragment : Fragment(R.layout.fragment_movie_list) {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!

    private val categoryNames = mutableListOf<String>()
    private val categoryMap = mutableMapOf<String, Long>()
    private lateinit var adapter: AdminItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMovieListBinding.bind(view)

        binding.btnAddMovie.text = "افزودن سریال جدید"

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_adminDashboard_to_seriesListFragment)
            }
        })

        fetchSeriesCategoriesForDropdown()

        binding.btnAddMovie.setOnClickListener {
            showAddSeriesDialog()
        }

        setupRecyclerView()
        loadSeriesFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = AdminItemAdapter(emptyList(),
            onEdit = { id, data -> showEditSeriesDialog(id, data) },
            onDelete = { id -> deleteSeries(id) }
        )
        binding.rvMoviesAdmin.layoutManager = LinearLayoutManager(context)
        binding.rvMoviesAdmin.adapter = adapter
    }

    private fun loadSeriesFromFirebase() {
        FirebaseManager.getSeriesRef().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val key = child.key
                    val value = child.value as? Map<String, Any>
                    if (key != null && value != null) key to value else null
                }
                adapter.updateData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "خطا در دریافت لیست سریال‌ها", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteSeries(id: String) {
        DialogUtils.showConfirmDialog(requireContext(), "حذف سریال", "آیا مطمئن هستید که می‌خواهید این سریال حذف شود؟") {
            FirebaseManager.deleteItem("series", id) { success ->
                if (success) Toast.makeText(context, "سریال حذف شد", Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "خطا در حذف سریال", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditSeriesDialog(id: String, data: Map<String, Any>) {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        val view = layoutInflater.inflate(R.layout.dialog_add_serie, null)

        val txtGeneratedId = view.findViewById<TextView>(R.id.txtGeneratedId)
        val edtNameFa = view.findViewById<EditText>(R.id.edtNameFa)
        val edtDescFa = view.findViewById<EditText>(R.id.edtDescFa)
        val edtTmdbId = view.findViewById<EditText>(R.id.edtTmdbId)
        val edtUrl1 = view.findViewById<EditText>(R.id.edtUrl1)
        val btnCategoryDropdown = view.findViewById<TextView>(R.id.btnCategoryDropdown)

        txtGeneratedId?.text = "شناسه سریال: $id"
        edtNameFa.setText(data["name_fa"] as? String ?: "")
        edtDescFa.setText(data["description_fa"] as? String ?: "")
        edtTmdbId.setText(data["tmdb_id"] as? String ?: "")
        edtUrl1?.visibility = View.GONE

        var selectedCategoryCode = (data["category"] as? Long) ?: 0L
        val catName = categoryMap.entries.find { it.value == selectedCategoryCode }?.key
        btnCategoryDropdown.text = catName ?: "دسته‌بندی: $selectedCategoryCode"

        btnCategoryDropdown.setOnClickListener {
            showSelectionMenu(categoryNames, it) { selectedName ->
                btnCategoryDropdown.text = "دسته‌بندی: $selectedName"
                selectedCategoryCode = categoryMap[selectedName] ?: 0L
            }
        }

        builder.setView(view)
        builder.setTitle("ویرایش اطلاعات سریال")

        builder.setPositiveButton("ذخیره تغییرات") { _, _ ->
            val updatedMap = mutableMapOf<String, Any>(
                "id" to (data["id"] as? Long ?: 0L),
                "name_fa" to edtNameFa.text.toString(),
                "description_fa" to edtDescFa.text.toString(),
                "tmdb_id" to edtTmdbId.text.toString(),
                "category" to selectedCategoryCode
            )

            if (data.containsKey("seasons")) {
                data["seasons"]?.let { updatedMap["seasons"] = it }
            } else {
                updatedMap["seasons"] = emptyMap<String, Any>()
            }

            FirebaseManager.getSeriesRef().child(id).setValue(updatedMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "بروزرسانی شد", Toast.LENGTH_SHORT).show()
                        showManageSeasonsDialog(id)
                    } else {
                        Toast.makeText(context, "خطا در بروزرسانی", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        builder.setNeutralButton("مدیریت فصل‌ها و اپیزودها") { _, _ ->
            showManageSeasonsDialog(id)
        }

        builder.setNegativeButton("انصراف", null)

        val dialog = builder.create()
        dialog.show()

        // استایل‌دهی به دکمه‌های دیالوگ ویرایش مطابق با تم پرکنتراست
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(requireContext().getColor(R.color.yellow))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(requireContext().getColor(R.color.yellow))
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(requireContext().getColor(R.color.yellow))
        }
    }

    private fun showManageSeasonsDialog(seriesFirebaseKey: String) {
        val context = requireContext()

        // اعمال بکگراند گرادیان سفارشی هم‌رنج با تم صفحه روی لایه‌ی اصلی دیالوگ
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 20)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF0A0A0F.toInt(), 0xCC0F1A2A.toInt(), 0xFF050510.toInt())
            )
        }

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
        }

        val containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(containerLayout)
        mainLayout.addView(scrollView)

        val txtStatus = TextView(context).apply {
            text = "در حال بارگذاری فصل‌ها..."
            setTextColor(0xFFD700.toInt())
            textSize = 13f
            setPadding(0, 10, 0, 10)
        }
        mainLayout.addView(txtStatus)

        val edtSeason = EditText(context).apply {
            hint = "شماره فصل (مثلا 1)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF777777.toInt())
        }
        mainLayout.addView(edtSeason)

        val edtEpisode = EditText(context).apply {
            hint = "شماره قسمت (مثلا 1)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF777777.toInt())
        }
        mainLayout.addView(edtEpisode)

        val edtUrl = EditText(context).apply {
            hint = "لینک مستقیم ویدیوی اپیزود"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF777777.toInt())
        }
        mainLayout.addView(edtUrl)

        fun loadAndRenderSeasons() {
            FirebaseManager.getSeriesRef().child(seriesFirebaseKey).child("seasons")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @Suppress("UNCHECKED_CAST")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        containerLayout.removeAllViews()
                        val seasonsMap = snapshot.value as? Map<String, Map<String, String>> ?: emptyMap()

                        if (seasonsMap.isEmpty()) {
                            txtStatus.text = "هنوز فصلی ثبت نشده است."
                            return
                        }

                        txtStatus.text = "فصل‌های ثبت‌شده: ${seasonsMap.size} فصل"

                        for ((seasonKey, episodesMap) in seasonsMap) {
                            val seasonCleanNum = seasonKey.toIntOrNull() ?: seasonKey
                            val seasonTitle = TextView(context).apply {
                                text = "📂 فصل $seasonCleanNum (${episodesMap.size} قسمت)"
                                setTextColor(0xFFFFD700.toInt())
                                textSize = 15f
                                setPadding(0, 12, 0, 4)
                                setTypeface(null, android.graphics.Typeface.BOLD)
                            }
                            containerLayout.addView(seasonTitle)

                            for ((epKey, epUrl) in episodesMap) {
                                val epLayout = LinearLayout(context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    setPadding(20, 4, 0, 4)
                                }

                                val epText = TextView(context).apply {
                                    text = "   • $epKey: $epUrl"
                                    setTextColor(0xFFDDDDDD.toInt())
                                    textSize = 13f
                                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                }

                                val btnDeleteEp = TextView(context).apply {
                                    text = "🗑️ حذف"
                                    setTextColor(0xFFFF5252.toInt())
                                    textSize = 12f
                                    setPadding(10, 0, 10, 0)
                                    setOnClickListener {
                                        FirebaseManager.getSeriesRef()
                                            .child(seriesFirebaseKey)
                                            .child("seasons")
                                            .child(seasonKey)
                                            .child(epKey)
                                            .removeValue()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "اپیزود حذف شد", Toast.LENGTH_SHORT).show()
                                                loadAndRenderSeasons()
                                            }
                                    }
                                }

                                epLayout.addView(epText)
                                epLayout.addView(btnDeleteEp)
                                containerLayout.addView(epLayout)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        loadAndRenderSeasons()

        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle("مدیریت فصل‌ها و اپیزودها")
            .setView(mainLayout)
            .setPositiveButton("ذخیره") { _, _ ->
                val sNum = edtSeason.text.toString().trim()
                val eNum = edtEpisode.text.toString().trim()
                val url = edtUrl.text.toString().trim()

                if (sNum.isNotEmpty() && eNum.isNotEmpty() && url.isNotEmpty()) {
                    val seasonNumberInt = sNum.toIntOrNull() ?: 1
                    val seasonKey = String.format(java.util.Locale.US, "%02d", seasonNumberInt)
                    val episodeNumberInt = eNum.toIntOrNull() ?: 1
                    val episodeKey = String.format(java.util.Locale.US, "ep%02d", episodeNumberInt)

                    FirebaseManager.getSeriesRef()
                        .child(seriesFirebaseKey)
                        .child("seasons")
                        .child(seasonKey)
                        .child(episodeKey)
                        .setValue(url)
                        .addOnSuccessListener {
                            Toast.makeText(context, "قسمت $eNum از فصل $sNum ثبت شد", Toast.LENGTH_SHORT).show()
                            edtEpisode.text.clear()
                            edtUrl.text.clear()
                            loadAndRenderSeasons()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "خطا در ثبت", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "لطفاً تمام فیلدها را پر کنید", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("بستن", null)
            .create()

        dialog.show()

        // اعمال استایل دکمه‌های high contrast برای دیالوگ مدیریت فصل‌ها
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(context.getColor(R.color.yellow))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(context.getColor(R.color.yellow))
        }
    }

    private fun fetchSeriesCategoriesForDropdown() {
        FirebaseManager.getSeriesCategoryRef().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryNames.clear()
                categoryMap.clear()
                for (cat in snapshot.children) {
                    val name = cat.child("name").getValue(String::class.java) ?: ""
                    val id = cat.child("id")?.getValue(Long::class.java) ?: cat.key?.replace("[^0-9]".toRegex(), "")?.toLongOrNull() ?: 0L
                    if (name.isNotEmpty()) {
                        categoryNames.add(name)
                        categoryMap[name] = id
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showSelectionMenu(options: List<String>, anchor: View, onSelected: (String) -> Unit) {
        val wrapper = androidx.appcompat.view.ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val popup = androidx.appcompat.widget.ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        popup.anchorView = anchor
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        popup.setAdapter(adapter)
        popup.isModal = true

        popup.setOnItemClickListener { _, _, position, _ ->
            onSelected(options[position])
            popup.dismiss()
        }
        popup.show()
    }

    private fun showAddSeriesDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        val view = layoutInflater.inflate(R.layout.dialog_add_serie, null)

        val edtNameFa = view.findViewById<EditText>(R.id.edtNameFa)
        val edtDescFa = view.findViewById<EditText>(R.id.edtDescFa)
        val edtTmdbId = view.findViewById<EditText>(R.id.edtTmdbId)
        val edtUrl1 = view.findViewById<EditText>(R.id.edtUrl1)
        val btnCategoryDropdown = view.findViewById<TextView>(R.id.btnCategoryDropdown)
        val txtGeneratedId = view.findViewById<TextView>(R.id.txtGeneratedId)

        edtUrl1?.visibility = View.GONE
        var selectedCategoryCode = 0L
        txtGeneratedId?.text = "در حال آماده‌سازی..."

        btnCategoryDropdown.setOnClickListener {
            showSelectionMenu(categoryNames, it) { selectedName ->
                btnCategoryDropdown.text = "دسته‌بندی: $selectedName"
                selectedCategoryCode = categoryMap[selectedName] ?: 0L
            }
        }

        builder.setView(view)
        builder.setTitle("افزودن سریال جدید")

        builder.setPositiveButton("افزودن") { _, _ ->
            FirebaseManager.getSeriesRef().orderByChild("id").limitToLast(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var maxNumericId = 0L
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val rawId = child.child("id").value
                                maxNumericId = when (rawId) {
                                    is Long -> rawId
                                    is Int -> rawId.toLong()
                                    is String -> rawId.toLongOrNull() ?: 0L
                                    else -> 0L
                                }
                            }
                        }

                        val dateFormat = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US)
                        val dateString = dateFormat.format(java.util.Date())
                        val datePrefix = "s_$dateString"

                        FirebaseManager.getSeriesRef().orderByKey().startAt(datePrefix).endAt(datePrefix + "\uf8ff")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    var newSerial = 1L
                                    for (child in snapshot.children) {
                                        val key = child.key ?: ""
                                        val serial = key.substringAfter(datePrefix).toLongOrNull() ?: 0L
                                        if (serial >= newSerial) newSerial = serial + 1
                                    }

                                    val newKey = "$datePrefix${String.format("%06d", newSerial)}"
                                    val newNumericId = maxNumericId + 1

                                    val newMap = mapOf(
                                        "id" to newNumericId,
                                        "name_fa" to edtNameFa.text.toString(),
                                        "description_fa" to edtDescFa.text.toString(),
                                        "tmdb_id" to edtTmdbId.text.toString(),
                                        "category" to selectedCategoryCode,
                                        "seasons" to emptyMap<String, Any>()
                                    )

                                    FirebaseManager.getSeriesRef().child(newKey).setValue(newMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "سریال با آیدی $newNumericId اضافه شد", Toast.LENGTH_SHORT).show()
                                            showManageSeasonsDialog(newKey)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "خطا در افزودن: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "خطا در اتصال به فایربیس", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "خطا در دریافت اطلاعات", Toast.LENGTH_SHORT).show()
                    }
                })
        }
        builder.setNegativeButton("انصراف", null)

        val dialog = builder.create()
        dialog.show()

        // استایل‌دهی به دکمه‌های دیالوگ افزودن سریال
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(requireContext().getColor(R.color.yellow))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(requireContext().getColor(R.color.yellow))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }
}