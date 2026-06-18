package com.example.kelkin.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.AdminItemAdapter
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentMovieListBinding
import com.example.kelkin.utils.FirebaseManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MovieListFragment : Fragment(R.layout.fragment_movie_list) {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!

    private val categoryNames = mutableListOf<String>()
    private val categoryMap = mutableMapOf<String, Long>()
    private lateinit var adapter: AdminItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMovieListBinding.bind(view)

        fetchCategoriesForDropdown()

        // در onViewCreated
        binding.btnAddMovie.setOnClickListener {
            // ارسال یک آیدیِ جدید و دیتای خالی برای حالتِ افزودن
            showAddMovieDialog()
        }

        setupRecyclerView()
        loadMoviesFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = AdminItemAdapter(emptyList(),
            onEdit = { id, data -> showEditDialog(id, data) },
            onDelete = { id -> deleteMovie(id) }
        )
        binding.rvMoviesAdmin.layoutManager = LinearLayoutManager(context)
        binding.rvMoviesAdmin.adapter = adapter
    }

    private fun loadMoviesFromFirebase() {
        FirebaseManager.getMoviesRef().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // اصلاح اینجا انجام شد: مپ کردن دقیق به Pair
                val list = snapshot.children.mapNotNull { child ->
                    val key = child.key
                    val value = child.value as? Map<String, Any>
                    if (key != null && value != null) key to value else null
                }
                adapter.updateData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "خطا در دریافت لیست", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteMovie(id: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف فیلم")
            .setMessage("آیا مطمئن هستید؟")
            .setPositiveButton("بله") { _, _ ->
                FirebaseManager.deleteItem("movie", id) { success ->
                    if (success) Toast.makeText(context, "حذف شد", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("خیر", null)
            .show()
    }

    private fun showEditDialog(id: String, data: Map<String, Any>) {
        val builder = android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        val view = layoutInflater.inflate(R.layout.dialog_add_movie, null)

        val txtGeneratedId = view.findViewById<TextView>(R.id.txtGeneratedId)
        val edtNameFa = view.findViewById<EditText>(R.id.edtNameFa)
        val edtDescFa = view.findViewById<EditText>(R.id.edtDescFa)
        val edtTmdbId = view.findViewById<EditText>(R.id.edtTmdbId)
        val edtUrl1 = view.findViewById<EditText>(R.id.edtUrl1)
        val btnCategoryDropdown = view.findViewById<TextView>(R.id.btnCategoryDropdown)

        txtGeneratedId.text = "شناسه فیلم: $id"
        edtNameFa.setText(data["name_fa"] as? String ?: "")
        edtDescFa.setText(data["description_fa"] as? String ?: "")
        edtTmdbId.setText(data["tmdb_id"] as? String ?: "")
        edtUrl1.setText(data["videoUrl1"] as? String ?: "")

        var selectedCategoryCode = (data["category"] as? Long) ?: 0L
        // پیدا کردن نام کتگوری برای نمایش در دکمه
        val catName = categoryMap.entries.find { it.value == selectedCategoryCode }?.key
        btnCategoryDropdown.text = catName ?: "دسته‌بندی: $selectedCategoryCode"

        btnCategoryDropdown.setOnClickListener {
            showSelectionMenu(categoryNames, it) { selectedName ->
                btnCategoryDropdown.text = "دسته‌بندی: $selectedName"
                selectedCategoryCode = categoryMap[selectedName] ?: 0L
            }
        }

        builder.setView(view)
        builder.setPositiveButton("ذخیره تغییرات") { _, _ ->
            val updatedMap = mapOf(
                "id" to (data["id"] as? Long ?: 0L), // حفظ آیدی عددی قبلی
                "name_fa" to edtNameFa.text.toString(),
                "description_fa" to edtDescFa.text.toString(),
                "tmdb_id" to edtTmdbId.text.toString(),
                "videoUrl1" to edtUrl1.text.toString(),
                "category" to selectedCategoryCode
            )

            // استفاده از setValue به جای updateChildren برای اطمینان از اعمال تغییرات
            FirebaseManager.getMoviesRef().child(id).setValue(updatedMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Toast.makeText(context, "بروزرسانی شد", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "خطا در بروزرسانی", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("انصراف", null)

        val dialog = builder.create()
        dialog.show()

        // اعمال استایل زرد رنگ به دکمه‌ها
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(requireContext().getColor(R.color.yellow))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).apply {
            setTextColor(requireContext().getColor(R.color.yellow))
        }
    }
    // این تابع را هم اضافه کن تا دسته‌بندی‌ها پر شوند
    private fun fetchCategoriesForDropdown() {
        FirebaseManager.getCategoryRef().addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                categoryNames.clear()
                categoryMap.clear()
                for (cat in snapshot.children) {
                    val name = cat.child("name").getValue(String::class.java) ?: ""
                    val id = cat.key?.replace("[^0-9]".toRegex(), "")?.toLongOrNull() ?: 0L
                    if (name.isNotEmpty()) {
                        categoryNames.add(name)
                        categoryMap[name] = id
                    }
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }


    // این تابع باید داخل کلاس MovieListFragment باشد
    private fun showSelectionMenu(options: List<String>, anchor: View, onSelected: (String) -> Unit) {
        // استفاده از ContextThemeWrapper برای اعمال استایل پاپ‌آپ
        val wrapper = androidx.appcompat.view.ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val popup = androidx.appcompat.widget.ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        popup.anchorView = anchor

        // استفاده از آرایه گزینه‌ها برای نمایش در پاپ‌آپ
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        popup.setAdapter(adapter)

        popup.isModal = true

        popup.setOnItemClickListener { _, _, position, _ ->
            onSelected(options[position])
            popup.dismiss()
        }

        popup.show()
    }

    private fun showAddMovieDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        val view = layoutInflater.inflate(R.layout.dialog_add_movie, null)

        val edtNameFa = view.findViewById<EditText>(R.id.edtNameFa)
        val edtDescFa = view.findViewById<EditText>(R.id.edtDescFa)
        val edtTmdbId = view.findViewById<EditText>(R.id.edtTmdbId)
        val edtUrl1 = view.findViewById<EditText>(R.id.edtUrl1)
        val btnCategoryDropdown = view.findViewById<TextView>(R.id.btnCategoryDropdown)
        val txtGeneratedId = view.findViewById<TextView>(R.id.txtGeneratedId)

        var selectedCategoryCode = 0L
        txtGeneratedId.text = "در حال آماده‌سازی..."

        btnCategoryDropdown.setOnClickListener {
            showSelectionMenu(categoryNames, it) { selectedName ->
                btnCategoryDropdown.text = "دسته‌بندی: $selectedName"
                selectedCategoryCode = categoryMap[selectedName] ?: 0L
            }
        }

        builder.setView(view)
        builder.setTitle("افزودن فیلم جدید")

        builder.setPositiveButton("افزودن") { _, _ ->
            // مرحله ۱: پیدا کردن بزرگترین آیدی عددی در کل دیتابیس (سراسری)
            FirebaseManager.getMoviesRef().orderByChild("id").limitToLast(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var maxNumericId = 0L
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                maxNumericId = child.child("id").getValue(Long::class.java) ?: 0L
                            }
                        }

                        // مرحله ۲: تولید کلید بر اساس تاریخ برای مرتب‌سازی زمانی
                        val dateFormat = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US)
                        val dateString = dateFormat.format(java.util.Date())
                        val datePrefix = "m_$dateString"

                        // مرحله ۳: پیدا کردن سریال امروز برای ساخت کلید یکتا
                        FirebaseManager.getMoviesRef().orderByKey().startAt(datePrefix).endAt(datePrefix + "\uf8ff")
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
                                        "videoUrl1" to edtUrl1.text.toString(),
                                        "category" to selectedCategoryCode
                                    )

                                    // مرحله ۴: استفاده از setValue برای جایگزینی قطعی داده‌ها
                                    FirebaseManager.getMoviesRef().child(newKey).setValue(newMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "فیلم با آیدی $newNumericId اضافه شد", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "خطا در افزودن", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        builder.setNegativeButton("انصراف", null)

        val dialog = builder.create()
        dialog.show()

        // استایل‌دهی دکمه‌های زرد
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(requireContext().getColor(R.color.yellow))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
            setTextColor(requireContext().getColor(R.color.yellow))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}