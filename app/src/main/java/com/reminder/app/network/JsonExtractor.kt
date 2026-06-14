package com.reminder.app.network

/**
 * Claude 응답에 코드펜스나 설명 문구가 섞여 있어도 JSON 객체/배열 부분만 추출한다.
 */
object JsonExtractor {

    fun extractJsonObject(text: String): String? = extractBetween(text, '{', '}')

    fun extractJsonArray(text: String): String? = extractBetween(text, '[', ']')

    private fun extractBetween(text: String, open: Char, close: Char): String? {
        val start = text.indexOf(open)
        val end = text.lastIndexOf(close)
        if (start == -1 || end == -1 || end < start) return null
        return text.substring(start, end + 1)
    }
}
