package com.example.kelkin

import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import com.example.kelkin.utils.openMenuDestination

class SidebarEntryTest {
    @get:Rule val rule = createAndroidComposeRule<SidebarHarnessActivity>()
    private val labels = listOf("خانه", "فیلم ها", "سریال ها", "تلویزیون", "جستجو", "تنظیمات")
    private fun key(code: Int) { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code); rule.waitForIdle() }
    private fun assertSingleSelection(label: String) {
        rule.onNodeWithText(label).assertIsFocused().assertIsSelected()
        for (other in labels.filter { it != label }) rule.onNodeWithText(other).assertIsNotSelected()
    }
    @Test fun enteringFromTopMiddleAndBottomAlwaysFocusesCurrentDestination() {
        val routes = listOf(R.id.homeFragment, R.id.moviesFragment, R.id.seriesFragment, R.id.tvFragment, R.id.searchFragment, R.id.settingsFragment)
        for ((destination, label) in routes.zip(labels)) {
            rule.runOnUiThread { if (rule.activity.nav.currentDestination?.id != destination) rule.activity.nav.navigate(destination) }
            rule.waitForIdle()
            for (position in listOf(0, 2, 4)) {
                rule.runOnUiThread { rule.activity.findViewById<View>(0x72010 + position).requestFocus() }
                rule.waitForIdle()
                key(KeyEvent.KEYCODE_DPAD_RIGHT)
                assertSingleSelection(label)
            }
        }
    }
    @Test fun movingWithinExpandedMenuShowsOnlyOneSelectedItem() {
        rule.runOnIdle { rule.activity.findViewById<View>(0x72010).requestFocus() }
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertSingleSelection("فیلم ها")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        assertSingleSelection("سریال ها")
    }

    @Test fun adminSectionsAlwaysBelongToSettingsInsteadOfViewerSections() {
        for (destination in listOf(R.id.movieListFragment, R.id.seriesListFragment, R.id.channelListFragment)) {
            rule.runOnUiThread { rule.activity.nav.navigate(destination) }
            rule.waitForIdle()
            rule.runOnUiThread { rule.activity.findViewById<View>(0x72010).requestFocus() }
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            assertSingleSelection("تنظیمات")
        }
    }

    @Test fun menuLeavesDetailsForSectionRootIncludingRestoredStacks() {
        rule.runOnUiThread { rule.activity.nav.navigate(R.id.movieDetailFragment) }
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.nav.openMenuDestination(R.id.homeFragment) }
        rule.waitForIdle()
        rule.runOnUiThread { assertEquals(R.id.homeFragment, rule.activity.nav.currentDestination?.id) }
        rule.runOnUiThread { rule.activity.nav.openMenuDestination(R.id.moviesFragment) }
        rule.waitForIdle()
        rule.runOnUiThread { assertEquals(R.id.moviesFragment, rule.activity.nav.currentDestination?.id) }
        rule.runOnUiThread { rule.activity.nav.navigate(R.id.movieDetailFragment) }
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.nav.openMenuDestination(R.id.moviesFragment) }
        rule.waitForIdle()
        rule.runOnUiThread { assertEquals(R.id.moviesFragment, rule.activity.nav.currentDestination?.id) }
    }
}
