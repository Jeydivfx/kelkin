package com.example.kelkin.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogGridSizingTest {
    @Test fun portraitPhonesStartWithTwoColumns() {
        listOf(320, 360, 412).forEach { width -> assertEquals(2, catalogColumns(width - 32, 1f)) }
    }

    @Test fun densityDoesNotChangeColumnCountForTheSameScreenWidth() {
        for (density in listOf(1f, 2f, 3f)) {
            assertEquals(2, catalogColumns((328 * density).toInt(), density))
            assertEquals(4, catalogColumns((680 * density).toInt(), density))
        }
    }

    @Test fun rotationsAndTabletWidthsHaveBoundedColumnCounts() {
        assertEquals(2, catalogColumns(0, 1f))
        assertEquals(3, catalogColumns(500, 1f))
        assertEquals(6, catalogColumns(2000, 1f))
    }
}
