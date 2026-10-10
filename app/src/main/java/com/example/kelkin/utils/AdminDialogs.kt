package com.example.kelkin.utils

import android.widget.EditText
import androidx.appcompat.app.AlertDialog

/** Keep the editor open until validation and the remote write succeed. */
fun AlertDialog.installValidatedSave(validate: () -> Boolean, save: (done: (Boolean) -> Unit) -> Unit) {
    val button = getButton(AlertDialog.BUTTON_POSITIVE)
    button.setOnClickListener {
        if (!validate()) return@setOnClickListener
        button.isEnabled = false
        save { success ->
            if (isShowing) {
                button.isEnabled = true
                if (success) dismiss()
            }
        }
    }
}
fun EditText.requireText(message: String): Boolean {
    if (text.toString().trim().isNotEmpty()) return true
    error = message; requestFocus(); return false
}
fun EditText.requireStreamUrl(): Boolean {
    if (StreamUrls.isValid(text.toString().trim())) return true
    error = "لینک معتبر http یا https وارد کنید"; requestFocus(); return false
}
fun EditText.optionalTmdbId(): Boolean {
    if (text.isNullOrBlank() || text.toString().trim().matches(Regex("[0-9]+"))) return true
    error = "شناسهٔ TMDB باید عدد باشد"; requestFocus(); return false
}
