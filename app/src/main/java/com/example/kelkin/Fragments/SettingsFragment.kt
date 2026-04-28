package com.example.kelkin.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.text.BidiFormatter
import androidx.fragment.app.Fragment
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // تعریف SharedPreferences
    private lateinit var prefs: SharedPreferences
    private val bidi = BidiFormatter.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("kelkin_settings", Context.MODE_PRIVATE)

        // لود کردن مقادیر ذخیره شده قبلی
        loadSavedSettings()

        // فوکوس اولیه
        binding.cardLanguage.postDelayed({ binding.cardLanguage.requestFocus() }, 300)

        setupFocusListeners()
        setupClickListeners()
    }

    private fun loadSavedSettings() {
        val lang = prefs.getString("language", "فارسی")
        val fontSize = prefs.getInt("font_size", 18)
        val quality = prefs.getString("quality", "Auto")

        binding.txtLanguageDisplay.text = "زبان برنامه: ${bidi.unicodeWrap(lang)}"
        binding.txtFontSizeDisplay.text = "اندازه فونت: $fontSize sp"
        binding.txtQualityDisplay.text = "کیفیت پخش: $quality"
    }

    private fun setupFocusListeners() {
        // لیست جفت‌سازی کارت‌ها و متن‌هایشان برای افکت فوکوس
        val focusMap = mapOf(
            binding.cardLanguage to binding.txtLanguageDisplay,
            binding.cardFontSize to binding.txtFontSizeDisplay,
            binding.cardQuality to binding.txtQualityDisplay,
            binding.cardAbout to binding.txtAboutDisplay, // آیدی متنی درباره ما را در XML اضافه کن
            binding.cardClearCache to binding.txtClearDisplay
        )

        focusMap.forEach { (card, textView) ->
            card.setOnFocusChangeListener { _, hasFocus ->
                applyFocusEffect(textView, hasFocus)
                if (hasFocus) {
                    when(card.id) {
                        R.id.cardLanguage -> updateSidePanel("زبان برنامه", "زبان محیط کاربری را انتخاب کنید.", R.drawable.ic_menu_settings)
                        R.id.cardFontSize -> updateSidePanel("اندازه فونت", "سایز نوشته‌ها را به صورت عددی وارد کنید.", R.drawable.ic_menu_settings)
                        R.id.cardQuality -> updateSidePanel("کیفیت پخش", "کیفیت پیش‌فرض تماشای فیلم.", R.drawable.ic_menu_settings)
                        R.id.cardAbout -> updateSidePanel("درباره ما", "ارتباط با پشتیبانی در تلگرام:\n@Kelkin_App", R.drawable.ic_menu_settings)
                    }
                }
            }
        }
    }

    private fun applyFocusEffect(textView: TextView, hasFocus: Boolean) {
        // تنظیم نقطه ثقل انیمیشن روی سمت راست (چون RTL هستیم)
        textView.pivotX = textView.width.toFloat()
        textView.pivotY = textView.height.toFloat() / 2

        if (hasFocus) {
            textView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
            textView.setTextColor(Color.parseColor("#FFD700")) // طلایی
            textView.textSize = 20f
        } else {
            textView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            textView.setTextColor(Color.WHITE)
            textView.textSize = 15f // سایز اصلی که در XML داری
        }
    }

    private fun updateSidePanel(title: String, desc: String, iconRes: Int) {
        binding.txtSettingDescription.text = "$title\n\n$desc"
        binding.imgSettingDetail.setImageResource(iconRes)
    }

    private fun setupClickListeners() {
        binding.cardLanguage.setOnClickListener {
            showSelectionMenu(listOf("فارسی", "English"), it) { selected ->
                prefs.edit().putString("language", selected).apply()
                binding.txtLanguageDisplay.text = "زبان برنامه: ${bidi.unicodeWrap(selected)}"
            }
        }

        binding.cardFontSize.setOnClickListener {
            showFontSizeDialog()
        }

        binding.cardQuality.setOnClickListener {
            showSelectionMenu(listOf("Auto", "1080p", "720p", "480p"), it) { selected ->
                prefs.edit().putString("quality", selected).apply()
                binding.txtQualityDisplay.text = "کیفیت پخش: $selected"
            }
        }

        binding.cardClearCache.setOnClickListener {
            requireContext().cacheDir.deleteRecursively()
            Toast.makeText(context, "حافظه موقت پاک شد", Toast.LENGTH_SHORT).show()
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

    private fun showFontSizeDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        builder.setTitle("تنظیم عدد فعال‌سازی / فونت")

        val input = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "عدد را وارد کنید..."
            // برای اینکه عدد خیلی طولانی نشود و سیستم کرش نکند (مثلاً حداکثر 9 رقم)
            filters = arrayOf(android.text.InputFilter.LengthFilter(9))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    // بستن کیبورد
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(windowToken, 0)
                    // فوکوس را از EditText بردار تا کیبورد در TV حتماً بسته شود
                    clearFocus()
                    true
                } else false
            }
        }

        // ایجاد یک لایه برای حاشیه دور EditText (فاصله از لبه‌های دیالوگ)
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 10)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("ذخیره") { _, _ ->
            val inputStr = input.text.toString()
            if (inputStr.isNotEmpty()) {
                try {
                    val value = inputStr.toInt()

                    // ذخیره در تنظیمات
                    prefs.edit().putInt("font_size", value).apply()

                    // نمایش در فرگمنت
                    binding.txtFontSizeDisplay.text = "عدد تنظیم شده: $value"

                    // اگر عدد خیلی بزرگ باشد، برای فونت استفاده نمی‌کنیم (فقط ذخیره می‌شود)
                    if (value in 10..40) {
                        applyFocusEffect(binding.txtFontSizeDisplay, true)
                    }

                    Toast.makeText(context, "با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(context, "عدد وارد شده بسیار بزرگ است", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("لغو") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.show()

        // فیکس کردن رنگ دکمه‌ها که در تصویر دیده نمی‌شدند
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FFD700")) // طلایی
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE) // سفید


    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}