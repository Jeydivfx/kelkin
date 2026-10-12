package com.example.kelkin.utils
import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test
class SubtitleSelectionTest {
 @Test fun noneDisablesText() {
  assertEquals(-1, subtitleMenuAction(C.SELECTION_FLAG_FORCED.inv(), false, false))
  assertEquals(0, subtitleMenuAction(C.SELECTION_FLAG_FORCED.inv(), false, true))
 }
 @Test fun selectingAfterNoneEnablesText() {
  assertEquals(1, subtitleMenuAction(C.SELECTION_FLAG_FORCED.inv(), true, true))
  assertEquals(1, subtitleMenuAction(C.SELECTION_FLAG_FORCED.inv(), true, false))
 }
 @Test fun normalSelectionIsUnchanged() {
  assertEquals(0, subtitleMenuAction(0, false, false))
  assertEquals(0, subtitleMenuAction(0, true, false))
 }
}
