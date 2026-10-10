package com.example.kelkin.utils

import org.junit.Assert.*
import org.junit.Test

class StreamUrlsTest {
    @Test fun platformUsesHostRatherThanSubstring() {
        assertEquals(StreamUrls.Platform.DIRECT, StreamUrls.platform("https://example.org/vk.com/movie.mp4"))
        assertEquals(StreamUrls.Platform.DIRECT, StreamUrls.platform("https://vk.com.example.org/video1_2"))
        assertEquals(StreamUrls.Platform.VK, StreamUrls.platform("https://m.vk.com/video-12_34"))
        assertEquals(StreamUrls.Platform.OK, StreamUrls.platform("https://ok.ru/video/123"))
    }
    @Test fun vkResumePreservesPrivateVideoHash() {
        val embedded = StreamUrls.embed("https://vk.com/video-12_34?hash=secret", 125000)
        assertTrue(embedded.startsWith("https://vk.com/video_ext.php?"))
        assertTrue(embedded.contains("oid=-12"))
        assertTrue(embedded.contains("id=34"))
        assertTrue(embedded.contains("hash=secret"))
        assertTrue(embedded.contains("t=125s"))
    }
    @Test fun okResumeIgnoresQueryWhenExtractingVideoId() {
        val embedded = StreamUrls.embed("https://ok.ru/video/987?from=share", 42000)
        assertTrue(embedded.startsWith("https://ok.ru/videoembed/987?"))
        assertTrue(embedded.contains("st.start=42"))
    }
    @Test fun rejectsNonNetworkAndMalformedUrls() {
        listOf("file:///etc/passwd", "javascript:alert(1)", "https://", "not a url").forEach {
            assertFalse(it, StreamUrls.isValid(it))
        }
        assertTrue(StreamUrls.isValid("https://example.org/live.m3u8"))
    }
    @Test fun directStreamsAreUnchanged() {
        val url = "https://example.org/live.m3u8?token=a%2Bb"
        assertEquals(url, StreamUrls.embed(url, 42000))
    }
}
