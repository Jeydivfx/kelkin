package com.example.kelkin.Fragments

import android.content.Context
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

        binding.cardSubtitleSize.postDelayed({ binding.cardSubtitleSize.requestFocus() }, 300)

        loadSubtitleSettingsUI()
        setupClickListeners()

        binding.txtAppVersion.text = " نسخه اپلیکیشن کلکین: ${getAppVersionName()}"
    }

    private fun loadSubtitleSettingsUI() {
        val prefs = requireContext().getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)
        val sizeIndex = prefs.getInt("sub_size_index", 1) // 0: کوچک، 1: متوسط، 2: بزرگ
        val colorIndex = prefs.getInt("sub_color_index", 0) // 0: زرد، 1: سفید، 2: سبز

        val sizes = arrayOf("کوچک", "متوسط", "بزرگ")
        val colors = arrayOf("زرد", "سفید", "سبز")

        binding.cardSubtitleSize.text = "اندازه فونت زیرنویس: ${sizes[sizeIndex]}"
        binding.cardSubtitleColor.text = "رنگ زیرنویس: ${colors[colorIndex]}"
    }

    private fun setupClickListeners() {
        val prefs = requireContext().getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)

        binding.cardSubtitleSize.setOnClickListener {
            val sizes = arrayOf("کوچک", "متوسط", "بزرگ")
            var currentIndex = prefs.getInt("sub_size_index", 1)

            android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle("انتخاب اندازه زیرنویس")
                .setSingleChoiceItems(sizes, currentIndex) { dialog, which ->
                    prefs.edit().putInt("sub_size_index", which).apply()
                    loadSubtitleSettingsUI()
                    dialog.dismiss()
                }
                .show()
        }

        binding.cardSubtitleColor.setOnClickListener {
            val colors = arrayOf("زرد", "سفید", "سبز")
            var currentIndex = prefs.getInt("sub_color_index", 0)

            android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle("انتخاب رنگ زیرنویس")
                .setSingleChoiceItems(colors, currentIndex) { dialog, which ->
                    prefs.edit().putInt("sub_color_index", which).apply()
                    loadSubtitleSettingsUI()
                    dialog.dismiss()
                }
                .show()
        }

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