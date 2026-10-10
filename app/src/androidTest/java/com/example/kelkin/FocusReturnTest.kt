package com.example.kelkin

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.fragment.NavHostFragment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusReturnTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun grid(activity: FocusHarnessActivity) = (activity.supportFragmentManager.findFragmentById(0x71001) as NavHostFragment)
        .childFragmentManager.primaryNavigationFragment as FocusGridFragment
    private fun settle() { instrumentation.waitForIdleSync(); Thread.sleep(300); instrumentation.waitForIdleSync() }

    @Test fun backRestoresOffscreenItemAndRecreationPreservesIt() {
        ActivityScenario.launch(FocusHarnessActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { grid(it).list.scrollToPosition(36) }
            settle()
            scenario.onActivity { activity ->
                val child = grid(activity).list.layoutManager!!.findViewByPosition(36)!!
                assertTrue(child.requestFocus())
                val nav = (activity.supportFragmentManager.findFragmentById(0x71001) as NavHostFragment).navController
                assertEquals("card-36", nav.currentBackStackEntry!!.savedStateHandle.get<String>("focus_key_${0x71006}"))
                child.performClick()
            }
            settle()
            scenario.onActivity { activity -> (activity.supportFragmentManager.findFragmentById(0x71001) as NavHostFragment).navController.popBackStack() }
            settle()
            scenario.onActivity { activity ->
                val nav = (activity.supportFragmentManager.findFragmentById(0x71001) as NavHostFragment).navController
                assertEquals("saved anchor", 0x71006, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("focus_row"))
                assertEquals("card-36", activity.currentFocus?.tag)
            }
            scenario.recreate()
            settle()
            scenario.onActivity { assertEquals("card-36", it.currentFocus?.tag) }
        }
    }

    @Test fun metadataRefreshDoesNotStealFocus() {
        ActivityScenario.launch(FocusHarnessActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { assertTrue(grid(it).list.layoutManager!!.findViewByPosition(3)!!.requestFocus()) }
            scenario.onActivity { grid(it).cards.notifyItemRangeChanged(0, 10, "metadata") }
            settle()
            scenario.onActivity { assertEquals("card-3", it.currentFocus?.tag) }
        }
    }

    @Test fun delayedDataKeepsSavedItemWhileRowIsHidden() {
        ActivityScenario.launch(FocusHarnessActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                val child = grid(activity).list.layoutManager!!.findViewByPosition(3)!!
                assertTrue(child.requestFocus())
            }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
            settle()
            scenario.onActivity { activity ->
                FocusGridFragment.delayNextLoad = true
                (activity.supportFragmentManager.findFragmentById(0x71001) as NavHostFragment).navController.popBackStack()
            }
            settle()
            Thread.sleep(1000)
            settle()
            scenario.onActivity { assertEquals("card-3", it.currentFocus?.tag) }
        }
    }
}
