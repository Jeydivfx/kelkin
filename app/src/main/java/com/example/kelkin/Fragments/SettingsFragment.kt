package com.example.kelkin.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kelkin.LoginActivity
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentSettingsBinding
import com.example.kelkin.utils.AdminSession
import com.example.kelkin.utils.RecyclerFocusMemory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var adminDialog: androidx.appcompat.app.AlertDialog? = null
    private var messageDialog: androidx.appcompat.app.AlertDialog? = null
    private var choiceDialog: androidx.appcompat.app.AlertDialog? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)
        RecyclerFocusMemory(this, binding.cardSubtitleSize)
        loadSubtitleSettingsUI()
        val prefs = requireContext().getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)
        binding.cardSubtitleSize.setOnClickListener {
            choose("اندازه زیرنویس", arrayOf("کوچک", "متوسط", "بزرگ"), prefs.getInt("sub_size_index", 1).coerceIn(0, 2)) {
                prefs.edit().putInt("sub_size_index", it).apply(); loadSubtitleSettingsUI()
            }
        }
        binding.cardSubtitleColor.setOnClickListener {
            choose("رنگ زیرنویس", arrayOf("زرد", "سفید", "سبز"), prefs.getInt("sub_color_index", 0).coerceIn(0, 2)) {
                prefs.edit().putInt("sub_color_index", it).apply(); loadSubtitleSettingsUI()
            }
        }
        binding.cardClearCache.setOnClickListener {
            val appContext = requireContext().applicationContext
            binding.cardClearCache.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val success = withContext(Dispatchers.IO) { runCatching { Glide.get(appContext).clearDiskCache() }.isSuccess }
                if (_binding != null) {
                    if (success) Glide.get(appContext).clearMemory()
                    binding.cardClearCache.isEnabled = true
                    Toast.makeText(appContext, if (success) "حافظه موقت تصاویر پاک شد" else "پاک‌کردن حافظه موقت انجام نشد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.cardAbout.setOnClickListener {
            showSettingsMessage("کلکین، پنجرهٔ شما",
                if (com.example.kelkin.BuildConfig.IS_TV) "برای بازکردن کانال‌ها و انتخاب فیلم و سریال از کلیدهای جهت و OK استفاده کنید. در پخش زنده، بالا فهرست سورس و پایین فهرست کانال را باز می‌کند. با نگه‌داشتن OK کانال را نشان کنید."
                else "برای انتخاب فیلم، سریال یا کانال روی کارت آن بزنید. فهرست‌ها را با کشیدن جابه‌جا کنید. در پلیر با لمس تصویر کنترل پخش و زیرنویس نمایش داده می‌شود؛ دکمه‌های کانال‌ها و سورس‌ها برای پخش زنده هستند.",
                "باشه")
        }
        binding.adminSignInAction.setOnClickListener { showAdminAuthDialog() }
        binding.accountSignOutAction.setOnClickListener {
            showSettingsMessage("خروج از حساب", "از این حساب خارج می‌شوید؟", "خروج", "انصراف") {
                FirebaseAuth.getInstance().signOut(); AdminSession.signOut()
                GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }
    }

    private fun showSettingsMessage(title: String, message: String, confirm: String,
                                    cancel: String? = null, onConfirm: () -> Unit = {}) {
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        fun label(value: String, size: Float, bold: Boolean) = android.widget.TextView(context).apply {
            text = value
            textSize = size
            gravity = android.view.Gravity.RIGHT
            textDirection = View.TEXT_DIRECTION_RTL
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_white))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context,
                if (bold) R.font.yekanbakh_bold else R.font.yekanbakh_regular)
        }
        panel.addView(label(title, 20f, true), LinearLayout.LayoutParams(-1, -2))
        val body = label(message, 16f, false).apply { setLineSpacing(dp(5).toFloat(), 1f) }
        val scroll = androidx.core.widget.NestedScrollView(context).apply {
            isFillViewport = false
            addView(body)
        }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.RIGHT or android.view.Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        panel.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(panel).create()
        fun action(text: String, click: () -> Unit): Button = Button(context).apply {
            this.text = text
            textSize = 15f
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.yekanbakh_bold)
            includeFontPadding = false
            gravity = android.view.Gravity.CENTER
            minimumHeight = 0; minHeight = 0
            backgroundTintList = null
            setBackgroundResource(R.drawable.sl_button_high_contrast)
            setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
            setPadding(dp(12), 0, dp(12), 0)
            isFocusableInTouchMode = true
            setOnClickListener { dialog.dismiss(); click() }
            actions.addView(this, LinearLayout.LayoutParams(dp(128), dp(48)).apply { marginEnd = dp(12) })
        }
        val positive = action(confirm, onConfirm)
        val negative = cancel?.let { action(it) {} }
        messageDialog?.dismiss()
        messageDialog = dialog
        dialog.setOnDismissListener { if (messageDialog === dialog) messageDialog = null }
        dialog.show()
        dialog.window?.setLayout(minOf(dp(560), resources.displayMetrics.widthPixels - dp(64)), -2)
        // Keep the footer outside the scroll region even when the description is long.
        scroll.post {
            val limit = (resources.displayMetrics.heightPixels * 0.45f).toInt()
            if (scroll.height > limit) scroll.layoutParams = scroll.layoutParams.apply { height = limit }
        }
        (negative ?: positive).requestFocus()
    }

    private fun choose(title: String, options: Array<String>, selected: Int, onSelect: (Int) -> Unit) {
        val density = resources.displayMetrics.density
        val panel = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (12 * density).toInt(), (24 * density).toInt(), (12 * density).toInt())
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle(title).setView(panel).setNegativeButton("بستن", null).create()
        val buttons = options.mapIndexed { index, label ->
            MaterialButton(requireContext()).apply {
                text = if (index == selected) "$label  ✓" else label
                contentDescription = if (index == selected) "$label، انتخاب فعلی" else label
                backgroundTintList = null
                setBackgroundResource(R.drawable.sl_button_high_contrast)
                backgroundTintList = null
                setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
                textSize = 18f
                setOnClickListener { onSelect(index); dialog.dismiss() }
                panel.addView(this, LinearLayout.LayoutParams(-1, (56 * density).toInt()).apply { bottomMargin = (12 * density).toInt() })
            }
        }
        choiceDialog?.dismiss()
        choiceDialog = dialog
        dialog.setOnDismissListener { choiceDialog = null }
        dialog.show()
        buttons[selected].requestFocus()
    }
    private fun loadSubtitleSettingsUI() {
        if (_binding == null) return
        val prefs = requireContext().getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)
        binding.txtSubtitleSizeValue.text = "${arrayOf("کوچک", "متوسط", "بزرگ")[prefs.getInt("sub_size_index", 1).coerceIn(0, 2)]}"
        binding.txtSubtitleColorValue.text = "${arrayOf("زرد", "سفید", "سبز")[prefs.getInt("sub_color_index", 0).coerceIn(0, 2)]}"
    }
    private fun showAdminAuthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_auth, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(dialogView).create()
        adminDialog = dialog
        val email = dialogView.findViewById<EditText>(R.id.edtAdminUsername)
        val password = dialogView.findViewById<EditText>(R.id.edtAdminPassword)
        val login = dialogView.findViewById<Button>(R.id.btnAdminLogin)
        val status = dialogView.findViewById<android.widget.TextView>(R.id.txtAdminLoginStatus)
        fun showError(message: String) { status.text = message; status.visibility = View.VISIBLE }
        var job: Job? = null
        dialogView.findViewById<Button>(R.id.btnAdminCancel).setOnClickListener { dialog.dismiss() }
        login.setOnClickListener {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches()) { showError("ایمیل معتبر وارد کنید"); return@setOnClickListener }
            if (password.text.isNullOrBlank()) { showError("رمز عبور را وارد کنید"); return@setOnClickListener }
            status.visibility = View.GONE
            login.isEnabled = false; login.text = "در حال ورود…"
            job = viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (AdminSession.signIn(email.text.toString(), password.text.toString())) {
                        dialog.dismiss()
                        findNavController().navigate(R.id.action_settingsFragment_to_adminDashboardFragment)
                    } else showError("حساب وارد شد، اما نقش مدیر ندارد. UID همین حساب باید در admins مقدار boolean true داشته باشد.")
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) { showError("زمان اتصال تمام شد؛ اینترنت را بررسی کنید و دوباره تلاش کنید.") }
                catch (e: CancellationException) { throw e }
                catch (e: com.google.firebase.auth.FirebaseAuthException) {
                    showError("ورود با ایمیل و رمز انجام نشد (${e.errorCode}). حساب و رمز را در Authentication و فعال بودن Email/Password بررسی کنید.")
                }
                catch (e: AdminSession.RoleCheckException) {
                    showError("ورود حساب موفق بود، اما بررسی نقش مدیر در دیتابیس انجام نشد. Rules منتشرشده، پروژهٔ Firebase و اتصال را بررسی کنید.")
                }
                catch (e: Exception) { showError("ورود انجام نشد؛ اتصال اینترنت و تنظیمات پروژهٔ Firebase را بررسی کنید.") }
                finally { login.isEnabled = true; login.text = "ورود مدیر" }
            }
        }
        dialog.setOnDismissListener { job?.cancel(); adminDialog = null }
        dialog.show(); email.requestFocus()
    }
    override fun onResume() { super.onResume(); (activity as? MainActivity)?.collapseSidebar() }
    override fun onDestroyView() { messageDialog?.dismiss(); choiceDialog?.dismiss(); adminDialog?.dismiss(); _binding = null; super.onDestroyView() }
}
