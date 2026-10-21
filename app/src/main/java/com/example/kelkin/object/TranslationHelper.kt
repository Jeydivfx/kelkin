package com.example.kelkin.`object`

object TranslationHelper {


    private val genreMap = mapOf(
        "Action" to "اکشن",
        "Adventure" to "ماجراجویی",
        "Animation" to "انیمیشن",
        "Comedy" to "کمدی",
        "Crime" to "جنایی",
        "Documentary" to "مستند",
        "Drama" to "درام",
        "Family" to "خانوادگی",
        "Fantasy" to "فانتزی",
        "History" to "تاریخی",
        "Horror" to "ترسناک",
        "Music" to "موزیکال",
        "Mystery" to "معمایی",
        "Romance" to "عاشقانه",
        "Science Fiction" to "علمی تخیلی",
        "Sci-Fi & Fantasy" to "علمی تخیلی، فانتزی",
        "Action & Adventure" to "اکشن، ماجراجویی",
        "Kids" to "کودک",
        "News" to "خبر",
        "Reality" to "رئالیتی شو",
        "Soap" to "سریال ملودرام",
        "Talk" to "تاک شو",
        "War & Politics" to "جنگی، سیاسی",
        "TV Movie" to "فیلم تلویزیونی",
        "Thriller" to "هیجان‌انگیز",
        "War" to "جنگی",
        "Western" to "وسترن"
    )


    fun translateGenres(englishGenres: String): String {
        return englishGenres.split('،', ',').filter { it.isNotBlank() }.joinToString("، ") {
            genreMap[it.trim()] ?: it.trim()
        }
    }

    fun String.toPersianDigits(): String {
        var result = this
        val englishDigits = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val persianDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
        for (i in 0..9) {
            result = result.replace(englishDigits[i], persianDigits[i])
        }
        return result
    }

    fun translateDetailGenres(englishGenres: String): String = translateGenres(englishGenres)
        .split('،', ',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(2)
        .joinToString("، ")
}
