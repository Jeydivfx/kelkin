package com.example.kelkin

import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LoginIntroTest {
    @Test fun loginActionsHaveAlignedBoundsAndRemoteNavigation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        ActivityScenario.launch<UiPreviewActivity>(Intent(context, UiPreviewActivity::class.java)
            .putExtra("layout", R.layout.activity_login)).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val google = activity.findViewById<View>(R.id.materialCardGoogle)
                val retry = activity.findViewById<View>(R.id.btnRetryActivation)
                assertTrue(google.hasFocus())
                assertEquals(google.width, retry.width)
                assertEquals(google.left, retry.left)
                assertTrue(retry.top >= google.bottom)
                assertEquals(retry, google.focusSearch(View.FOCUS_DOWN))
                assertEquals(google, retry.focusSearch(View.FOCUS_UP))
            }
            val image = instrumentation.uiAutomation.takeScreenshot()
            val output = File(context.getExternalFilesDir(null), "ui-checks").apply { mkdirs() }
            File(output, "login.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
    }

    @Test fun remoteOkSkipsIntroEvenWhenPlayerHasFocus() {
        ActivityScenario.launch(SplashActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.playerView).requestFocus()
                assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)))
                val navigated = SplashActivity::class.java.getDeclaredField("isNavigated").apply { isAccessible = true }
                val player = SplashActivity::class.java.getDeclaredField("exoPlayer").apply { isAccessible = true }
                assertEquals(true, navigated.get(activity))
                assertNull(player.get(activity))
                assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER)))
            }
        }
    }
}
