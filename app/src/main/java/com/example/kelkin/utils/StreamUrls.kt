package com.example.kelkin.utils

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

object StreamUrls {
    enum class Platform { DIRECT, VK, OK }
    fun platform(url: String): Platform {
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        fun hostIs(domain: String) = host == domain || host.endsWith(".$domain")
        return when { hostIs("vk.com") || hostIs("vkvideo.ru") -> Platform.VK; hostIs("ok.ru") -> Platform.OK; else -> Platform.DIRECT }
    }
    fun isValid(url: String): Boolean = runCatching { val uri = URI(url); uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() }.getOrDefault(false)
    fun embed(url: String, position: Long): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return url
        val query = uri.rawQuery.orEmpty().split('&').filter { it.isNotBlank() }.associate {
            val parts = it.split('=', limit = 2)
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }.toMutableMap()
        val seconds = position.coerceAtLeast(0) / 1000
        val target = when (platform(url)) {
            Platform.VK -> {
                val match = Regex("video(-?\\d+)_(\\d+)").find(uri.path.orEmpty())
                if (match != null) { query["oid"] = match.groupValues[1]; query["id"] = match.groupValues[2] }
                if (!query.containsKey("oid") || !query.containsKey("id")) return url
                query["autoplay"] = "1"; if (seconds > 0) query["t"] = "${seconds}s"
                "https://vk.com/video_ext.php"
            }
            Platform.OK -> {
                val id = uri.path.orEmpty().trimEnd('/').substringAfterLast('/')
                if (!id.matches(Regex("\\d+"))) return url
                query["autoplay"] = "1"; query["st.start"] = seconds.toString()
                "https://ok.ru/videoembed/$id"
            }
            Platform.DIRECT -> return url
        }
        return target + "?" + query.entries.joinToString("&") { URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8") }
    }
}
