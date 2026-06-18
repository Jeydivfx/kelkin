package com.example.kelkin.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var aboutClickCount = 0
    private var lastClickTime = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.cardAbout.postDelayed({ binding.cardAbout.requestFocus() }, 300)

        setupFocusListeners()
        setupClickListeners()

        binding.txtAppVersion.text = " نسخه اپلیکیشن کلکین: ${getAppVersionName()}"
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
            if (currentTime - lastClickTime > 1500) aboutClickCount = 0
            aboutClickCount++
            lastClickTime = currentTime

            if (aboutClickCount == 5) {
                aboutClickCount = 0
                showAdminAuthDialog() // متد جدید لاگین
            }
        }
    }

    private fun showAdminAuthDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 30)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // تایتل راست‌چین شده
        val txtTitle = android.widget.TextView(requireContext()).apply {
            text = "ورود به پنل مدیریت"
            setTextColor(requireContext().getColor(R.color.yellow))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.RIGHT
            setPadding(0, 0, 0, 20)
        }
        layout.addView(txtTitle)

        val edtUser = android.widget.EditText(requireContext()).apply {
            hint = "نام کاربری"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            gravity = android.view.Gravity.RIGHT
        }

        val edtPass = android.widget.EditText(requireContext()).apply {
            hint = "رمز عبور"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            gravity = android.view.Gravity.RIGHT
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(edtUser)
        layout.addView(edtPass)
        builder.setView(layout)

        builder.setPositiveButton("ورود") { dialog, _ ->
            val username = edtUser.text.toString()
            val password = edtPass.text.toString()

            if (username == "admin" && password == "padmin") {
                dialog.dismiss()
                androidx.navigation.Navigation.findNavController(requireView())
                    .navigate(R.id.action_settingsFragment_to_adminDashboardFragment)
            } else {
                Toast.makeText(context, "اطلاعات ورود اشتباه است!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("لغو", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(requireContext().getColor(R.color.yellow))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).apply {
            setTextColor(requireContext().getColor(R.color.yellow))
        }
    }

    private fun getAppVersionName(): String {
        return try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}