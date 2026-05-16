package com.example.kelkin.Fragments

import android.R.attr.textStyle
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentSettingsBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences

    // متغیرهای شمارنده کلیک کاملاً مخفی
    private var aboutClickCount = 0
    private var lastClickTime = 0L

    private val categoryNames = mutableListOf<String>()
    private val categoryMap = mutableMapOf<String, Long>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("kelkin_settings", Context.MODE_PRIVATE)

        fetchCategoriesFromFirebase()

        binding.cardAbout.postDelayed({ binding.cardAbout.requestFocus() }, 300)

        setupFocusListeners()
        setupClickListeners()

        binding.txtAppVersion.text = "نسخه اپلیکیشن: 1.0.0 (Kelkin TV)"
    }

    private fun fetchCategoriesFromFirebase() {
        try {
            val database = FirebaseDatabase.getInstance()
            val catRef = database.getReference("category")

            catRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryNames.clear()
                    categoryMap.clear()

                    for (catSnapshot in snapshot.children) {
                        val catKey = catSnapshot.key ?: ""
                        val catName = catSnapshot.child("name").getValue(String::class.java) ?: ""

                        if (catName.isNotEmpty()) {
                            val numericId = catKey.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                            categoryNames.add(catName)
                            categoryMap[catName] = numericId
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase_Cat", "Failed to load categories: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("Firebase_Cat", "Firebase error: ${e.message}")
        }
    }

    private fun setupFocusListeners() {
        val focusMap = mapOf(
            binding.cardAbout to binding.txtAboutDisplay,
            binding.cardClearCache to binding.txtClearDisplay
        )

        focusMap.forEach { (card, textView) ->
            card.setOnFocusChangeListener { _, hasFocus ->
                applyFocusEffect(textView, hasFocus)
                if (hasFocus) {
                    when(card.id) {
                        R.id.cardAbout -> updateSidePanel("درباره ما", "ارتباط با پشتیبانی در تلگرام:\n@Kelkin_App", R.drawable.ic_menu_settings)
                        R.id.cardClearCache -> updateSidePanel("حافظه موقت", "پاک کردن حافظه پنهان برنامه برای بهبود عملکرد سیستم پلیر.", R.drawable.ic_menu_settings)
                    }
                }
            }
        }
    }

    private fun applyFocusEffect(textView: TextView, hasFocus: Boolean) {
        textView.pivotX = textView.width.toFloat()
        textView.pivotY = textView.height.toFloat() / 2

        if (hasFocus) {
            textView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
            textView.setTextColor(Color.parseColor("#FFD700"))
            textView.textSize = 17f
        } else {
            textView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            textView.setTextColor(Color.WHITE)
            textView.textSize = 15f
        }
    }

    private fun updateSidePanel(title: String, desc: String, iconRes: Int) {
        binding.txtSettingDescription.text = "$title\n\n$desc"
        binding.imgSettingDetail.setImageResource(iconRes)
    }

    private fun setupClickListeners() {
        binding.cardClearCache.setOnClickListener {
            requireContext().cacheDir.deleteRecursively()
            Toast.makeText(context, "حافظه موقت پاک شد", Toast.LENGTH_SHORT).show()
        }

        binding.cardAbout.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 1500) {
                aboutClickCount = 0
            }

            aboutClickCount++
            lastClickTime = currentTime

            if (aboutClickCount == 5) {
                aboutClickCount = 0
                showAdminAuthDialog()
            }
            // حذف کامل بخش Else برای مخفی نگه داشتن کامل شمارنده دکمه
        }
    }

    private fun showSelectionMenu(options: List<String>, anchor: View, onSelected: (String) -> Unit) {
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val popup = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)
        popup.anchorView = anchor
        popup.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            onSelected(options[position])
            popup.dismiss()
        }
        popup.show()
        popup.listView?.apply {
            id = R.id.radio_menu_list
            requestFocus()
        }
    }

    // دیالوگ ورود کاملاً راست‌چین و سفارشی شده با مشخصات درخواستی
    private fun showAdminAuthDialog() {
        // ۱. استفاده از یک استایل تیره و بدون تایتل پیش‌فرض برای حذف لجبازی اندروید
        val builder = android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)

        // کانتینر اصلی دیالوگ به صورت عمودی
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 30)
            layoutDirection = View.LAYOUT_DIRECTION_RTL // اجبار کل کانتینر به راست‌چین
        }

        // ۲. ساخت تایتل سفارشی برای تضمین راست‌چین شدن (جایگزین builder.setTitle)
        val txtCustomTitle = TextView(requireContext()).apply {
            text = "ورود به پنل مدیریت فایربیس"
            setTextColor(Color.parseColor("#FFD700")) // رنگ طلایی هماهنگ با تم کلکین
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.RIGHT or android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 40) // فاصله مناسب از فیلدهای پایینی
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        layout.addView(txtCustomTitle)

        // فیلد نام کاربری
        val edtUser = EditText(requireContext()).apply {
            hint = "نام کاربری"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = android.view.Gravity.RIGHT or android.view.Gravity.CENTER_VERTICAL
        }

        // فیلد رمز عبور
        val edtPass = EditText(requireContext()).apply {
            hint = "رمز عبور"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = android.view.Gravity.RIGHT or android.view.Gravity.CENTER_VERTICAL
        }

        layout.addView(edtUser)
        layout.addView(edtPass)
        builder.setView(layout)

        builder.setPositiveButton("ورود") { dialog, _ ->
            val username = edtUser.text.toString()
            val password = edtPass.text.toString()

            if (username == "jeydi" && password == "jeydi") {
                dialog.dismiss()
                showAddMovieDialog()
            } else {
                Toast.makeText(context, "اطلاعات ورود اشتباه است!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("لغو") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        // ۳. اجبار لایه دکوراتیو دیالوگ به چینش RTL جهت تراز شدن دکمه‌های «ورود» و «لغو» در سمت چپ/راست استاندارد
        dialog.window?.decorView?.layoutDirection = View.LAYOUT_DIRECTION_RTL

        // رنگ‌آمیزی نهایی دکمه‌های دیالوگ
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FFD700"))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
    }

    private fun showAddMovieDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        builder.setTitle("افزودن فیلم جدید به کلکین")

        val formView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_movie, null)
        builder.setView(formView)

        val txtGeneratedId = formView.findViewById<TextView>(R.id.txtGeneratedId)
        val edtNameFa = formView.findViewById<EditText>(R.id.edtNameFa)
        val edtDescFa = formView.findViewById<EditText>(R.id.edtDescFa)
        val btnCategoryDropdown = formView.findViewById<TextView>(R.id.btnCategoryDropdown)
        val edtTmdbId = formView.findViewById<EditText>(R.id.edtTmdbId)
        val edtUrl1 = formView.findViewById<EditText>(R.id.edtUrl1)

        val timeStamp = SimpleDateFormat("yyMMddHHmmss", Locale.US).format(Date())
        val generatedMovieKey = "m_$timeStamp"
        txtGeneratedId.text = "شناسه خودکار فیلم: $generatedMovieKey"

        var selectedCategoryCode = 0L
        btnCategoryDropdown.setOnClickListener {
            if (categoryNames.isEmpty()) {
                Toast.makeText(context, "لیست دسته‌بندی‌ها خالی است یا لود نشده!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showSelectionMenu(categoryNames, it) { selectedName ->
                btnCategoryDropdown.text = "دسته‌بندی: $selectedName"
                btnCategoryDropdown.setTextColor(Color.WHITE)
                selectedCategoryCode = categoryMap[selectedName] ?: 0L
            }
        }

        builder.setPositiveButton("ارسال به فایربیس") { dialog, _ ->
            val nameFa = edtNameFa.text.toString()
            val descFa = edtDescFa.text.toString()
            val tmdbId = edtTmdbId.text.toString()
            val url1 = edtUrl1.text.toString()

            if (nameFa.isEmpty() || url1.isEmpty() || selectedCategoryCode == 0L) {
                Toast.makeText(context, "لطفاً تمام فیلدها را کامل کنید", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val numericId = timeStamp.toLongOrNull() ?: 0L

            val movieUploadMap = hashMapOf<String, Any>(
                "id" to numericId,
                "name_fa" to nameFa,
                "description_fa" to descFa,
                "category" to selectedCategoryCode,
                "tmdb_id" to tmdbId,
                "videoUrl1" to url1
            )

            sendMovieToFirebase(movieUploadMap, generatedMovieKey)
            dialog.dismiss()
        }

        builder.setNegativeButton("انصراف") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.window?.decorView?.layoutDirection = View.LAYOUT_DIRECTION_RTL
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FFD700"))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
    }

    private fun sendMovieToFirebase(movieMap: HashMap<String, Any>, movieKey: String) {
        try {
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("movies").child(movieKey)

            myRef.setValue(movieMap)
                .addOnSuccessListener {
                    Toast.makeText(context, "فیلم با موفقیت ثبت شد!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "خطا در ثبت دیتابیس: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در برقراری ارتباط با فایربیس!", Toast.LENGTH_SHORT).show()
            Log.e("Firebase_Error", e.message ?: "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}