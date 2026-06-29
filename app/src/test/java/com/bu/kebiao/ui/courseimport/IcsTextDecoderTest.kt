package com.bu.kebiao.ui.courseimport

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class IcsTextDecoderTest {

    @Test
    fun decodesUtf8WithBom() {
        val text = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nEND:VEVENT\r\nEND:VCALENDAR"
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray(StandardCharsets.UTF_8)

        val decoded = IcsTextDecoder.decode(bytes)

        assertTrue(decoded.startsWith("BEGIN:VCALENDAR"))
        assertTrue(decoded.contains("BEGIN:VEVENT"))
    }

    @Test
    fun decodesUtf16LeWithBom() {
        val text = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nEND:VEVENT\r\nEND:VCALENDAR"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(StandardCharsets.UTF_16LE)

        val decoded = IcsTextDecoder.decode(bytes)

        assertTrue(decoded.startsWith("BEGIN:VCALENDAR"))
        assertTrue(decoded.contains("BEGIN:VEVENT"))
    }
}
