package com.example.kelkin

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LayoutPreviewTest {
    @Test fun captureTvLayouts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val output = File(context.getExternalFilesDir(null), "ui-checks").apply { mkdirs() }
        for ((name, layout) in listOf("home" to R.layout.fragment_home, "movie-details" to R.layout.fragment_movie_detail, "series-details" to R.layout.fragment_series_details)) {
            ActivityScenario.launch<UiPreviewActivity>(Intent(context, UiPreviewActivity::class.java).putExtra("layout", layout)).use { scenario ->
                instrumentation.waitForIdleSync()
                Thread.sleep(500)
                scenario.onActivity { assertTrue(it.window.decorView.width > 0) }
                val screenshot = instrumentation.uiAutomation.takeScreenshot()
                File(output, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                screenshot.recycle()
            }
        }
    }
}
