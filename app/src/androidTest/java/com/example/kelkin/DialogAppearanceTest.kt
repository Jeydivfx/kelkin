package com.example.kelkin

import android.content.Intent
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class DialogAppearanceTest {
    @Test fun platformAndAppCompatMessagesUseWhiteTextThenBlackOnFocus() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<UiPreviewActivity>(Intent(context, UiPreviewActivity::class.java)
            .putExtra("layout", R.layout.activity_login)).use { scenario ->
            scenario.onActivity { activity ->
                val compat = androidx.appcompat.app.AlertDialog.Builder(activity, R.style.CustomAlertDialog)
                    .setMessage("پیام نمونه").setPositiveButton("باشه", null).setNegativeButton("بستن", null).show()
                for (which in listOf(-1, -2)) {
                    val button = compat.getButton(which)
                    button.clearFocus()
                    assertEquals(Color.WHITE, button.textColors.defaultColor)
                    button.requestFocus()
                    assertEquals(Color.BLACK, button.currentTextColor)
                    assertNotNull(compat.getButton(which).background)
                }
                compat.dismiss()
                val platform = android.app.AlertDialog.Builder(activity, R.style.CustomAlertDialog)
                    .setMessage("پیام نمونه").setPositiveButton("باشه", null).show()
                val button = platform.getButton(-1)
                assertEquals(Color.WHITE, button.textColors.defaultColor)
                button.requestFocus()
                assertEquals(Color.BLACK, button.currentTextColor)
                platform.dismiss()
            }
        }
    }
}
