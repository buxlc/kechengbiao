package com.bu.kebiao.ui.courseimport

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object IcsTextDecoder {
    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        return when {
            bytes.startsWith(0xEF, 0xBB, 0xBF) ->
                bytes.copyOfRange(3, bytes.size).toString(StandardCharsets.UTF_8)
            bytes.startsWith(0xFF, 0xFE) ->
                bytes.copyOfRange(2, bytes.size).toString(StandardCharsets.UTF_16LE)
            bytes.startsWith(0xFE, 0xFF) ->
                bytes.copyOfRange(2, bytes.size).toString(StandardCharsets.UTF_16BE)
            looksLikeUtf16Le(bytes) -> bytes.toString(StandardCharsets.UTF_16LE)
            looksLikeUtf16Be(bytes) -> bytes.toString(StandardCharsets.UTF_16BE)
            else -> decodeUtf8Strict(bytes)
                ?: decodeWithCharset(bytes, "GB18030")
                ?: decodeWithCharset(bytes, "GBK")
                ?: bytes.toString(StandardCharsets.UTF_8)
        }.removePrefix("\uFEFF")
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String? {
        return try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun decodeWithCharset(bytes: ByteArray, charsetName: String): String? {
        return runCatching {
            bytes.toString(Charset.forName(charsetName))
        }.getOrNull()
    }

    private fun looksLikeUtf16Le(bytes: ByteArray): Boolean {
        val sample = bytes.take(80)
        val oddZeroCount = sample.filterIndexed { index, byte -> index % 2 == 1 && byte == 0.toByte() }.size
        return oddZeroCount >= sample.size / 4
    }

    private fun looksLikeUtf16Be(bytes: ByteArray): Boolean {
        val sample = bytes.take(80)
        val evenZeroCount = sample.filterIndexed { index, byte -> index % 2 == 0 && byte == 0.toByte() }.size
        return evenZeroCount >= sample.size / 4
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { index -> this[index].toInt() and 0xFF == prefix[index] }
    }
}
