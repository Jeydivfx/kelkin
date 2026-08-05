package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kelkin.MainActivity
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
        val buttons = listOf(binding.cardAbout, binding.cardClearCache)

        buttons.forEach { button ->
            button.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    when (button.id) {
                        R.id.cardAbout -> updateSidePanel("درباره ما", "ارتباط با پشتیبانی در تلگرام:\n@Kelkin_App", R.drawable.ic_menu_settings)
                        R.id.cardClearCache -> updateSidePanel("حافظه موقت", "پاک کردن حافظه پنهان برنامه برای بهبود عملکرد سیستم پلیر.", R.drawable.ic_menu_settings)
                    }
                }
            }
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
                showAdminAuthDialog()
            }
        }
    }

    private fun showAdminAuthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_auth, null)
        val dialog = android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val edtUser = dialogView.findViewById<android.widget.EditText>(R.id.edtAdminUsername)
        val edtPass = dialogView.findViewById<android.widget.EditText>(R.id.edtAdminPassword)
        val btnLogin = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAdminLogin)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAdminCancel)

        btnLogin.setOnClickListener {
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

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        edtUser.requestFocus()
    }

    private fun getAppVersionName(): String {
        return try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}