package com.example.kelkin.utils

import com.example.kelkin.`object`.TranslationHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailGenresTest {
    @Test fun combinedTvGenresAreLimitedAfterTranslation() {
        assertEquals("اکشن، ماجراجویی", TranslationHelper.translateDetailGenres("Action & Adventure، War & Politics، Drama"))
    }
    @Test fun duplicatesDoNotConsumeTheLimit() {
        assertEquals("جنگی، درام", TranslationHelper.translateDetailGenres("War، War، Drama"))
    }
    @Test fun emptyGenresStayEmpty() {
        assertEquals("", TranslationHelper.translateDetailGenres(""))
    }
    @Test fun combinedTranslationsUsePersianCommas() {
        assertEquals("اکشن، ماجراجویی، علمی تخیلی، فانتزی، جنگی، سیاسی",
            TranslationHelper.translateGenres("Action & Adventure، Sci-Fi & Fantasy، War & Politics"))
    }
}
